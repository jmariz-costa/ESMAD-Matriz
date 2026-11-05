package pt.esmad.matriz.comum.erros;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.Comparator;
import java.util.List;

/**
 * Converte as exceções em respostas com o formato único de erro.
 */
public class MapeadoresErro {

    private static final String PEDIDO_INVALIDO = "O pedido tem dados inválidos.";

    @ServerExceptionMapper
    public Response erroAplicacao(ErroAplicacao erro) {
        return resposta(erro.estadoHttp(), new Erro(erro.codigo(), erro.getMessage(), erro.detalhes()));
    }

    /** Falhas de Bean Validation nos DTOs de pedido. */
    @ServerExceptionMapper
    public Response validacao(ConstraintViolationException erro) {
        List<Erro.Detalhe> detalhes = erro.getConstraintViolations().stream()
                .map(v -> new Erro.Detalhe(campo(v), v.getMessage()))
                .sorted(Comparator.comparing(Erro.Detalhe::campo))
                .toList();
        return resposta(400, new Erro(CodigoErro.VALIDACAO, PEDIDO_INVALIDO, detalhes));
    }

    /**
     * Tipos errados ou campos desconhecidos. Tem de ser deste tipo exato para
     * se sobrepor ao mapeador que o Quarkus traz para esta exceção.
     */
    @ServerExceptionMapper
    public Response jsonComCamposInvalidos(MismatchedInputException erro) {
        return resposta(400, Erro.de(CodigoErro.VALIDACAO, PEDIDO_INVALIDO));
    }

    /**
     * O Quarkus embrulha o JSON mal formado num WebApplicationException 400.
     * As restantes WebApplicationException (por exemplo 404 ou 405) mantêm a resposta original.
     */
    @ServerExceptionMapper
    public Response pedidoHttp(WebApplicationException erro) {
        if (erro.getCause() instanceof JsonProcessingException) {
            return resposta(400, Erro.de(CodigoErro.VALIDACAO, PEDIDO_INVALIDO));
        }
        return erro.getResponse();
    }

    /** Último nó do caminho da violação, por exemplo {@code email}; vazio quando é o próprio corpo. */
    private static String campo(ConstraintViolation<?> violacao) {
        String nome = "";
        for (Path.Node no : violacao.getPropertyPath()) {
            if (no.getKind() == jakarta.validation.ElementKind.PROPERTY) {
                nome = no.getName();
            }
        }
        return nome.isEmpty() ? "corpo" : nome;
    }

    private static Response resposta(int estado, Erro erro) {
        return Response.status(estado).type(MediaType.APPLICATION_JSON).entity(erro).build();
    }
}
