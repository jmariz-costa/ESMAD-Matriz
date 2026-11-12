package pt.esmad.matriz.comentarios;

import pt.esmad.matriz.contas.PessoaResumo;

import java.time.Instant;
import java.util.UUID;

/** openapi.yaml, schema Comentario. */
public record ComentarioResposta(UUID id, UUID tarefaId, PessoaResumo autor, String texto,
                                 Instant criadoEm, Instant editadoEm) {

    static ComentarioResposta de(ComentariosServico.ComentarioDetalhe detalhe) {
        Comentario c = detalhe.comentario();
        return new ComentarioResposta(c.id(), c.tarefaId(), detalhe.autor(), c.texto(), c.criadoEm(), c.editadoEm());
    }
}
