package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Expiração por inatividade (D06), sem base de dados. */
class SessaoTest {

    private static final Instant INICIO = Instant.parse("2026-11-05T10:00:00Z");

    @Test
    void expiraAoFimDeTrintaMinutosSemAtividade() {
        Sessao sessao = Sessao.iniciar(UUID.randomUUID(), "hash", INICIO);
        assertFalse(sessao.expirou(INICIO.plus(Duration.ofMinutes(30)).minusSeconds(1)));
        assertTrue(sessao.expirou(INICIO.plus(Duration.ofMinutes(30))));
    }

    @Test
    void renovarAdiaAExpiracao() {
        Sessao sessao = Sessao.iniciar(UUID.randomUUID(), "hash", INICIO);
        sessao.renovar(INICIO.plus(Duration.ofMinutes(20)));
        assertFalse(sessao.expirou(INICIO.plus(Duration.ofMinutes(45))));
        assertTrue(sessao.expirou(INICIO.plus(Duration.ofMinutes(50))));
    }
}
