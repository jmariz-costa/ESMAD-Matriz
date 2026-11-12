package pt.esmad.matriz.tarefas;

import java.time.LocalDate;

/** Dados para criar uma tarefa (openapi.yaml, CriarTarefaPedido). Só o título é obrigatório (RN10). */
public record NovaTarefa(String titulo, String descricao, LocalDate prazo, Prioridade prioridade) {
}
