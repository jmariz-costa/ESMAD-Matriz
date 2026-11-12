package pt.esmad.matriz.comentarios;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.esmad.matriz.comum.Identificadores;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

/** Endpoints dos comentários de uma tarefa (openapi.yaml, tag Comentários). */
@Path("/tarefas/{tarefaId}/comentarios")
@Produces(MediaType.APPLICATION_JSON)
public class ComentariosRecurso {

    /**
     * Corpo de POST (openapi.yaml, CriarComentarioPedido). Só tem o texto: um autor ou uma data
     * enviados pelo cliente são campos desconhecidos e o pedido é recusado (400).
     */
    public record CriarComentarioPedido(String texto) {
    }

    private final ComentariosServico servico;

    public ComentariosRecurso(ComentariosServico servico) {
        this.servico = servico;
    }

    @GET
    public Pagina<ComentarioResposta> listar(@PathParam("tarefaId") String tarefaId,
                                             @QueryParam("pagina") String pagina,
                                             @QueryParam("tamanho") String tamanho) {
        return servico.listar(Identificadores.paraUuid(tarefaId), PedidoPagina.de(pagina, tamanho))
                .converter(ComentarioResposta::de);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response comentar(@PathParam("tarefaId") String tarefaId,
                             @NotNull(message = "O corpo do pedido é obrigatório.") CriarComentarioPedido pedido) {
        ComentarioResposta comentario = ComentarioResposta.de(
                servico.comentar(Identificadores.paraUuid(tarefaId), pedido.texto()));
        return Response.status(Response.Status.CREATED).entity(comentario).build();
    }
}
