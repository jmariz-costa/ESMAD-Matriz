package pt.esmad.matriz.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import pt.esmad.matriz.comum.Identificadores;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

import java.util.UUID;

/**
 * Endpoints de projetos (openapi.yaml, tag Projetos).
 */
@Path("/projetos")
@Produces(MediaType.APPLICATION_JSON)
public class ProjetosRecurso {

    private final ProjetosServico servico;

    public ProjetosRecurso(ProjetosServico servico) {
        this.servico = servico;
    }

    @GET
    public Pagina<ProjetoResposta> listar(@QueryParam("q") String q,
                                          @QueryParam("pagina") String pagina,
                                          @QueryParam("tamanho") String tamanho) {
        return servico.listar(q, PedidoPagina.de(pagina, tamanho)).converter(ProjetoResposta::de);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(@NotNull(message = "O corpo do pedido é obrigatório.") CriarProjetoPedido pedido,
                          @Context UriInfo uri) {
        ProjetoResposta projeto = ProjetoResposta.de(servico.criar(pedido.nome(), pedido.descricao()));
        return Response.created(uri.getAbsolutePathBuilder().path(projeto.id().toString()).build())
                .entity(projeto)
                .build();
    }

    @GET
    @Path("/{projetoId}")
    public ProjetoResposta consultar(@PathParam("projetoId") String projetoId) {
        return ProjetoResposta.de(servico.consultar(Identificadores.paraUuid(projetoId)));
    }

    @PATCH
    @Path("/{projetoId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ProjetoResposta editar(@PathParam("projetoId") String projetoId,
                                  @NotNull(message = "O corpo do pedido é obrigatório.") JsonNode corpo) {
        UUID id = Identificadores.paraUuid(projetoId);
        return ProjetoResposta.de(servico.editar(id, EditarProjetoPedido.ler(corpo)));
    }

    @DELETE
    @Path("/{projetoId}")
    public Response eliminar(@PathParam("projetoId") String projetoId) {
        servico.eliminar(Identificadores.paraUuid(projetoId));
        return Response.noContent().build();
    }
}
