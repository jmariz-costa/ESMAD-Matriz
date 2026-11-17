package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras das palavras-passe novas (RN04) e hash (S01), sem base de dados. */
class PalavraPasseTest {

    private static List<String> mensagens(String palavraPasse) {
        Validacao validacao = new Validacao();
        PalavraPasse.validar(palavraPasse, "palavraPasseNova", validacao);
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, validacao::lancarSeHouverErros);
        erro.detalhes().forEach(d -> assertEquals("palavraPasseNova", d.campo()));
        return erro.detalhes().stream().map(Erro.Detalhe::mensagem).toList();
    }

    @Test
    void temDeTerPeloMenos8Caracteres() {
        assertEquals(List.of("A palavra-passe é obrigatória."), mensagens(null));
        assertEquals(List.of("A palavra-passe é obrigatória."), mensagens(""));
        assertEquals(List.of("A palavra-passe deve ter pelo menos 8 caracteres."), mensagens("1234567"));

        Validacao valida = new Validacao();
        PalavraPasse.validar("12345678", "palavraPasseNova", valida);
        valida.lancarSeHouverErros();
    }

    @Test
    void oHashNaoEAPalavraPasseEConfereSoComACerta() {
        String hash = PalavraPasse.hash("palavra-passe-nova");

        assertNotEquals("palavra-passe-nova", hash);
        assertTrue(PalavraPasse.confere("palavra-passe-nova", hash));
        assertFalse(PalavraPasse.confere("outra-palavra-passe", hash));
    }
}
