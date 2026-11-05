package pt.esmad.matriz.contas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

/** GET /utilizadores (openapi.yaml, procurarUtilizadores). */
@Path("/utilizadores")
@Produces(MediaType.APPLICATION_JSON)
public class UtilizadoresRecurso {

    private final ContasServico servico;

    public UtilizadoresRecurso(ContasServico servico) {
        this.servico = servico;
    }

    @GET
    public Pagina<UtilizadorResumo> procurar(@QueryParam("q") String q,
                                             @QueryParam("pagina") String pagina,
                                             @QueryParam("tamanho") String tamanho) {
        return servico.procurarMembrosAtivos(q, PedidoPagina.de(pagina, tamanho));
    }
}
