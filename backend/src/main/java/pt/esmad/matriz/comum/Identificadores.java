package pt.esmad.matriz.comum;

import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;
import java.util.UUID;

/**
 * Identificadores recebidos no caminho dos pedidos.
 */
public final class Identificadores {

    private Identificadores() {
    }

    /** Um identificador que não é um UUID válido corresponde a um recurso que não existe (404). */
    public static UUID paraUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            throw ErroAplicacao.naoEncontrado();
        }
    }

    /** No corpo do pedido, um id em falta ou inválido é um erro de validação (400), e não um 404. */
    public static UUID doCorpo(String valor, String campo, String mensagemEmFalta) {
        if (valor == null || valor.isBlank()) {
            throw ErroAplicacao.validacao(List.of(new Erro.Detalhe(campo, mensagemEmFalta)));
        }
        try {
            return UUID.fromString(valor.strip());
        } catch (IllegalArgumentException e) {
            throw ErroAplicacao.validacao(List.of(new Erro.Detalhe(campo, "O identificador não é válido.")));
        }
    }
}
