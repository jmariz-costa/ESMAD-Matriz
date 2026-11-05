package pt.esmad.matriz.contas;

import java.util.UUID;

/**
 * Dados de uma conta que os outros módulos podem mostrar: id, nome, email (S07)
 * e se a conta está ativa (PR15).
 */
public record ContaResumo(UUID id, String nome, String email, boolean ativo) {
}
