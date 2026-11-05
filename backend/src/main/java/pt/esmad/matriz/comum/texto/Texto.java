package pt.esmad.matriz.comum.texto;

/**
 * Regras comuns de texto (RN23): os espaços nas pontas não contam.
 */
public final class Texto {

    private Texto() {
    }

    /** Sem espaços nas pontas; {@code null} se o texto for nulo ou só tiver espaços. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Número de caracteres como o utilizador os vê (pontos de código, não unidades UTF-16). */
    public static int comprimento(String texto) {
        return texto.codePointCount(0, texto.length());
    }
}
