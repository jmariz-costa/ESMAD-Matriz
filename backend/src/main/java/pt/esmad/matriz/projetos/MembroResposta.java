package pt.esmad.matriz.projetos;

import java.util.UUID;

/** openapi.yaml, schema Membro. */
public record MembroResposta(UUID id, String nome, String email, boolean ativo, boolean gestor) {
}
