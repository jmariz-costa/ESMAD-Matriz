package pt.esmad.matriz.comum.texto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Espaços nas pontas (RN23), incluindo os espaços Unicode e os caracteres invisíveis de formatação.
 * Regressão da revisão da Ficha PL 04 (Q1/S2, docs/quality-security-review.md): um título feito só
 * de U+00A0, U+200B ou U+FEFF era aceite, embora à vista estivesse vazio.
 */
class TextoTest {

    @ParameterizedTest(name = "U+{0} só, ou nas pontas, não conta")
    @ValueSource(strings = {"0020", "0009", "00A0", "2003", "2007", "202F", "3000", "2028", "200B", "200D", "2060", "FEFF"})
    void espacosEInvisiveisNasPontasNaoContam(String hex) {
        String c = Character.toString(Integer.parseInt(hex, 16));
        assertNull(Texto.normalizar(c.repeat(3)));
        assertEquals("abc", Texto.normalizar(c + "abc" + c));
    }

    @Test
    void oQueEstaNoMeioMantemSe() {
        assertEquals("a b", Texto.normalizar(" a b "));
        // Emoji composto com U+200D (ZWJ) no meio: continua igual
        assertEquals("👩‍💻 Rever", Texto.normalizar("​👩‍💻 Rever﻿"));
    }

    @Test
    void nuloOuVazio() {
        assertNull(Texto.normalizar(null));
        assertNull(Texto.normalizar(""));
    }
}
