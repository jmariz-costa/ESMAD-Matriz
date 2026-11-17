package pt.esmad.matriz.contas;

import java.util.UUID;

/** openapi.yaml, schema Conta: uma conta de Membro como o Administrador a vê (US14). */
public record ContaResposta(UUID id, String nome, String email, EstadoConta estado) {

    static ContaResposta de(Utilizador u) {
        return new ContaResposta(u.id(), u.nome(), u.email(), u.estado());
    }
}
