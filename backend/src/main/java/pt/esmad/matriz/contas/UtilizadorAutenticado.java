package pt.esmad.matriz.contas;

import pt.esmad.matriz.comum.autenticacao.Identidade;

import java.util.UUID;

/** Resposta de POST /auth/login e GET /auth/me (openapi.yaml, schema UtilizadorAutenticado). */
public record UtilizadorAutenticado(UUID id, String nome, String email, TipoUtilizador tipo) {

    static UtilizadorAutenticado de(Utilizador utilizador) {
        return new UtilizadorAutenticado(utilizador.id(), utilizador.nome(),
                utilizador.email(), utilizador.tipo());
    }

    static UtilizadorAutenticado de(Identidade identidade) {
        return new UtilizadorAutenticado(identidade.id(), identidade.nome(), identidade.email(),
                identidade.administrador() ? TipoUtilizador.ADMINISTRADOR : TipoUtilizador.MEMBRO);
    }
}
