package pt.esmad.matriz.tarefas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leitura dos corpos de criar e editar tarefa (openapi.yaml, CriarTarefaPedido e
 * EditarTarefaPedido), sem Quarkus nem base de dados.
 */
class PedidoTarefaTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode json(String texto) {
        try {
            return JSON.readTree(texto);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Os campos com erro, pela ordem em que foram encontrados. */
    private static List<String> camposComErro(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        assertEquals(CodigoErro.VALIDACAO, erro.codigo());
        return erro.detalhes().stream().map(Erro.Detalhe::campo).toList();
    }

    // Criar

    @Test
    void criacaoComTodosOsCampos() {
        NovaTarefa nova = PedidoTarefa.lerCriacao(json("""
                {"titulo": "Rever orçamento", "descricao": "Até sexta", "prazo": "2026-12-01", "prioridade": "ALTA"}
                """));
        assertEquals(new NovaTarefa("Rever orçamento", "Até sexta", LocalDate.of(2026, 12, 1), Prioridade.ALTA), nova);
    }

    @Test
    void criacaoComCamposANullOuOmitidos() {
        NovaTarefa nova = PedidoTarefa.lerCriacao(json("""
                {"titulo": "Rever orçamento", "descricao": null, "prazo": null, "prioridade": null}
                """));
        assertEquals(new NovaTarefa("Rever orçamento", null, null, null), nova);
        assertEquals(new NovaTarefa(null, null, null, null), PedidoTarefa.lerCriacao(json("{}")));
    }

    @ParameterizedTest(name = "corpo {0}")
    @ValueSource(strings = {"[]", "\"texto\"", "3", "null"})
    void corpoQueNaoEObjeto(String corpo) {
        assertEquals(List.of("corpo"), camposComErro(() -> PedidoTarefa.lerCriacao(json(corpo))));
    }

    @Test
    void corpoAusente() {
        assertEquals(List.of("corpo"), camposComErro(() -> PedidoTarefa.lerCriacao(null)));
    }

    @Test
    void criacaoComCamposInvalidosIndicaCadaUm() {
        List<String> campos = camposComErro(() -> PedidoTarefa.lerCriacao(json("""
                {"titulo": 1, "descricao": true, "prazo": "01-12-2026", "prioridade": "URGENTE", "cor": "azul"}
                """)));
        assertEquals(List.of("cor", "titulo", "descricao", "prazo", "prioridade"), campos);
    }

    @Test
    void prazoEPrioridadeQueNaoSaoTexto() {
        assertEquals(List.of("prazo", "prioridade"), camposComErro(() -> PedidoTarefa.lerCriacao(json("""
                {"titulo": "Rever", "prazo": 20261201, "prioridade": 2}
                """))));
    }

    @Test
    void prioridadeDistingueMaiusculas() {
        assertEquals(List.of("prioridade"), camposComErro(() -> PedidoTarefa.lerCriacao(json("""
                {"titulo": "Rever", "prioridade": "alta"}
                """))));
    }

    // Editar

    @Test
    void edicaoSoComOsCamposEnviados() {
        AlteracoesTarefa alteracoes = PedidoTarefa.lerEdicao(json("""
                {"versao": 3, "estado": "EM_CURSO"}
                """));
        assertEquals(3, alteracoes.versao());
        assertEquals(Estado.EM_CURSO, alteracoes.estado());
        assertNull(alteracoes.titulo());
        assertFalse(alteracoes.alteraDescricao());
        assertFalse(alteracoes.alteraPrazo());
        assertFalse(alteracoes.alteraPrioridade());
    }

    @Test
    void edicaoComNullLimpaOsCamposOpcionais() {
        AlteracoesTarefa alteracoes = PedidoTarefa.lerEdicao(json("""
                {"versao": 0, "descricao": null, "prazo": null, "prioridade": null}
                """));
        assertTrue(alteracoes.alteraDescricao());
        assertNull(alteracoes.descricao());
        assertTrue(alteracoes.alteraPrazo());
        assertNull(alteracoes.prazo());
        assertTrue(alteracoes.alteraPrioridade());
        assertNull(alteracoes.prioridade());
        assertNull(alteracoes.estado());
    }

    @ParameterizedTest(name = "versao {0}")
    @ValueSource(strings = {"1.5", "-1", "3000000000", "\"2\"", "true"})
    void versaoQueNaoEInteiroAPartirDeZero(String versao) {
        assertEquals(List.of("versao"),
                camposComErro(() -> PedidoTarefa.lerEdicao(json("{\"versao\": " + versao + ", \"titulo\": \"Rever\"}"))));
    }

    @Test
    void versaoEmFaltaOuANull() {
        assertEquals(List.of("versao"), camposComErro(() -> PedidoTarefa.lerEdicao(json("""
                {"titulo": "Rever", "estado": "EM_CURSO"}
                """))));
        assertEquals(List.of("versao"), camposComErro(() -> PedidoTarefa.lerEdicao(json("""
                {"versao": null, "titulo": "Rever"}
                """))));
    }

    @Test
    void soAVersaoNaoChega() {
        assertEquals(List.of("corpo"), camposComErro(() -> PedidoTarefa.lerEdicao(json("{\"versao\": 1}"))));
    }

    @Test
    void tituloANullERecusado() {
        assertEquals(List.of("titulo"), camposComErro(() -> PedidoTarefa.lerEdicao(json("""
                {"versao": 1, "titulo": null}
                """))));
    }

    @ParameterizedTest(name = "estado {0}")
    @ValueSource(strings = {"\"FEITA\"", "\"em_curso\"", "null", "1"})
    void estadoInvalido(String estado) {
        assertEquals(List.of("estado"),
                camposComErro(() -> PedidoTarefa.lerEdicao(json("{\"versao\": 1, \"estado\": " + estado + "}"))));
    }

    @Test
    void edicaoRecusaCamposDesconhecidosECorpoQueNaoEObjeto() {
        assertEquals(List.of("projetoId"), camposComErro(() -> PedidoTarefa.lerEdicao(json("""
                {"versao": 1, "projetoId": "x"}
                """))));
        assertEquals(List.of("corpo"), camposComErro(() -> PedidoTarefa.lerEdicao(json("[1]"))));
    }
}
