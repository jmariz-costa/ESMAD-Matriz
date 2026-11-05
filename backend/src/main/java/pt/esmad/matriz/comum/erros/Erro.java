package pt.esmad.matriz.comum.erros;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Corpo de todas as respostas de erro da API (openapi.yaml, schema Erro).
 * {@code detalhes} só aparece quando há problemas por campo.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Erro(CodigoErro codigo, String mensagem, List<Detalhe> detalhes) {

    public record Detalhe(String campo, String mensagem) {
    }

    public static Erro de(CodigoErro codigo, String mensagem) {
        return new Erro(codigo, mensagem, List.of());
    }
}
