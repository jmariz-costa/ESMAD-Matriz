// Operações de tarefas (openapi.yaml, tag Tarefas).
import { pedido } from './cliente'
import type { Estado, Pagina, PessoaResumo } from './projetos'

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA'

/** Filtro de prioridade; SEM_PRIORIDADE seleciona as tarefas sem prioridade (US17 crit. 3). */
export type FiltroPrioridade = Prioridade | 'SEM_PRIORIDADE'

export interface Tarefa {
  id: string
  projetoId: string
  titulo: string
  descricao: string | null
  /** Data no formato AAAA-MM-DD. */
  prazo: string | null
  prioridade: Prioridade | null
  estado: Estado
  /** Ordenados pelo nome; vazio = "Sem responsável" (US07 crit. 3). */
  responsaveis: PessoaResumo[]
  versao: number
}

export interface DadosTarefa {
  titulo: string
  descricao: string | null
  prazo: string | null
  prioridade: Prioridade | null
}

/** A versão é obrigatória (D08); os campos omitidos não são alterados e null limpa um campo opcional. */
export type AlteracoesTarefa = Partial<DadosTarefa> & { versao: number; estado?: Estado }

/** Cada coluna do quadro mostra até 20 tarefas; "Ver mais" carrega as seguintes. */
export const TAMANHO_COLUNA = 20

export interface FiltroTarefas {
  estado?: Estado
  prioridade?: FiltroPrioridade
  pagina?: number
  tamanho?: number
}

export function listarTarefas(projetoId: string, filtro: FiltroTarefas = {}): Promise<Pagina<Tarefa>> {
  const parametros = new URLSearchParams()
  if (filtro.estado) {
    parametros.set('estado', filtro.estado)
  }
  if (filtro.prioridade) {
    parametros.set('prioridade', filtro.prioridade)
  }
  parametros.set('pagina', String(filtro.pagina ?? 1))
  parametros.set('tamanho', String(filtro.tamanho ?? TAMANHO_COLUNA))
  return pedido(`/projetos/${encodeURIComponent(projetoId)}/tarefas?${parametros}`)
}

export function consultarTarefa(id: string): Promise<Tarefa> {
  return pedido(`/tarefas/${encodeURIComponent(id)}`)
}

export function criarTarefa(projetoId: string, dados: DadosTarefa): Promise<Tarefa> {
  return pedido(`/projetos/${encodeURIComponent(projetoId)}/tarefas`, { metodo: 'POST', corpo: dados })
}

export function editarTarefa(id: string, alteracoes: AlteracoesTarefa): Promise<Tarefa> {
  return pedido(`/tarefas/${encodeURIComponent(id)}`, { metodo: 'PATCH', corpo: alteracoes })
}

export function eliminarTarefa(id: string): Promise<void> {
  return pedido(`/tarefas/${encodeURIComponent(id)}`, { metodo: 'DELETE' })
}

/** Qualquer membro; só membros do projeto com conta ativa (RI02). Devolve a tarefa atualizada. */
export function atribuirResponsavel(tarefaId: string, utilizadorId: string): Promise<Tarefa> {
  return pedido(`/tarefas/${encodeURIComponent(tarefaId)}/responsaveis`, { metodo: 'POST', corpo: { utilizadorId } })
}

/** Remove só esta atribuição (US05 crit. 7). */
export function removerResponsavel(tarefaId: string, utilizadorId: string): Promise<void> {
  return pedido(`/tarefas/${encodeURIComponent(tarefaId)}/responsaveis/${encodeURIComponent(utilizadorId)}`,
    { metodo: 'DELETE' })
}
