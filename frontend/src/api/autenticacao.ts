// Operações de autenticação (openapi.yaml, tag Autenticação).
import { pedido } from './cliente'

export interface UtilizadorAutenticado {
  id: string
  nome: string
  email: string
  tipo: 'MEMBRO' | 'ADMINISTRADOR'
}

export function entrar(email: string, palavraPasse: string): Promise<UtilizadorAutenticado> {
  return pedido('/auth/login', { metodo: 'POST', corpo: { email, palavraPasse }, ignorar401: true })
}

export function sair(): Promise<void> {
  return pedido('/auth/logout', { metodo: 'POST' })
}

/** Usado ao abrir a aplicação: um 401 significa apenas que ainda não há sessão. */
export function utilizadorAtual(): Promise<UtilizadorAutenticado> {
  return pedido('/auth/me', { ignorar401: true })
}
