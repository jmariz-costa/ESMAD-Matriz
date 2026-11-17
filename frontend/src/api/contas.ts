// Gestão de contas pelo Administrador (openapi.yaml, tag Contas; US14).
import { pedido } from './cliente'
import { TAMANHO_PAGINA, type Pagina } from './projetos'

export type EstadoConta = 'ATIVO' | 'DESATIVADO'

export interface Conta {
  id: string
  nome: string
  email: string
  estado: EstadoConta
}

/** Só o necessário para nomear o novo Gestor (S04). */
export interface ProjetoGerido {
  id: string
  nome: string
  arquivado: boolean
}

export interface NovoGestor {
  projetoId: string
  gestorId: string
}

/** No máximo 50 pessoas (RNF04): uma página de 100 chega para escolher o novo Gestor. */
export const TAMANHO_MAXIMO = 100

export function listarContas(filtro: { q?: string; estado?: EstadoConta; pagina?: number; tamanho?: number } = {}):
  Promise<Pagina<Conta>> {
  const parametros = new URLSearchParams()
  if (filtro.q) {
    parametros.set('q', filtro.q)
  }
  if (filtro.estado) {
    parametros.set('estado', filtro.estado)
  }
  parametros.set('pagina', String(filtro.pagina ?? 1))
  parametros.set('tamanho', String(filtro.tamanho ?? TAMANHO_PAGINA))
  return pedido(`/contas?${parametros}`)
}

export function listarProjetosGeridos(contaId: string): Promise<ProjetoGerido[]> {
  return pedido(`/contas/${encodeURIComponent(contaId)}/projetos-geridos`)
}

/** Novos Gestores, saída das tarefas por concluir, desativação e fim das sessões, tudo de uma vez. */
export function desativarConta(contaId: string, novosGestores: NovoGestor[]): Promise<Conta> {
  return pedido(`/contas/${encodeURIComponent(contaId)}/desativacao`, { metodo: 'POST', corpo: { novosGestores } })
}

export function reativarConta(contaId: string): Promise<Conta> {
  return pedido(`/contas/${encodeURIComponent(contaId)}/reativacao`, { metodo: 'POST' })
}

/** Só o nome (US19 crit. 1); o email não se altera. */
export function editarConta(contaId: string, nome: string): Promise<Conta> {
  return pedido(`/contas/${encodeURIComponent(contaId)}`, { metodo: 'PATCH', corpo: { nome } })
}

/** Palavra-passe temporária, a mudar no próximo login (US19 crit. 2 e 3, PR20). */
export function reporAcesso(contaId: string, palavraPasseTemporaria: string): Promise<void> {
  return pedido(`/contas/${encodeURIComponent(contaId)}/reposicao-acesso`, {
    metodo: 'POST', corpo: { palavraPasseTemporaria },
  })
}
