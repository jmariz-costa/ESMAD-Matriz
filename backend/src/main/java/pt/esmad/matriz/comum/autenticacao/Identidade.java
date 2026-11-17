package pt.esmad.matriz.comum.autenticacao;

import java.util.UUID;

/**
 * Quem fez o pedido, obtido da sessão (nunca de dados enviados pelo cliente).
 * {@code deveMudarPalavraPasse}: tem uma palavra-passe temporária por mudar (PR20).
 */
public record Identidade(UUID id, String nome, String email, boolean administrador,
                         boolean deveMudarPalavraPasse) {
}
