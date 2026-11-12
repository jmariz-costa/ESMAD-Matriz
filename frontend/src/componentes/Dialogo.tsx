import { useEffect, useId, useRef, type ReactNode } from 'react'

interface Props {
  titulo: string
  aoFechar: () => void
  children: ReactNode
  /** Mais largo, para conteúdos maiores (ex.: o detalhe da tarefa). */
  largo?: boolean
}

/**
 * Janela modal: foca o primeiro campo ou botão ao abrir, fecha com Esc e devolve o foco
 * ao elemento que estava ativo antes de abrir.
 */
export function Dialogo({ titulo, aoFechar, children, largo = false }: Props) {
  const idTitulo = useId()
  const caixa = useRef<HTMLDivElement>(null)
  const fechar = useRef(aoFechar)

  useEffect(() => {
    fechar.current = aoFechar
  })

  useEffect(() => {
    const anterior = document.activeElement as HTMLElement | null
    caixa.current?.querySelector<HTMLElement>('input, textarea, button')?.focus()
    function teclado(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        fechar.current()
      }
    }
    document.addEventListener('keydown', teclado)
    return () => {
      document.removeEventListener('keydown', teclado)
      anterior?.focus()
    }
  }, [])

  return (
    <div className="fundo-dialogo">
      <div ref={caixa} className={largo ? 'dialogo caixa largo' : 'dialogo caixa'} role="dialog" aria-modal="true" aria-labelledby={idTitulo}>
        <h2 id={idTitulo}>{titulo}</h2>
        {children}
      </div>
    </div>
  )
}
