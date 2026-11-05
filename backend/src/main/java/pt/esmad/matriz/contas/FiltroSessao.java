package pt.esmad.matriz.contas;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Cookie;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import pt.esmad.matriz.comum.autenticacao.Publico;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;

/**
 * Identifica o utilizador pelo cookie de sessão em todos os pedidos à API,
 * exceto nos endpoints marcados com {@link Publico} (D06).
 */
public class FiltroSessao {

    private final AutenticacaoServico servico;
    private final UtilizadorAtual utilizadorAtual;

    public FiltroSessao(AutenticacaoServico servico, UtilizadorAtual utilizadorAtual) {
        this.servico = servico;
        this.utilizadorAtual = utilizadorAtual;
    }

    @ServerRequestFilter
    public void identificar(ContainerRequestContext pedido, ResourceInfo recurso) {
        if (ePublico(recurso)) {
            return;
        }
        Cookie cookie = pedido.getCookies().get(AutenticacaoRecurso.COOKIE_SESSAO);
        if (cookie == null || cookie.getValue().isBlank()) {
            throw UtilizadorAtual.naoAutenticado();
        }
        utilizadorAtual.definir(servico.validarSessao(cookie.getValue())
                .orElseThrow(UtilizadorAtual::naoAutenticado));
    }

    private static boolean ePublico(ResourceInfo recurso) {
        return recurso.getResourceMethod().isAnnotationPresent(Publico.class)
                || recurso.getResourceClass().isAnnotationPresent(Publico.class);
    }
}
