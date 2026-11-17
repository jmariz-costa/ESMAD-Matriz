import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as autenticacao from '../api/autenticacao'
import { definirAoTerminarSessao } from '../api/cliente'
import { ContextoSessao, MENSAGEM_SESSAO_TERMINADA, type EstadoSessao } from './sessao'

/**
 * Sabe se há sessão iniciada. Ao abrir a aplicação pergunta ao Backend (GET /auth/me);
 * depois, qualquer pedido com 401 termina a sessão e mostra a mensagem de sessão terminada (D06).
 */
export function FornecedorSessao({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSessao>({ tipo: 'a-verificar' })
  const [mensagem, setMensagem] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    autenticacao.utilizadorAtual()
      .then((utilizador) => ativo && setEstado({ tipo: 'autenticado', utilizador }))
      .catch(() => ativo && setEstado({ tipo: 'anonimo' }))
    return () => {
      ativo = false
    }
  }, [])

  useEffect(() => definirAoTerminarSessao(() => {
    setEstado({ tipo: 'anonimo' })
    setMensagem(MENSAGEM_SESSAO_TERMINADA)
  }), [])

  const entrar = useCallback(async (email: string, palavraPasse: string) => {
    const utilizador = await autenticacao.entrar(email, palavraPasse)
    setMensagem(null)
    setEstado({ tipo: 'autenticado', utilizador })
  }, [])

  const sair = useCallback(async () => {
    try {
      await autenticacao.sair()
    } catch {
      // Mesmo que o pedido falhe, o utilizador sai da aplicação neste browser
    } finally {
      setMensagem(null)
      setEstado({ tipo: 'anonimo' })
    }
  }, [])

  const palavraPasseMudada = useCallback(() => {
    setEstado((atual) => atual.tipo === 'autenticado'
      ? { tipo: 'autenticado', utilizador: { ...atual.utilizador, deveMudarPalavraPasse: false } }
      : atual)
  }, [])

  const valor = useMemo(() => ({ estado, mensagem, entrar, sair, palavraPasseMudada }),
    [estado, mensagem, entrar, sair, palavraPasseMudada])

  return <ContextoSessao.Provider value={valor}>{children}</ContextoSessao.Provider>
}
