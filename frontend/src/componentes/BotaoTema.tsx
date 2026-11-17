import { useTema } from '../comum/tema'

function IconeLua() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor"
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  )
}

function IconeSol() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor"
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

/** Botão do cabeçalho que alterna entre o tema claro e o escuro (premido = escuro). */
export function BotaoTema() {
  const [tema, alternar] = useTema()
  const escuro = tema === 'escuro'

  return (
    <button
      type="button"
      className="btn tema"
      aria-pressed={escuro}
      title={escuro ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'}
      onClick={alternar}
    >
      {escuro ? <IconeSol /> : <IconeLua />}
      <span className="so-leitores">Tema escuro</span>
    </button>
  )
}
