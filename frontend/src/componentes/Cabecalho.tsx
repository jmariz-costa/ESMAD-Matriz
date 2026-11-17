import { useState } from 'react'
import { NavLink } from 'react-router'
import { MudarPalavraPasse } from '../contas/MudarPalavraPasse'
import { useSessao, useUtilizador } from '../sessao/sessao'
import { BotaoTema } from './BotaoTema'

function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/)
  const primeira = partes[0]?.[0] ?? ''
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : ''
  return (primeira + ultima).toUpperCase()
}

/**
 * Cabeçalho dos ecrãs com sessão iniciada (ecrã 2 do wireframe). Nos Membros, o nome abre o
 * diálogo para mudar a palavra-passe (US20); a do Administrador é definida na instalação (RN21).
 */
export function Cabecalho() {
  const utilizador = useUtilizador()
  const { sair } = useSessao()
  const [aMudar, setAMudar] = useState(false)

  return (
    <header className="cabecalho">
      <span className="logo">MATRIZ</span>
      {utilizador.tipo === 'MEMBRO' && (
        <nav aria-label="Principal">
          <NavLink to="/" end>Início</NavLink>
          <NavLink to="/projetos">Projetos</NavLink>
          <NavLink to="/minhas-tarefas">As minhas tarefas</NavLink>
        </nav>
      )}
      <div className="utilizador">
        <BotaoTema />
        <span className="avatar" aria-hidden="true">{iniciais(utilizador.nome)}</span>
        {utilizador.tipo === 'MEMBRO' ? (
          <button type="button" className="nome-utilizador" onClick={() => setAMudar(true)}
            aria-haspopup="dialog" title="Mudar a palavra-passe">
            {utilizador.nome}<span className="so-leitores"> — mudar a palavra-passe</span>
          </button>
        ) : (
          <span>{utilizador.nome}</span>
        )}
        <button type="button" className="btn" onClick={() => void sair()}>Sair</button>
      </div>
      {aMudar && <MudarPalavraPasse aoFechar={() => setAMudar(false)} />}
    </header>
  )
}
