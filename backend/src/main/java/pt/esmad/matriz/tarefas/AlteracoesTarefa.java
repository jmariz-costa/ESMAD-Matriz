package pt.esmad.matriz.tarefas;

import java.time.LocalDate;

/**
 * Corpo de PATCH /tarefas/{id} já lido (openapi.yaml, EditarTarefaPedido).
 * Cada campo opcional tem um indicador, porque enviar {@code null} limpa o campo e
 * omiti-lo deixa-o como está. O título e o estado nunca ficam vazios: {@code null} = não altera.
 */
public record AlteracoesTarefa(int versao,
                               String titulo,
                               boolean alteraDescricao, String descricao,
                               boolean alteraPrazo, LocalDate prazo,
                               boolean alteraPrioridade, Prioridade prioridade,
                               Estado estado) {
}
