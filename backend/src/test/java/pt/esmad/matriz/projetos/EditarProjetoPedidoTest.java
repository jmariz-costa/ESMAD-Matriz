package pt.esmad.matriz.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Leitura do corpo de editar projeto (openapi.yaml, EditarProjetoPedido), sem Quarkus nem base de dados. */
class EditarProjetoPedidoTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static JsonNode json(String texto) {
        try {
            return JSON.readTree(texto);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static List<Erro.Detalhe> erros(String corpo) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> EditarProjetoPedido.ler(json(corpo)));
        assertEquals(400, erro.estadoHttp());
        assertEquals(CodigoErro.VALIDACAO, erro.codigo());
        return erro.detalhes();
    }

    @Test
    void soONome() {
        assertEquals(new AlteracoesProjeto(true, "Padaria", false, null),
                EditarProjetoPedido.ler(json("{\"nome\": \"Padaria\"}")));
    }

    @Test
    void descricaoANullLimpaADescricao() {
        assertEquals(new AlteracoesProjeto(false, null, true, null),
                EditarProjetoPedido.ler(json("{\"descricao\": null}")));
        assertEquals(new AlteracoesProjeto(true, "Padaria", true, "Nova"),
                EditarProjetoPedido.ler(json("{\"nome\": \"Padaria\", \"descricao\": \"Nova\"}")));
    }

    @ParameterizedTest(name = "corpo {0}")
    @ValueSource(strings = {"[]", "\"texto\"", "null"})
    void corpoQueNaoEObjeto(String corpo) {
        assertEquals("corpo", erros(corpo).getFirst().campo());
    }

    @Test
    void corpoAusente() {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> EditarProjetoPedido.ler(null));
        assertEquals("corpo", erro.detalhes().getFirst().campo());
    }

    @Test
    void corpoVazio() {
        assertEquals(List.of(new Erro.Detalhe("corpo", "Indique pelo menos um campo a alterar.")), erros("{}"));
    }

    @Test
    void nomeANullOuQueNaoETexto() {
        assertEquals(List.of(new Erro.Detalhe("nome", "O nome é obrigatório.")), erros("{\"nome\": null}"));
        assertEquals(List.of(new Erro.Detalhe("nome", "O nome tem de ser texto.")), erros("{\"nome\": 7}"));
    }

    @Test
    void descricaoQueNaoETexto() {
        assertEquals(List.of(new Erro.Detalhe("descricao", "A descrição tem de ser texto.")),
                erros("{\"descricao\": false}"));
    }

    @Test
    void campoDesconhecido() {
        assertEquals(List.of(new Erro.Detalhe("gestorId", "Campo desconhecido.")),
                erros("{\"nome\": \"Padaria\", \"gestorId\": \"x\"}"));
    }
}
