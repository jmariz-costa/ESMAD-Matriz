package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contagens de tarefas por estado, pedidas pelo módulo Projetos (ADR 0001, regra 5).
 * O resto do módulo Tarefas é criado no I4.
 */
@ApplicationScoped
public class ContagemTarefasServico {

    private final EntityManager entityManager;

    public ContagemTarefasServico(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Contagens de cada projeto pedido, numa única consulta. Projetos sem tarefas ficam a zero. */
    public Map<UUID, ContagensEstado> porProjeto(Collection<UUID> projetoIds) {
        Map<UUID, ContagensEstado> resultado = new HashMap<>();
        projetoIds.forEach(id -> resultado.put(id, ContagensEstado.ZERO));
        if (projetoIds.isEmpty()) {
            return resultado;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = entityManager.createNativeQuery("""
                SELECT projeto_id, estado, count(*)
                FROM tarefa
                WHERE projeto_id IN (:ids)
                GROUP BY projeto_id, estado
                """)
                .setParameter("ids", projetoIds)
                .getResultList();
        for (Object[] linha : linhas) {
            UUID projetoId = (UUID) linha[0];
            resultado.merge(projetoId, ContagensEstado.ZERO,
                    (atual, zero) -> atual.somar((String) linha[1], ((Number) linha[2]).longValue()));
        }
        return resultado;
    }

    public ContagensEstado doProjeto(UUID projetoId) {
        return porProjeto(List.of(projetoId)).get(projetoId);
    }
}
