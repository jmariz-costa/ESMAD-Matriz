import { useId, useState } from 'react'
import { comoErroApi } from '../api/cliente'
import { listarMembros, type Membro } from '../api/membros'
import type { PessoaResumo } from '../api/projetos'
import { atribuirResponsavel, consultarTarefa, removerResponsavel, type Tarefa } from '../api/tarefas'

interface Props {
  tarefa: Tarefa
  /** Projeto arquivado: só leitura (RN15). */
  soLeitura: boolean
  /** A tarefa depois de atribuir ou remover (a versão mudou). */
  aoMudar: (tarefa: Tarefa) => void
}

/**
 * Responsáveis da tarefa (Ficha PL 03, Exercício 6; wireframe.html, nota 9): vários por tarefa,
 * ✕ remove só essa atribuição (US05 crit. 7) e "+ Adicionar" mostra só os membros ativos do
 * projeto que ainda não são responsáveis (RI02, CE8). Cada ação fica gravada logo, sem "Guardar".
 */
export function ResponsaveisTarefa({ tarefa, soLeitura, aoMudar }: Props) {
  const idTitulo = useId()
  const [aEscolher, setAEscolher] = useState(false)
  const [membros, setMembros] = useState<Membro[] | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  const atuais = new Set(tarefa.responsaveis.map((r) => r.id))
  const elegiveis = (membros ?? []).filter((m) => m.ativo && !atuais.has(m.id))

  async function abrirLista() {
    setAviso(null)
    setAEscolher(true)
    if (membros === null) {
      try {
        setMembros(await listarMembros(tarefa.projetoId))
      } catch (e) {
        setAviso(comoErroApi(e).message)
        setAEscolher(false)
      }
    }
  }

  async function executar(acao: () => Promise<Tarefa>) {
    setOcupado(true)
    setAviso(null)
    try {
      aoMudar(await acao())
      setAEscolher(false)
    } catch (e) {
      setAviso(comoErroApi(e).message)
    } finally {
      setOcupado(false)
    }
  }

  const atribuir = (membro: Membro) => executar(() => atribuirResponsavel(tarefa.id, membro.id))

  const remover = (pessoa: PessoaResumo) => executar(async () => {
    await removerResponsavel(tarefa.id, pessoa.id)
    return consultarTarefa(tarefa.id)
  })

  return (
    <div className="responsaveis-tarefa" role="group" aria-labelledby={idTitulo}>
      <div id={idTitulo} className="rotulo">Responsáveis</div>
      <ul className="etiquetas">
        {tarefa.responsaveis.length === 0 && <li className="sem-resp">Sem responsável</li>}
        {tarefa.responsaveis.map((pessoa) => (
          <li key={pessoa.id} className="chip">
            {pessoa.nome}
            {!soLeitura && (
              <button type="button" className="remover-chip" aria-label={`Remover ${pessoa.nome}`}
                disabled={ocupado} onClick={() => void remover(pessoa)}>✕</button>
            )}
          </li>
        ))}
        {!soLeitura && !aEscolher && (
          <li>
            <button type="button" className="btn pequeno" disabled={ocupado} onClick={() => void abrirLista()}>
              + Adicionar
            </button>
          </li>
        )}
      </ul>

      {aEscolher && (
        <div className="escolher caixa">
          {membros === null && <p className="suave">A carregar os membros…</p>}
          {membros !== null && elegiveis.length === 0 && (
            <p className="suave">Todos os membros ativos do projeto já são responsáveis.</p>
          )}
          {elegiveis.length > 0 && (
            <ul className="lista-escolha" aria-label="Membros que podem ser responsáveis">
              {elegiveis.map((membro) => (
                <li key={membro.id}>
                  <button type="button" className="btn pequeno" disabled={ocupado} onClick={() => void atribuir(membro)}>
                    {membro.nome}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <button type="button" className="btn pequeno" onClick={() => setAEscolher(false)}>Fechar lista</button>
        </div>
      )}

      {aviso && <div className="aviso" role="alert">{aviso}</div>}
    </div>
  )
}
