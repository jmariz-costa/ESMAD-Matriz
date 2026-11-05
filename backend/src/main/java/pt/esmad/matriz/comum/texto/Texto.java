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
