package pt.esmad.matriz.contas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pt.esmad.matriz.comum.autenticacao.Publico;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;

/**
 * Endpoints de autenticação (openapi.yaml, tag Autenticação).
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AutenticacaoRecurso {

    public static final String COOKIE_SESSAO = "matriz_sessao";

    private final AutenticacaoServico servico;
    private final UtilizadorAtual utilizadorAtual;
    private final boolean cookieSeguro;

    public AutenticacaoRecurso(AutenticacaoServico servico, UtilizadorAtual utilizadorAtual,
                               @ConfigProperty(name = "matriz.sessao.cookie-seguro") boolean cookieSeguro) {
        this.servico = servico;
        this.utilizadorAtual = utilizadorAtual;
        this.cookieSeguro = cookieSeguro;
    }

    @POST
    @Path("/login")
    @Publico
    @Consumes(MediaType.APPLICATION_JSON)
    public Response entrar(@Valid @NotNull(message = "O corpo do pedido é obrigatório.") LoginPedido pedido) {
        AutenticacaoServico.SessaoIniciada sessao = servico.entrar(pedido.email(), pedido.palavraPasse());
        return Response.ok(UtilizadorAutenticado.de(sessao.utilizador()))
                .cookie(cookieSessao(sessao.token()))
                .build();
    }

    /** Responde 204 mesmo sem sessão válida, para que sair funcione sempre. */
    @POST
    @Path("/logout")
    @Publico
    public Response sair(@CookieParam(COOKIE_SESSAO) String token) {
        if (token != null && !token.isBlank()) {
            servico.sair(token);
        }
        return Response.noContent().cookie(cookieApagado()).build();
    }

    @GET
    @Path("/me")
    public UtilizadorAutenticado utilizadorAutenticado() {
        return UtilizadorAutenticado.de(utilizadorAtual.identidade());
    }

    /** Cookie sem prazo no browser: a validade é controlada pelo servidor (D06). */
    private NewCookie cookieSessao(String token) {
        return cookieBase().value(token).build();
    }

    /** Pede ao browser que apague o cookie (Max-Age=0). */
    private NewCookie cookieApagado() {
        return cookieBase().value("").maxAge(0).build();
    }

    private NewCookie.Builder cookieBase() {
        return new NewCookie.Builder(COOKIE_SESSAO)
                .path("/api")
                .httpOnly(true)
                .secure(cookieSeguro)
                .sameSite(NewCookie.SameSite.STRICT);
    }
}
