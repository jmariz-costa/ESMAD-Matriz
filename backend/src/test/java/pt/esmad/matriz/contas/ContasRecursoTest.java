package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.contas.ContasRecurso.DesativarContaPedido;
import pt.esmad.matriz.contas.ContasRecurso.NovoGestorPedido;
import pt.esmad.matriz.projetos.GestoresServico.NovoGestor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Leitura dos parâmetros e do corpo da gestão de contas (US14), sem Quarkus nem base de dados. */
class ContasRecursoTest {

    private static List<String> camposComErro(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        return erro.detalhes().stream().map(Erro.Detalhe::campo).toList();
    }

    @Test
    void estadoVazioEUmSemFiltroEAceitaAtivoEDesativado() {
        assertNull(ContasRecurso.lerEstado(null));
        assertNull(ContasRecurso.lerEstado("  "));
        assertEquals(EstadoConta.ATIVO, ContasRecurso.lerEstado(" ATIVO "));
        assertEquals(EstadoConta.DESATIVADO, ContasRecurso.lerEstado("DESATIVADO"));
    }

    @Test
    void outroEstadoDa400() {
        assertEquals(List.of("estado"), camposComErro(() -> ContasRecurso.lerEstado("POR_CONFIRMAR")));
        assertEquals(List.of("estado"), camposComErro(() -> ContasRecurso.lerEstado("ativo")));
    }

    @Test
    void semCorpoOuSemListaNaoHaNovosGestores() {
        assertEquals(List.of(), ContasRecurso.novosGestores(null));
        assertEquals(List.of(), ContasRecurso.novosGestores(new DesativarContaPedido(null)));
    }

    @Test
    void leOsNovosGestores() {
        UUID projeto = UUID.randomUUID();
        UUID gestor = UUID.randomUUID();
        assertEquals(List.of(new NovoGestor(projeto, gestor)), ContasRecurso.novosGestores(
                new DesativarContaPedido(List.of(new NovoGestorPedido(projeto.toString(), gestor.toString())))));
    }

    @Test
    void elementoNuloOuIdentificadoresEmFaltaOuInvalidosDao400() {
        assertEquals(List.of("novosGestores"), camposComErro(() -> ContasRecurso.novosGestores(
                new DesativarContaPedido(Arrays.asList((NovoGestorPedido) null)))));
        assertEquals(List.of("novosGestores"), camposComErro(() -> ContasRecurso.novosGestores(
                new DesativarContaPedido(List.of(new NovoGestorPedido(null, UUID.randomUUID().toString()))))));
        assertEquals(List.of("novosGestores"), camposComErro(() -> ContasRecurso.novosGestores(
                new DesativarContaPedido(List.of(new NovoGestorPedido(UUID.randomUUID().toString(), "x"))))));
    }
}
