package pt.esmad.matriz.projetos;

/**
 * Campos a alterar num projeto. Distingue "não enviado" de "enviado a null",
 * porque {@code descricao: null} limpa a descrição (openapi.yaml, EditarProjetoPedido).
 */
public record AlteracoesProjeto(boolean alteraNome, String nome, boolean alteraDescricao, String descricao) {
}
