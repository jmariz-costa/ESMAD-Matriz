package pt.esmad.matriz.comum.autenticacao;

import java.util.UUID;

/**
 * Quem fez o pedido, obtido da sessão (nunca de dados enviados pelo cliente).
 */
public record Identidade(UUID id, String nome, String email, boolean administrador) {
}
