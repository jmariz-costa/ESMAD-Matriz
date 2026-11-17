// Operações de autenticação (openapi.yaml, tag Autenticação).
import { pedido } from './cliente'

export interface UtilizadorAutenticado {
  id: string
  nome: string
  email: string
  tipo: 'MEMBRO' | 'ADMINISTRADOR'
  /** Depois de o Administrador repor o acesso, até definir uma palavra-passe nova (PR20). */
  deveMudarPalavraPasse: boolean
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

/** Só Membros (US20). As outras sessões terminam; esta mantém-se (PR21). */
export function mudarPalavraPasse(palavraPasseAtual: string, palavraPasseNova: string): Promise<void> {
  return pedido('/auth/palavra-passe', { metodo: 'POST', corpo: { palavraPasseAtual, palavraPasseNova } })
}
