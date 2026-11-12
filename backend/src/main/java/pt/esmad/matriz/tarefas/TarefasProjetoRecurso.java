package pt.esmad.matriz.tarefas;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import pt.esmad.matriz.comum.Identificadores;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

import java.util.UUID;

/** Endpoints das tarefas de um projeto (openapi.yaml, tag Tarefas). */
@Path("/projetos/{projetoId}/tarefas")
@Produces(MediaType.APPLICATION_JSON)
public class TarefasProjetoRecurso {

    private final TarefasServico servico;

    public TarefasProjetoRecurso(TarefasServico servico) {
        this.servico = servico;
    }

    /** Os filtros e a página são lidos como texto, para um valor inválido dar 400 com o campo indicado. */
    @GET
    public Pagina<TarefaResposta> listar(@PathParam("projetoId") String projetoId,
                                         @QueryParam("estado") String estado,
                                         @QueryParam("prioridade") String prioridade,
                                         @QueryParam("pagina") String pagina,
                                         @QueryParam("tamanho") String tamanho) {
        UUID projeto = Identificadores.paraUuid(projetoId);
        Validacao validacao = new Validacao();
        Estado filtroEstado = filtro(Estado.class, estado, "estado",
                "O estado tem de ser A_FAZER, EM_CURSO, EM_REVISAO ou CONCLUIDA.", validacao);
        FiltroPrioridade filtroPrioridade = filtro(FiltroPrioridade.class, prioridade, "prioridade",
                "A prioridade tem de ser BAIXA, MEDIA, ALTA ou SEM_PRIORIDADE.", validacao);
        validacao.lancarSeHouverErros();
        return servico.listar(projeto, filtroEstado, filtroPrioridade, PedidoPagina.de(pagina, tamanho))
                .converter(TarefaResposta::de);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(@PathParam("projetoId") String projetoId,
                          @NotNull(message = "O corpo do pedido é obrigatório.") JsonNode corpo,
                          @Context UriInfo uri) {
        UUID projeto = Identificadores.paraUuid(projetoId);
        TarefaResposta tarefa = TarefaResposta.de(servico.criar(projeto, PedidoTarefa.lerCriacao(corpo)));
        return Response.created(uri.getBaseUriBuilder().path("tarefas").path(tarefa.id().toString()).build())
                .entity(tarefa)
                .build();
    }

    /** Vazio ou ausente = sem filtro. */
    private static <E extends Enum<E>> E filtro(Class<E> tipo, String valor, String campo, String mensagem,
                                                Validacao validacao) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        E resultado = PedidoTarefa.valorDe(tipo, valor.strip());
        if (resultado == null) {
            validacao.erro(campo, mensagem);
        }
        return resultado;
    }
}
