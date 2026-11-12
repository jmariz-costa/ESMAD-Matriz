package pt.esmad.matriz.tarefas;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.esmad.matriz.comum.Identificadores;

import java.util.UUID;

/** Endpoints de uma tarefa (openapi.yaml, tag Tarefas). */
@Path("/tarefas/{tarefaId}")
@Produces(MediaType.APPLICATION_JSON)
public class TarefasRecurso {

    private final TarefasServico servico;

    public TarefasRecurso(TarefasServico servico) {
        this.servico = servico;
    }

    @GET
    public TarefaResposta consultar(@PathParam("tarefaId") String tarefaId) {
        return TarefaResposta.de(servico.consultar(Identificadores.paraUuid(tarefaId)));
    }

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    public TarefaResposta editar(@PathParam("tarefaId") String tarefaId,
                                 @NotNull(message = "O corpo do pedido é obrigatório.") JsonNode corpo) {
        UUID id = Identificadores.paraUuid(tarefaId);
        return TarefaResposta.de(servico.editar(id, PedidoTarefa.lerEdicao(corpo)));
    }

    @DELETE
    public Response eliminar(@PathParam("tarefaId") String tarefaId) {
        servico.eliminar(Identificadores.paraUuid(tarefaId));
        return Response.noContent().build();
    }
}
