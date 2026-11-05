package pt.esmad.matriz.contas;

import java.util.UUID;

/** Apenas o identificador e o nome, sem expor outros dados da conta (S07; openapi.yaml, PessoaResumo). */
public record PessoaResumo(UUID id, String nome) {
}
