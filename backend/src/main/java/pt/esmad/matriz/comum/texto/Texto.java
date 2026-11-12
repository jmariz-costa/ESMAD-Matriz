package pt.esmad.matriz.comum.texto;

/**
 * Regras comuns de texto (RN23): os espaços nas pontas não contam.
 */
public final class Texto {

    private Texto() {
    }

    /**
     * Sem espaços nas pontas; {@code null} se o texto for nulo ou só tiver espaços. Contam como
     * espaço também os espaços Unicode (ex.: U+00A0) e os caracteres invisíveis de formatação
     * (ex.: U+200B, U+FEFF), com a mesma regra do Frontend (comum/texto.ts). O que está no meio
     * do texto mantém-se. Revisão da Ficha PL 04 (Q1/S2).
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        int inicio = 0;
        int fim = texto.length();
        while (inicio < fim && invisivel(texto.codePointAt(inicio))) {
            inicio += Character.charCount(texto.codePointAt(inicio));
        }
        while (fim > inicio && invisivel(texto.codePointBefore(fim))) {
            fim -= Character.charCount(texto.codePointBefore(fim));
        }
        return inicio == fim ? null : texto.substring(inicio, fim);
    }

    /** Espaço (incluindo os Unicode, como U+00A0) ou carácter invisível de formatação (categoria Cf). */
    private static boolean invisivel(int carater) {
        return Character.isWhitespace(carater) || Character.isSpaceChar(carater)
                || Character.getType(carater) == Character.FORMAT;
    }

    /**
     * Padrão para {@code lower(campo) like ? escape '\\'}: "%texto%", sem distinguir maiúsculas,
     * com os caracteres especiais do LIKE (% _ \\) tratados como texto. {@code null} se não houver texto.
     */
    public static String padraoPesquisa(String pesquisa) {
        String texto = normalizar(pesquisa);
        if (texto == null) {
            return null;
        }
        String escapado = texto.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }

    /** Número de caracteres como o utilizador os vê (pontos de código, não unidades UTF-16). */
    public static int comprimento(String texto) {
        return texto.codePointCount(0, texto.length());
    }
}
