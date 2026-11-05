// Operações de projetos (openapi.yaml, tag Projetos).
import { pedido } from './cliente'

export type Estado = 'A_FAZER' | 'EM_CURSO' | 'EM_REVISAO' | 'CONCLUIDA'

export type Contagens = Record<Estado, number>

export interface PessoaResumo {
  id: string
  nome: string
}

export interface Projeto {
  id: string
  nome: string
  descricao: string | null
  arquivado: boolean
  gestor: PessoaResumo
  /** Só para mostrar ou esconder opções; o servidor verifica sempre a permissão (S03). */
  souGestor: boolean
  contagens: Contagens
}

export interface Pagina<T> {
  itens: T[]
  total: number
  pagina: number
  tamanho: number
}

export interface DadosProjeto {
  nome: string
  descricao: string | null
}

export const TAMANHO_PAGINA = 20

export function listarProjetos(filtro: { q?: string; pagina?: number; tamanho?: number } = {}): Promise<Pagina<Projeto>> {
  const parametros = new URLSearchParams()
  if (filtro.q) {
    parametros.set('q', filtro.q)
  }
  parametros.set('pagina', String(filtro.pagina ?? 1))
  parametros.set('tamanho', String(filtro.tamanho ?? TAMANHO_PAGINA))
  return pedido(`/projetos?${parametros}`)
}

export function consultarProjeto(id: string): Promise<Projeto> {
  return pedido(`/projetos/${encodeURIComponent(id)}`)
}

export function criarProjeto(dados: DadosProjeto): Promise<Projeto> {
  return pedido('/projetos', { metodo: 'POST', corpo: dados })
}

export function editarProjeto(id: string, dados: Partial<DadosProjeto>): Promise<Projeto> {
  return pedido(`/projetos/${encodeURIComponent(id)}`, { metodo: 'PATCH', corpo: dados })
}

export function eliminarProjeto(id: string): Promise<void> {
  return pedido(`/projetos/${encodeURIComponent(id)}`, { metodo: 'DELETE' })
}
