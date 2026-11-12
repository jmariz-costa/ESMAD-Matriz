package pt.esmad.matriz.historico;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Registo do histórico das tarefas (RI12), pedido pelo módulo Tarefas (ADR 0001, regra 5).
 * A consulta do histórico (US13) é da Fase 2.
 * Os métodos têm de ser chamados dentro da transação de quem altera a tarefa.
 */
@ApplicationScoped
public class HistoricoServico {

    /** Um campo alterado, com os valores já convertidos em texto ({@code null} quando o campo fica vazio). */
    public record Alteracao(String campo, String valorAnterior, String valorNovo) {
    }

    private final EntityManager entityManager;

    HistoricoServico(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void registarCriacao(UUID tarefaId, UUID autorId) {
        entityManager.persist(EntradaHistorico.criacao(tarefaId, autorId, Instant.now()));
    }

    /** Uma entrada por campo, todas com a mesma data e hora. */
    public void registarAlteracoes(UUID tarefaId, UUID autorId, List<Alteracao> alteracoes) {
        Instant agora = Instant.now();
        for (Alteracao alteracao : alteracoes) {
            entityManager.persist(EntradaHistorico.alteracao(tarefaId, autorId, agora,
                    alteracao.campo(), alteracao.valorAnterior(), alteracao.valorNovo()));
        }
    }
}
