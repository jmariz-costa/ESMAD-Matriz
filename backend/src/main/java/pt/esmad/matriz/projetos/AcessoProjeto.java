package pt.esmad.matriz.projetos;

import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.UUID;

/**
 * Carrega um projeto verificando o acesso de quem tem a sessão (S03), na ordem:
 * Administrador → 403 (RN19); projeto inexistente → 404; não membro → 403 (RN20);
 * não Gestor, nas operações só do Gestor → 403 (RN22).
 */
@ApplicationScoped
class AcessoProjeto {

    private final ProjetoRepositorio projetos;
    private final UtilizadorAtual utilizadorAtual;

    AcessoProjeto(ProjetoRepositorio projetos, UtilizadorAtual utilizadorAtual) {
        this.projetos = projetos;
        this.utilizadorAtual = utilizadorAtual;
    }

    record ProjetoAcessivel(Projeto projeto, Identidade quem) {
    }

    ProjetoAcessivel comoMembro(UUID projetoId) {
        Identidade quem = utilizadorAtual.exigirMembro();
        Projeto projeto = projetos.findByIdOptional(projetoId).orElseThrow(ErroAplicacao::naoEncontrado);
        if (!projeto.eMembro(quem.id())) {
            throw ErroAplicacao.semPermissao();
        }
        return new ProjetoAcessivel(projeto, quem);
    }

    ProjetoAcessivel comoGestor(UUID projetoId) {
        ProjetoAcessivel acesso = comoMembro(projetoId);
        if (!acesso.projeto().eGestor(acesso.quem().id())) {
            throw ErroAplicacao.semPermissao();
        }
        return acesso;
    }
}
