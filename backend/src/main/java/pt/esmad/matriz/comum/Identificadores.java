package pt.esmad.matriz.comum;

import pt.esmad.matriz.comum.erros.ErroAplicacao;

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
}
