import { useCallback, useEffect, useState, type ReactNode } from 'react'
import { ContextoNotificacao } from '../comum/notificacao'

const DURACAO_MS = 4000

/**
 * Mensagem de sucesso no canto do ecrã, anunciada pelos leitores de ecrã (role="status").
 * Desaparece ao fim de 4 segundos; uma nova substitui a anterior. Mantém-se ao mudar de ecrã
 * (ex.: "Projeto eliminado." depois de voltar à lista de projetos).
 */
export function FornecedorNotificacao({ children }: { children: ReactNode }) {
  const [mensagem, setMensagem] = useState<{ texto: string; n: number } | null>(null)

  const notificar = useCallback((texto: string) => {
    setMensagem((atual) => ({ texto, n: (atual?.n ?? 0) + 1 }))
  }, [])

  useEffect(() => {
    if (!mensagem) return
    const espera = setTimeout(() => setMensagem(null), DURACAO_MS)
    return () => clearTimeout(espera)
  }, [mensagem])

  return (
    <ContextoNotificacao.Provider value={notificar}>
      {children}
      <div className="notificacao-zona" role="status" aria-live="polite">
        {mensagem && <div key={mensagem.n} className="notificacao caixa">{mensagem.texto}</div>}
      </div>
    </ContextoNotificacao.Provider>
  )
}
