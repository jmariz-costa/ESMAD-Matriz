package pt.esmad.matriz.tarefas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TarefaRepositorio implements PanacheRepositoryBase<Tarefa, UUID> {

    /** Filtros da listagem, combináveis (US17 crit. 3); {@code null} quando não se filtra. */
    record Filtros(Estado estado, FiltroPrioridade prioridade) {

        String condicoes() {
            StringBuilder where = new StringBuilder("t.projetoId = :projeto");
            if (estado != null) {
                where.append(" and t.estado = :estado");
            }
            if (prioridade == FiltroPrioridade.SEM_PRIORIDADE) {
                where.append(" and t.prioridade is null");
            } else if (prioridade != null) {
                where.append(" and t.prioridade = :prioridade");
            }
            return where.toString();
        }

        <T> TypedQuery<T> parametros(TypedQuery<T> consulta, UUID projetoId) {
            consulta.setParameter("projeto", projetoId);
            if (estado != null) {
                consulta.setParameter("estado", estado);
            }
            if (prioridade != null && prioridade != FiltroPrioridade.SEM_PRIORIDADE) {
                consulta.setParameter("prioridade", prioridade.prioridade());
            }
            return consulta;
        }
    }

    /** Ordenadas pelo prazo (o mais próximo primeiro, sem prazo no fim) e depois pelo título. */
    List<Tarefa> doProjeto(UUID projetoId, Filtros filtros, PedidoPagina pagina) {
        TypedQuery<Tarefa> consulta = getEntityManager().createQuery(
                "select t from Tarefa t where " + filtros.condicoes()
                        + " order by t.prazo asc nulls last, lower(t.titulo), t.id", Tarefa.class);
        return filtros.parametros(consulta, projetoId)
                .setFirstResult(pagina.inicio())
                .setMaxResults(pagina.tamanho())
                .getResultList();
    }

    long contarDoProjeto(UUID projetoId, Filtros filtros) {
        TypedQuery<Long> consulta = getEntityManager().createQuery(
                "select count(t) from Tarefa t where " + filtros.condicoes(), Long.class);
        return filtros.parametros(consulta, projetoId).getSingleResult();
    }

    private static final String MINHAS = """
            t.projetoId in :projetos
              and :utilizador member of t.responsaveis
            """;

    /**
     * "As minhas tarefas" (US11): não concluídas, nos projetos indicados, em que a pessoa é
     * responsável; ordenadas como no quadro (prazo, sem prazo no fim, e título).
     */
    List<Tarefa> porConcluirDe(UUID utilizadorId, Collection<UUID> projetos, PedidoPagina pagina) {
        return getEntityManager().createQuery("select t from Tarefa t where " + MINHAS
                        + " and t.estado <> :concluida order by t.prazo asc nulls last, lower(t.titulo), t.id",
                        Tarefa.class)
                .setParameter("projetos", projetos)
                .setParameter("utilizador", utilizadorId)
                .setParameter("concluida", Estado.CONCLUIDA)
                .setFirstResult(pagina.inicio())
                .setMaxResults(pagina.tamanho())
                .getResultList();
    }

    long contarPorConcluirDe(UUID utilizadorId, Collection<UUID> projetos) {
        return getEntityManager().createQuery("select count(t) from Tarefa t where " + MINHAS
                        + " and t.estado <> :concluida", Long.class)
                .setParameter("projetos", projetos)
                .setParameter("utilizador", utilizadorId)
                .setParameter("concluida", Estado.CONCLUIDA)
                .getSingleResult();
    }

    /** Todas as concluídas, sem ordem: a ordem (data de conclusão) vem do módulo Histórico. */
    List<Tarefa> concluidasDe(UUID utilizadorId, Collection<UUID> projetos) {
        return getEntityManager().createQuery("select t from Tarefa t where " + MINHAS
                        + " and t.estado = :concluida", Tarefa.class)
                .setParameter("projetos", projetos)
                .setParameter("utilizador", utilizadorId)
                .setParameter("concluida", Estado.CONCLUIDA)
                .getResultList();
    }

    /** Tarefas não concluídas de todos os projetos, incluindo arquivados, em que a pessoa é responsável (RI05). */
    List<Tarefa> naoConcluidasComResponsavel(UUID utilizadorId) {
        return getEntityManager().createQuery("""
                        select t from Tarefa t
                        where t.estado <> :concluida
                          and :utilizador member of t.responsaveis
                        """, Tarefa.class)
                .setParameter("concluida", Estado.CONCLUIDA)
                .setParameter("utilizador", utilizadorId)
                .getResultList();
    }

    /** Tarefas não concluídas do projeto em que a pessoa é responsável (RN08, RI04). */
    List<Tarefa> naoConcluidasComResponsavel(UUID projetoId, UUID utilizadorId) {
        return getEntityManager().createQuery("""
                        select t from Tarefa t
                        where t.projetoId = :projeto
                          and t.estado <> :concluida
                          and :utilizador member of t.responsaveis
                        """, Tarefa.class)
                .setParameter("projeto", projetoId)
                .setParameter("concluida", Estado.CONCLUIDA)
                .setParameter("utilizador", utilizadorId)
                .getResultList();
    }
}
