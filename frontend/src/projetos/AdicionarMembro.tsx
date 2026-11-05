import { useState } from 'react'
import { comoErroApi } from '../api/cliente'
import { adicionarMembro, procurarUtilizadores, type UtilizadorResumo } from '../api/membros'
import { Dialogo } from '../componentes/Dialogo'
import { useAtrasado } from '../comum/useAtrasado'
import { useDados } from '../comum/useDados'

interface Props {
  projetoId: string
  /** Ids de quem já é membro, para não aparecerem nos resultados. */
  membros: ReadonlySet<string>
  aoFechar: () => void
  aoAdicionar: () => void
}

const ESPERA_PESQUISA_MS = 300

/**
 * Pesquisa de Membros ativos (RI16) para adicionar ao projeto (US04 crit. 1).
 * A janela fica aberta para se poderem adicionar várias pessoas seguidas.
 */
export function AdicionarMembro({ projetoId, membros, aoFechar, aoAdicionar }: Props) {
  const [texto, setTexto] = useState('')
  const pesquisa = useAtrasado(texto.trim(), ESPERA_PESQUISA_MS)
  const [resultados] = useDados(() => procurarUtilizadores({ q: pesquisa }), pesquisa)
  const [aAdicionar, setAAdicionar] = useState<string | null>(null)
  const [aviso, setAviso] = useState<{ tipo: 'sucesso' | 'erro'; texto: string } | null>(null)

  async function adicionar(pessoa: UtilizadorResumo) {
    setAviso(null)
    setAAdicionar(pessoa.id)
    try {
      await adicionarMembro(projetoId, pessoa.id)
      setAviso({ tipo: 'sucesso', texto: `${pessoa.nome} foi adicionado ao projeto.` })
      aoAdicionar()
    } catch (e) {
      setAviso({ tipo: 'erro', texto: comoErroApi(e).message })
    } finally {
      setAAdicionar(null)
    }
  }

  const candidatos = resultados.estado === 'pronto'
    ? resultados.dados.itens.filter((pessoa) => !membros.has(pessoa.id))
    : []

  return (
    <Dialogo titulo="Adicionar membro" aoFechar={aoFechar}>
      <label htmlFor="pesquisa-membro">Pesquisar pelo nome ou email</label>
      <input
        id="pesquisa-membro"
        type="search"
        className="campo"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
      />

      {aviso && (
        <div className="aviso" role={aviso.tipo === 'erro' ? 'alert' : 'status'}>{aviso.texto}</div>
      )}

      {resultados.estado === 'a-carregar' && <p className="suave">A procurar…</p>}
      {resultados.estado === 'erro' && <p role="alert">Não foi possível procurar. {resultados.erro.message}</p>}
      {resultados.estado === 'pronto' && candidatos.length === 0 && (
        <p className="suave">Nenhum Membro encontrado.</p>
      )}
      {candidatos.length > 0 && (
        <ul className="lista-pessoas">
          {candidatos.map((pessoa) => (
            <li key={pessoa.id}>
              <span>
                {pessoa.nome}
                <small className="suave"> {pessoa.email}</small>
              </span>
              <button
                type="button"
                className="btn"
                onClick={() => void adicionar(pessoa)}
                disabled={aAdicionar !== null}
                aria-label={`Adicionar ${pessoa.nome}`}
              >
                {aAdicionar === pessoa.id ? 'A adicionar…' : 'Adicionar'}
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="botoes">
        <button type="button" className="btn" onClick={aoFechar}>Fechar</button>
      </div>
    </Dialogo>
  )
}
