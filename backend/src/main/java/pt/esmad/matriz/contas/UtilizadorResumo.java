package pt.esmad.matriz.contas;

import java.util.UUID;

/** openapi.yaml, schema UtilizadorResumo: só o id, o nome e o email (S07). */
public record UtilizadorResumo(UUID id, String nome, String email) {
}
