package pt.esmad.matriz.projetos;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.esmad.matriz.comum.Identificadores;

import java.util.List;
import java.util.UUID;

/** Endpoints dos membros de um projeto (openapi.yaml, tag Membros). */
@Path("/projetos/{projetoId}/membros")
@Produces(MediaType.APPLICATION_JSON)
public class MembrosRecurso {

    /** Corpo de POST (openapi.yaml, UtilizadorIdPedido). */
    public record UtilizadorIdPedido(String utilizadorId) {
    }

    private final MembrosServico servico;

    public MembrosRecurso(MembrosServico servico) {
        this.servico = servico;
    }

    @GET
    public List<MembroResposta> listar(@PathParam("projetoId") String projetoId) {
        return servico.listar(Identificadores.paraUuid(projetoId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adicionar(@PathParam("projetoId") String projetoId,
                              @NotNull(message = "O corpo do pedido é obrigatório.") UtilizadorIdPedido pedido) {
        UUID projeto = Identificadores.paraUuid(projetoId);
        MembroResposta membro = servico.adicionar(projeto,
                Identificadores.doCorpo(pedido.utilizadorId(), "utilizadorId", "Indique a pessoa a adicionar."));
        return Response.status(Response.Status.CREATED).entity(membro).build();
    }

    @DELETE
    @Path("/{utilizadorId}")
    public Response remover(@PathParam("projetoId") String projetoId,
                            @PathParam("utilizadorId") String utilizadorId) {
        servico.remover(Identificadores.paraUuid(projetoId), Identificadores.paraUuid(utilizadorId));
        return Response.noContent().build();
    }
}
