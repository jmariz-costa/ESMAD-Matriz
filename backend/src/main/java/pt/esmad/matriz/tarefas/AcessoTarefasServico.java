package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.projetos.AcessoProjetosServico;

import java.util.UUID;

/**
 * Verificação de acesso a uma tarefa, pedida pelos outros módulos (ADR 0001, regra 5),
 * por exemplo Comentários. Ordem: Administrador → 403 (RN19); tarefa inexistente → 404;
 * não membro do projeto da tarefa → 403 (RN20); para alterar, projeto arquivado → 409 (RN15).
 * Tem de ser chamado dentro da transação de quem pede.
 */
@ApplicationScoped
public class AcessoTarefasServico {

    private final TarefaRepositorio tarefas;
    private final AcessoProjetosServico projetos;
    private final UtilizadorAtual utilizadorAtual;

    AcessoTarefasServico(TarefaRepositorio tarefas, AcessoProjetosServico projetos, UtilizadorAtual utilizadorAtual) {
        this.tarefas = tarefas;
        this.projetos = projetos;
        this.utilizadorAtual = utilizadorAtual;
    }

    /** Para consultar: só membros do projeto da tarefa; o projeto pode estar arquivado. */
    public Identidade comoMembro(UUID tarefaId) {
        return projetos.comoMembro(projetoDa(tarefaId));
    }

    /** Para alterar (ex.: comentar): só membros, num projeto não arquivado. */
    public Identidade comoMembroParaAlterar(UUID tarefaId) {
        return projetos.comoMembroParaAlterar(projetoDa(tarefaId));
    }

    private UUID projetoDa(UUID tarefaId) {
        utilizadorAtual.exigirMembro();
        return tarefas.findByIdOptional(tarefaId).orElseThrow(ErroAplicacao::naoEncontrado).projetoId();
    }
}
