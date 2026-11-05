package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do limite de tentativas (D09), sem base de dados. */
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
}
