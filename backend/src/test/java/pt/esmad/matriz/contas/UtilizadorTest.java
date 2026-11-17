package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do limite de tentativas (D09), do nome (US19) e da palavra-passe temporária (PR20), sem base de dados. */
class UtilizadorTest {

    private static final Instant AGORA = Instant.parse("2026-11-05T10:00:00Z");

    private static Utilizador membro() {
        return new Utilizador(UUID.randomUUID(), "Ana Exemplo", "ana@empresa.pt", "hash",
                TipoUtilizador.MEMBRO, EstadoConta.ATIVO);
    }

    @Test
    void quatroFalhasNaoBloqueiam() {
        Utilizador utilizador = membro();
        for (int i = 0; i < 4; i++) {
            utilizador.registarFalha(AGORA);
        }
        assertFalse(utilizador.estaBloqueado(AGORA));
        assertEquals(4, utilizador.tentativasFalhadas());
    }

    @Test
    void quintaFalhaBloqueiaDuranteQuinzeMinutos() {
        Utilizador utilizador = membro();
        for (int i = 0; i < 5; i++) {
            utilizador.registarFalha(AGORA);
        }
        assertTrue(utilizador.estaBloqueado(AGORA));
        assertTrue(utilizador.estaBloqueado(AGORA.plus(Duration.ofMinutes(15)).minusSeconds(1)));
        assertFalse(utilizador.estaBloqueado(AGORA.plus(Duration.ofMinutes(15))));
    }

    @Test
    void falhasDuranteBloqueioNaoOProlongam() {
        Utilizador utilizador = membro();
        for (int i = 0; i < 5; i++) {
            utilizador.registarFalha(AGORA);
        }
        utilizador.registarFalha(AGORA.plus(Duration.ofMinutes(10)));
        assertFalse(utilizador.estaBloqueado(AGORA.plus(Duration.ofMinutes(15))));
        assertEquals(0, utilizador.tentativasFalhadas());
    }

    @Test
    void loginCorretoPoeContadorAZero() {
        Utilizador utilizador = membro();
        for (int i = 0; i < 4; i++) {
            utilizador.registarFalha(AGORA);
        }
        utilizador.registarSucesso();
        utilizador.registarFalha(AGORA);
        assertEquals(1, utilizador.tentativasFalhadas());
        assertFalse(utilizador.estaBloqueado(AGORA));
    }

    // Desativar e reativar (US14 crit. 1 e 6)

    @Test
    void desativarImpedeDeEntrarEReativarDevolveOAcesso() {
        Utilizador utilizador = membro();

        utilizador.desativar();
        assertFalse(utilizador.estaAtivo());
        assertEquals(EstadoConta.DESATIVADO, utilizador.estado());

        utilizador.reativar();
        assertTrue(utilizador.estaAtivo());
        assertEquals(EstadoConta.ATIVO, utilizador.estado());
    }

    // Editar o nome e repor o acesso (US19), mudar a palavra-passe (US20)

    private static List<String> camposComErro(Runnable acao) {
        ErroAplicacao erro =
                assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        return erro.detalhes().stream().map(Erro.Detalhe::campo).toList();
    }

    @Test
    void oNomeTemEntre2E150CaracteresSemContarOsEspacosNasPontas() {
        Utilizador utilizador = membro();

        utilizador.alterarNome("  Al  ");
        assertEquals("Al", utilizador.nome());
        utilizador.alterarNome("x".repeat(150));
        assertEquals(150, utilizador.nome().length());

        assertEquals(List.of("nome"), camposComErro(() -> utilizador.alterarNome(" A ")));
        assertEquals(List.of("nome"), camposComErro(() -> utilizador.alterarNome("x".repeat(151))));
        assertEquals(List.of("nome"), camposComErro(() -> utilizador.alterarNome("   ")));
        assertEquals(List.of("nome"), camposComErro(() -> utilizador.alterarNome(null)));
        assertEquals(150, utilizador.nome().length());
    }

    @Test
    void reporOAcessoObrigaAMudarAPalavraPasseELimpaOBloqueio() {
        Utilizador utilizador = membro();
        for (int i = 0; i < Utilizador.MAXIMO_FALHAS_SEGUIDAS; i++) {
            utilizador.registarFalha(AGORA);
        }
        assertTrue(utilizador.estaBloqueado(AGORA));

        utilizador.reporAcesso("hash-temporario");

        assertEquals("hash-temporario", utilizador.passwordHash());
        assertTrue(utilizador.deveMudarPalavraPasse());
        assertFalse(utilizador.estaBloqueado(AGORA));
        assertEquals(0, utilizador.tentativasFalhadas());
    }

    @Test
    void naoSeRepoeOAcessoDeUmaContaDesativada() {
        Utilizador utilizador = membro();
        utilizador.desativar();

        ErroAplicacao erro = assertThrows(
                ErroAplicacao.class, () -> utilizador.reporAcesso("hash-temporario"));

        assertEquals(409, erro.estadoHttp());
        assertEquals(CodigoErro.CONTA_DESATIVADA, erro.codigo());
        assertEquals("hash", utilizador.passwordHash());
        assertFalse(utilizador.deveMudarPalavraPasse());
    }

    @Test
    void definirUmaPalavraPasseNovaDeixaDeSerTemporaria() {
        Utilizador utilizador = membro();
        utilizador.reporAcesso("hash-temporario");

        utilizador.definirPalavraPasse("hash-novo");

        assertEquals("hash-novo", utilizador.passwordHash());
        assertFalse(utilizador.deveMudarPalavraPasse());
    }
}
