package pt.esmad.matriz.comum.autenticacao;

import jakarta.enterprise.context.RequestScoped;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

/**
 * Identidade do pedido atual, preenchida pelo filtro de sessão.
 * Os serviços usam-na para saber quem pediu e para verificar permissões (S03).
 */
@RequestScoped
public class UtilizadorAtual {

    public static final String MENSAGEM_SESSAO_INVALIDA = "A sessão não é válida. Inicie sessão novamente.";

    private Identidade identidade;

    public void definir(Identidade identidade) {
        this.identidade = identidade;
    }

    /** A identidade da sessão; 401 se o pedido não tiver sessão válida. */
    public Identidade identidade() {
        if (identidade == null) {
            throw naoAutenticado();
        }
        return identidade;
    }

    /**
     * A identidade de um Membro. O Administrador não participa em projetos (RN19)
     * e recebe 403 em todas as operações de projetos, tarefas e comentários.
     */
    public Identidade exigirMembro() {
        Identidade quem = identidade();
        if (quem.administrador()) {
            throw ErroAplicacao.semPermissao();
        }
        return quem;
    }

    public static ErroAplicacao naoAutenticado() {
        return new ErroAplicacao(401, CodigoErro.NAO_AUTENTICADO, MENSAGEM_SESSAO_INVALIDA);
    }
}
