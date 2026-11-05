package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.UUID;

/**
 * Atribuições de responsáveis, pedidas pelo módulo Projetos (ADR 0001, regra 5).
 * O resto da gestão de responsáveis é criado no I5.
 */
@ApplicationScoped
public class AtribuicoesServico {

    private final EntityManager entityManager;

    public AtribuicoesServico(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Quem é removido de um projeto deixa de ser responsável pelas tarefas não concluídas
     * desse projeto (RN08, RI04). Nas tarefas concluídas continua a constar.
     * Tem de ser chamado dentro da transação de quem remove o membro.
     */
    public int retirarDasTarefasNaoConcluidas(UUID projetoId, UUID utilizadorId) {
        return entityManager.createNativeQuery("""
                DELETE FROM atribuicao_tarefa a
                USING tarefa t
                WHERE a.tarefa_id = t.id
                  AND t.projeto_id = :projeto
                  AND a.utilizador_id = :utilizador
                  AND t.estado <> 'CONCLUIDA'
                """)
                .setParameter("projeto", projetoId)
                .setParameter("utilizador", utilizadorId)
                .executeUpdate();
    }
}
