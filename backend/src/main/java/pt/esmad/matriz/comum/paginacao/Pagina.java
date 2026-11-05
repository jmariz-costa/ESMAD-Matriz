package pt.esmad.matriz.comum.paginacao;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados (openapi.yaml, Paginacao + itens).
 * {@code total} conta toda a listagem, e não só esta página.
 */
public record Pagina<T>(List<T> itens, long total, int pagina, int tamanho) {

    public <R> Pagina<R> converter(Function<T, R> conversao) {
        return new Pagina<>(itens.stream().map(conversao).toList(), total, pagina, tamanho);
    }
}
