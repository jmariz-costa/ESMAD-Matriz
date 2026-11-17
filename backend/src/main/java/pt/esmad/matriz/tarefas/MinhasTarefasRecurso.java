package pt.esmad.matriz.tarefas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

/** Endpoint de "As minhas tarefas" (openapi.yaml, operação listarMinhasTarefas; US11). */
@Path("/minhas-tarefas")
@Produces(MediaType.APPLICATION_JSON)
public class MinhasTarefasRecurso {

    private final MinhasTarefasServico servico;

    public MinhasTarefasRecurso(MinhasTarefasServico servico) {
        this.servico = servico;
    }

    /** Os parâmetros são lidos como texto, para um valor inválido dar 400 com o campo indicado. */
    @GET
    public Pagina<MinhaTarefaResposta> listar(@QueryParam("concluidas") String concluidas,
                                              @QueryParam("pagina") String pagina,
                                              @QueryParam("tamanho") String tamanho) {
        boolean soConcluidas = lerConcluidas(concluidas);
        return servico.listar(soConcluidas, PedidoPagina.de(pagina, tamanho)).converter(MinhaTarefaResposta::de);
    }

    /** Vazio ou ausente = false; só aceita true ou false. */
    static boolean lerConcluidas(String valor) {
        String texto = valor == null ? "" : valor.strip();
        if (!texto.isEmpty() && !texto.equals("true") && !texto.equals("false")) {
            new Validacao().erro("concluidas", "O parâmetro concluidas tem de ser true ou false.").lancarSeHouverErros();
        }
        return texto.equals("true");
    }
}
