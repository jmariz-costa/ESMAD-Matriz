package pt.esmad.matriz.comentarios;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regras do agregado Comentario (RN23), sem base de dados. */
class ComentarioTest {

    private static final UUID TAREFA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final Instant AGORA = Instant.parse("2026-11-12T10:00:00Z");

    private static Comentario criar(String texto) {
        return Comentario.criar(TAREFA, AUTOR, texto, AGORA);
    }

    private static String campoDoErro(String texto) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> criar(texto));
        assertEquals(400, erro.estadoHttp());
        return erro.detalhes().getFirst().campo();
    }

    @Test
    void guardaOAutorEADataRecebidosDoServico() {
        Comentario comentario = criar("  Olá  ");
        assertEquals("Olá", comentario.texto());
        assertEquals(AUTOR, comentario.autorId());
        assertEquals(AGORA, comentario.criadoEm());
        assertNull(comentario.editadoEm());
    }

    @Test
    void textoEntre1E2000CaracteresSemContarOsEspacosNasPontas() {
        assertEquals("texto", campoDoErro(null));
        assertEquals("texto", campoDoErro(""));
        assertEquals("texto", campoDoErro("   "));
        assertEquals("texto", campoDoErro("x".repeat(2001)));
        assertEquals(1, criar("x").texto().length());
        assertEquals(2000, criar("x".repeat(2000)).texto().length());
    }
}
