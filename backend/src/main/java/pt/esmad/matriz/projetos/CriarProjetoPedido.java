package pt.esmad.matriz.projetos;

/**
 * Corpo de POST /projetos (openapi.yaml, CriarProjetoPedido).
 * As regras do texto (RN23) são verificadas pelo agregado {@link Projeto}.
 */
public record CriarProjetoPedido(String nome, String descricao) {
}
