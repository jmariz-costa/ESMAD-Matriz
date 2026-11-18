package pt.esmad.matriz.contas;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.esmad.matriz.comum.Identificadores;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.projetos.GestoresServico;

import java.util.List;

/** Endpoints da gestão de contas pelo Administrador (openapi.yaml, tag Contas; US14, US19, US21). */
@Path("/contas")
@Produces(MediaType.APPLICATION_JSON)
public class ContasRecurso {

    /** Corpo de POST /contas (openapi.yaml, CriarContaPedido). */
    public record CriarContaPedido(String nome, String email, String palavraPasseTemporaria) {
    }

    /** Corpo de POST /contas/{id}/desativacao (openapi.yaml, DesativarContaPedido). */
    public record DesativarContaPedido(List<NovoGestorPedido> novosGestores) {
    }

    public record NovoGestorPedido(String projetoId, String gestorId) {
    }

    /** Corpo de PATCH /contas/{id} (openapi.yaml, EditarContaPedido). */
    public record EditarContaPedido(String nome) {
    }

    /** Corpo de POST /contas/{id}/reposicao-acesso (openapi.yaml, ReporAcessoPedido). */
    public record ReporAcessoPedido(String palavraPasseTemporaria) {
    }

    private final GestaoContasServico servico;

    public ContasRecurso(GestaoContasServico servico) {
        this.servico = servico;
    }

    @GET
    public Pagina<ContaResposta> listar(@QueryParam("q") String q,
                                        @QueryParam("estado") String estado,
                                        @QueryParam("pagina") String pagina,
                                        @QueryParam("tamanho") String tamanho) {
        EstadoConta filtro = lerEstado(estado);
        return servico.listar(q, filtro, PedidoPagina.de(pagina, tamanho));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response criar(@NotNull(message = "O corpo do pedido é obrigatório.") CriarContaPedido pedido) {
        ContaResposta conta = servico.criar(pedido.nome(), pedido.email(), pedido.palavraPasseTemporaria());
        return Response.status(Response.Status.CREATED).entity(conta).build();
    }

    @PATCH
    @Path("/{contaId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContaResposta editar(@PathParam("contaId") String contaId,
                                @NotNull(message = "O corpo do pedido é obrigatório.") EditarContaPedido pedido) {
        return servico.editarNome(Identificadores.paraUuid(contaId), pedido.nome());
    }

    @POST
    @Path("/{contaId}/reposicao-acesso")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reporAcesso(@PathParam("contaId") String contaId,
                                @NotNull(message = "O corpo do pedido é obrigatório.") ReporAcessoPedido pedido) {
        servico.reporAcesso(Identificadores.paraUuid(contaId), pedido.palavraPasseTemporaria());
        return Response.noContent().build();
    }

    @GET
    @Path("/{contaId}/projetos-geridos")
    public List<GestoresServico.ProjetoGerido> projetosGeridos(@PathParam("contaId") String contaId) {
        return servico.projetosGeridos(Identificadores.paraUuid(contaId));
    }

    /** O corpo pode faltar quando a pessoa não é Gestora de nenhum projeto. */
    @POST
    @Path("/{contaId}/desativacao")
    @Consumes(MediaType.APPLICATION_JSON)
    public ContaResposta desativar(@PathParam("contaId") String contaId, DesativarContaPedido pedido) {
        return servico.desativar(Identificadores.paraUuid(contaId), novosGestores(pedido));
    }

    @POST
    @Path("/{contaId}/reativacao")
    public ContaResposta reativar(@PathParam("contaId") String contaId) {
        return servico.reativar(Identificadores.paraUuid(contaId));
    }

    /** Vazio ou ausente = sem filtro; só aceita ATIVO ou DESATIVADO. */
    static EstadoConta lerEstado(String valor) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty()) {
            return null;
        }
        if (!texto.equals("ATIVO") && !texto.equals("DESATIVADO")) {
            new Validacao().erro("estado", "O estado tem de ser ATIVO ou DESATIVADO.").lancarSeHouverErros();
        }
        return EstadoConta.valueOf(texto);
    }

    static List<GestoresServico.NovoGestor> novosGestores(DesativarContaPedido pedido) {
        if (pedido == null || pedido.novosGestores() == null) {
            return List.of();
        }
        return pedido.novosGestores().stream()
                .map(n -> {
                    if (n == null) {
                        new Validacao().erro("novosGestores", "Indique o projeto e o novo Gestor.").lancarSeHouverErros();
                    }
                    return new GestoresServico.NovoGestor(
                            Identificadores.doCorpo(n.projetoId(), "novosGestores", "Indique o projeto."),
                            Identificadores.doCorpo(n.gestorId(), "novosGestores", "Indique o novo Gestor."));
                })
                .toList();
    }
}
