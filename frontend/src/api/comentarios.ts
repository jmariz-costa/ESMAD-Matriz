// Comentários de uma tarefa (openapi.yaml, tag Comentários).
import { pedido } from './cliente'
import type { Pagina, PessoaResumo } from './projetos'

export interface Comentario {
  id: string
  tarefaId: string
  autor: PessoaResumo
  texto: string
  /** Data e hora ISO 8601, definida pelo Backend (US12 crit. 1). */
  criadoEm: string
  editadoEm: string | null
}

export const TAMANHO_COMENTARIOS = 20

/** Do mais antigo para o mais recente. */
export function listarComentarios(tarefaId: string, pagina = 1): Promise<Pagina<Comentario>> {
  return pedido(`/tarefas/${encodeURIComponent(tarefaId)}/comentarios?pagina=${pagina}&tamanho=${TAMANHO_COMENTARIOS}`)
}

/** Só o texto: o autor e a data são definidos pelo Backend. */
export function comentarTarefa(tarefaId: string, texto: string): Promise<Comentario> {
  return pedido(`/tarefas/${encodeURIComponent(tarefaId)}/comentarios`, { metodo: 'POST', corpo: { texto } })
}
