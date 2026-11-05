package pt.esmad.matriz.comum.paginacao;

import pt.esmad.matriz.comum.erros.Validacao;

/**
 * Página pedida numa listagem (openapi.yaml, parâmetros Pagina e Tamanho).
 */
public record PedidoPagina(int pagina, int tamanho) {

    static final int PAGINA_OMISSAO = 1;
    static final int TAMANHO_OMISSAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    /**
     * Lê os parâmetros da query como texto, para que um valor que não é número
     * dê 400 VALIDACAO com o campo indicado (e não o 404 do JAX-RS).
     */
    public static PedidoPagina de(String pagina, String tamanho) {
        Validacao validacao = new Validacao();
        Integer p = inteiro(pagina, PAGINA_OMISSAO);
        Integer t = inteiro(tamanho, TAMANHO_OMISSAO);
        if (p == null || p < 1) {
            validacao.erro("pagina", "A página tem de ser um número inteiro a partir de 1.");
        }
        if (t == null || t < 1 || t > TAMANHO_MAXIMO) {
            validacao.erro("tamanho", "O tamanho da página tem de ser um número inteiro entre 1 e 100.");
        }
        validacao.lancarSeHouverErros();
        return new PedidoPagina(p, t);
    }

    private static Integer inteiro(String valor, int omissao) {
        if (valor == null || valor.isBlank()) {
            return omissao;
        }
        try {
            return Integer.valueOf(valor.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int inicio() {
        return (pagina - 1) * tamanho;
    }
}
