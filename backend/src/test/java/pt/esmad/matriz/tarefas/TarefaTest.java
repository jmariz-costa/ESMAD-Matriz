package pt.esmad.matriz.tarefas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do agregado Tarefa (RN10, RN23, PR05), sem base de dados. */
class TarefaTest {

    private static final UUID PROJETO = UUID.randomUUID();

    private static Tarefa criar(String titulo, String descricao) {
        return Tarefa.criar(PROJETO, new NovaTarefa(titulo, descricao, null, null));
    }

    private static Erro.Detalhe unicoErro(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        assertEquals(1, erro.detalhes().size());
        return erro.detalhes().getFirst();
    }

    @Test
    void comecaEmAFazerSemResponsaveisENaVersao0() {
        Tarefa tarefa = Tarefa.criar(PROJETO,
                new NovaTarefa("Rever", null, LocalDate.of(2020, 1, 1), Prioridade.ALTA));
        assertEquals(Estado.A_FAZER, tarefa.estado());
        assertTrue(tarefa.responsaveis().isEmpty());
        assertEquals(0, tarefa.versao());
        assertEquals(PROJETO, tarefa.projetoId());
        assertEquals(LocalDate.of(2020, 1, 1), tarefa.prazo());
        assertEquals(Prioridade.ALTA, tarefa.prioridade());
    }

    @Test
    void espacosNasPontasNaoContamEDescricaoSoComEspacosFicaVazia() {
        Tarefa tarefa = criar("   abc   ", "   ");
        assertEquals("abc", tarefa.titulo());
        assertNull(tarefa.descricao());
    }

    @Test
    void tituloObrigatorioEntre3E150Caracteres() {
        assertEquals("titulo", unicoErro(() -> criar(null, null)).campo());
        assertEquals("titulo", unicoErro(() -> criar("   ", null)).campo());
        assertEquals("titulo", unicoErro(() -> criar("ab", null)).campo());
        assertEquals("titulo", unicoErro(() -> criar("x".repeat(151), null)).campo());
        assertEquals(150, criar("x".repeat(150), null).titulo().length());
    }

    /**
     * Valores-limite do título (RN23: entre 3 e 150, limites incluídos), com e sem espaços nas
     * pontas, que não contam. Ficha PL 04, Exercício 2; cenário T05 do docs/test-plan.md.
     */
    @ParameterizedTest(name = "título com {0} caracteres: aceite = {1}")
    @CsvSource({"2, false", "3, true", "150, true", "151, false"})
    void limitesDoTituloSemContarOsEspacosNasPontas(int tamanho, boolean aceite) {
        String titulo = "x".repeat(tamanho);
        for (String escrito : List.of(titulo, "  " + titulo + "  ")) {
            if (aceite) {
                assertEquals(titulo, criar(escrito, null).titulo());
            } else {
                assertEquals("titulo", unicoErro(() -> criar(escrito, null)).campo());
            }
        }
    }

    @Test
    void contaCaracteresComoOUtilizadorOsVe() {
        // 3 emojis = 3 caracteres, embora sejam 6 unidades UTF-16
        assertEquals("😀😀😀", criar("😀😀😀", null).titulo());
    }

    private static AlteracoesTarefa soEstado(int versao, Estado estado) {
        return new AlteracoesTarefa(versao, null, false, null, false, null, false, null, estado);
    }

    @Test
    void editarDevolveSoOsCamposQueMudaramComOsValoresEmTexto() {
        Tarefa tarefa = Tarefa.criar(PROJETO,
                new NovaTarefa("Rever", "Texto", LocalDate.of(2026, 11, 20), Prioridade.MEDIA));

        List<Tarefa.CampoAlterado> alterados = tarefa.editar(new AlteracoesTarefa(0, " Rever ",
                true, null, true, LocalDate.of(2026, 12, 1), true, Prioridade.MEDIA, Estado.EM_CURSO));

        assertEquals(List.of(
                new Tarefa.CampoAlterado("descricao", "Texto", null),
                new Tarefa.CampoAlterado("estado", "A_FAZER", "EM_CURSO"),
                new Tarefa.CampoAlterado("prazo", "2026-11-20", "2026-12-01")), alterados);
        assertNull(tarefa.descricao());
        assertEquals(Estado.EM_CURSO, tarefa.estado());
    }

    @Test
    void camposNaoEnviadosMantemSe() {
        Tarefa tarefa = Tarefa.criar(PROJETO,
                new NovaTarefa("Rever", "Texto", LocalDate.of(2026, 11, 20), Prioridade.ALTA));
        tarefa.editar(soEstado(0, Estado.CONCLUIDA));
        assertEquals("Rever", tarefa.titulo());
        assertEquals("Texto", tarefa.descricao());
        assertEquals(LocalDate.of(2026, 11, 20), tarefa.prazo());
        assertEquals(Prioridade.ALTA, tarefa.prioridade());
    }

    @Test
    void editarComVersaoAntigaERecusadoSemAlterarNada() {
        Tarefa tarefa = criar("Rever", null);
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> tarefa.editar(soEstado(3, Estado.CONCLUIDA)));
        assertEquals(409, erro.estadoHttp());
        assertEquals(CodigoErro.VERSAO_DESATUALIZADA, erro.codigo());
        assertEquals(Estado.A_FAZER, tarefa.estado());
    }

    @Test
    void editarComTituloInvalidoNaoAlteraNada() {
        Tarefa tarefa = criar("Rever", null);
        AlteracoesTarefa alteracoes = new AlteracoesTarefa(0, "  ", false, null, false, null, false, null,
                Estado.CONCLUIDA);
        assertEquals("titulo", unicoErro(() -> tarefa.editar(alteracoes)).campo());
        assertEquals("Rever", tarefa.titulo());
        assertEquals(Estado.A_FAZER, tarefa.estado());
    }

    @Test
    void descricaoAte2000Caracteres() {
        assertEquals("descricao", unicoErro(() -> criar("Rever", "x".repeat(2001))).campo());
        assertEquals(2000, criar("Rever", "x".repeat(2000)).descricao().length());
    }
}
