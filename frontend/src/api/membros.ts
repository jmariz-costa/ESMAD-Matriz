// Membros de um projeto e pesquisa de Membros ativos (openapi.yaml, tag Membros).
import { pedido } from './cliente'
import type { Pagina } from './projetos'

export interface Membro {
  id: string
  nome: string
  email: string
  /** Falso se a conta estiver desativada (PR15). */
  ativo: boolean
  gestor: boolean
}

export interface UtilizadorResumo {
  id: string
  nome: string
  email: string
}

const caminho = (projetoId: string) => `/projetos/${encodeURIComponent(projetoId)}/membros`

export function listarMembros(projetoId: string): Promise<Membro[]> {
  return pedido(caminho(projetoId))
}

export function adicionarMembro(projetoId: string, utilizadorId: string): Promise<Membro> {
  return pedido(caminho(projetoId), { metodo: 'POST', corpo: { utilizadorId } })
}

export function removerMembro(projetoId: string, utilizadorId: string): Promise<void> {
  return pedido(`${caminho(projetoId)}/${encodeURIComponent(utilizadorId)}`, { metodo: 'DELETE' })
}

export function procurarUtilizadores(filtro: { q?: string; tamanho?: number } = {}): Promise<Pagina<UtilizadorResumo>> {
  const parametros = new URLSearchParams()
  if (filtro.q) {
    parametros.set('q', filtro.q)
  }
  parametros.set('pagina', '1')
  parametros.set('tamanho', String(filtro.tamanho ?? 10))
  return pedido(`/utilizadores?${parametros}`)
}
