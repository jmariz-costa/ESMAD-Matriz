import type { Contagens } from '../api/projetos'

export function totalTarefas(contagens: Contagens): number {
  return contagens.A_FAZER + contagens.EM_CURSO + contagens.EM_REVISAO + contagens.CONCLUIDA
}

/**
 * Percentagem de conclusão (US10, PR09): concluídas ÷ total × 100, arredondada para baixo.
 * Sem tarefas é 0%; só é 100% quando todas estão concluídas.
 */
export function percentagemConclusao(contagens: Contagens): number {
  const total = totalTarefas(contagens)
  return total === 0 ? 0 : Math.floor((contagens.CONCLUIDA * 100) / total)
}

/** "1 tarefa", "3 tarefas". */
export function textoTarefas(total: number): string {
  return total === 1 ? '1 tarefa' : `${total} tarefas`
}
