// Estado da sessão partilhado pela aplicação (ver FornecedorSessao.tsx).
import { createContext, useContext } from 'react'
import type { UtilizadorAutenticado } from '../api/autenticacao'

export const MENSAGEM_SESSAO_TERMINADA = 'A sua sessão terminou. Inicie sessão novamente.'

export type EstadoSessao =
  | { tipo: 'a-verificar' }
  | { tipo: 'anonimo' }
  | { tipo: 'autenticado'; utilizador: UtilizadorAutenticado }

export interface ValorSessao {
  estado: EstadoSessao
  /** Mensagem a mostrar no início de sessão, por exemplo quando a sessão expirou. */
  mensagem: string | null
  entrar: (email: string, palavraPasse: string) => Promise<void>
  sair: () => Promise<void>
  /** Depois de mudar a palavra-passe: deixa de ser temporária (PR20). */
  palavraPasseMudada: () => void
}

export const ContextoSessao = createContext<ValorSessao | null>(null)

export function useSessao(): ValorSessao {
  const valor = useContext(ContextoSessao)
  if (valor === null) {
    throw new Error('useSessao tem de ser usado dentro de FornecedorSessao')
  }
  return valor
}

/** O utilizador autenticado; só pode ser usado em ecrãs protegidos por RotaProtegida. */
export function useUtilizador(): UtilizadorAutenticado {
  const { estado } = useSessao()
  if (estado.tipo !== 'autenticado') {
    throw new Error('useUtilizador só pode ser usado com sessão iniciada')
  }
  return estado.utilizador
}
