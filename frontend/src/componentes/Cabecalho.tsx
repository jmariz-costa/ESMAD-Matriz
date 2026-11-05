import { NavLink } from 'react-router'
import { useSessao, useUtilizador } from '../sessao/sessao'

function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}

/** Cabeçalho dos ecrãs com sessão iniciada (ecrã 2 do wireframe). */
export function Cabecalho() {
  const utilizador = useUtilizador()
  const { sair } = useSessao()

  return (
    <header className="cabecalho">
      <span className="logo">MATRIZ</span>
      {utilizador.tipo === 'MEMBRO' && (
        <nav aria-label="Principal">
          <NavLink to="/" end>Início</NavLink>
          <NavLink to="/projetos">Projetos</NavLink>
        </nav>
      )}
      <div className="utilizador">
        <span className="avatar" aria-hidden="true">{iniciais(utilizador.nome)}</span>
        <span>{utilizador.nome}</span>
        <button type="button" className="btn" onClick={() => void sair()}>Sair</button>
      </div>
    </header>
  )
}
