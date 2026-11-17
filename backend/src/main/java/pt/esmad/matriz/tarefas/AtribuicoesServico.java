package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.contas.ContasServico;
import pt.esmad.matriz.historico.HistoricoServico;

import java.util.List;
import java.util.UUID;

/**
 * Atribuições de responsáveis pedidas pelos módulos Projetos e Contas (ADR 0001, regra 5).
 * A atribuição e a remoção feitas na tarefa estão no {@link TarefasServico}.
 */
@ApplicationScoped
public class AtribuicoesServico {

    private final TarefaRepositorio tarefas;
    private final ContasServico contas;
    private final HistoricoServico historico;

    AtribuicoesServico(TarefaRepositorio tarefas, ContasServico contas, HistoricoServico historico) {
        this.tarefas = tarefas;
        this.contas = contas;
        this.historico = historico;
    }

    /**
     * Quem é removido de um projeto deixa de ser responsável pelas tarefas não concluídas
     * desse projeto (RN08, RI04); nas tarefas concluídas continua a constar. Cada tarefa
     * afetada aumenta a versão (D08) e regista a remoção no histórico, com quem removeu o
     * membro como autor (RI12). Tem de ser chamado dentro da transação de quem remove o membro.
     */
    public int retirarDasTarefasNaoConcluidas(UUID projetoId, UUID utilizadorId, UUID autorId) {
        return retirar(tarefas.naoConcluidasComResponsavel(projetoId, utilizadorId), utilizadorId, autorId);
    }

    /**
     * Quem é desativado deixa de ser responsável pelas tarefas não concluídas de todos os
     * projetos, incluindo arquivados (RN18, RI05); cada remoção fica no histórico com o
     * Administrador como autor. Tem de ser chamado dentro da transação da desativação.
     */
    public int retirarDeTodasAsTarefasNaoConcluidas(UUID utilizadorId, UUID autorId) {
        return retirar(tarefas.naoConcluidasComResponsavel(utilizadorId), utilizadorId, autorId);
    }

    private int retirar(List<Tarefa> afetadas, UUID utilizadorId, UUID autorId) {
        if (afetadas.isEmpty()) {
            return 0;
        }
        String nome = contas.pessoa(utilizadorId).nome();
        for (Tarefa tarefa : afetadas) {
            tarefa.removerResponsavel(utilizadorId);
            historico.registarAlteracoes(tarefa.id(), autorId,
                    List.of(new HistoricoServico.Alteracao("responsavel", nome, null)));
        }
        return afetadas.size();
    }
}
