import { useState, type DragEvent } from 'react'
import { useSearchParams } from 'react-router'
import { comoErroApi } from '../api/cliente'
import type { Estado, Projeto } from '../api/projetos'
import { editarTarefa, type FiltroPrioridade, type Tarefa } from '../api/tarefas'
import { ESTADOS } from '../projetos/estados'
import { totalTarefas } from '../projetos/percentagem'
import { CartaoTarefa } from './CartaoTarefa'
import { DetalheTarefa } from './DetalheTarefa'
import { NovaTarefa } from './NovaTarefa'
import { FILTROS_PRIORIDADE } from './textos'
import { useQuadro } from './useQuadro'

interface Props {
  projeto: Projeto
  /** Depois de cada alteração, para atualizar as contagens e a percentagem do projeto. */
  aoAlterar: () => void
}

const MENSAGEM_VERSAO = 'A tarefa foi alterada por outra pessoa entretanto. O quadro foi atualizado; tente novamente.'

function lerEstado(valor: string | null): Estado | null {
  return ESTADOS.some((e) => e.codigo === valor) ? (valor as Estado) : null
}

function lerPrioridade(valor: string | null): FiltroPrioridade | null {
  return FILTROS_PRIORIDADE.some((p) => p.codigo === valor) ? (valor as FiltroPrioridade) : null
}

/**
 * Quadro Kanban do projeto (US07, wireframe.html): 4 colunas pela ordem dos estados, com até
 * 20 tarefas cada e "Ver mais"; filtros por estado e prioridade no endereço (US17 crit. 3);
 * mudar o estado arrastando um cartão para outra coluna (US06, nota 3). Num projeto arquivado
 * o quadro é só de leitura.
 */
export function QuadroKanban({ projeto, aoAlterar }: Props) {
  const [parametros, setParametros] = useSearchParams()
  const estadoFiltro = lerEstado(parametros.get('estado'))
  const prioridade = lerPrioridade(parametros.get('prioridade'))
  const { dados, recarregar, verMais, aCarregarMais, erroVerMais } = useQuadro(projeto.id, estadoFiltro, prioridade)

  const [aArrastar, setAArrastar] = useState<Tarefa | null>(null)
  const [alvo, setAlvo] = useState<Estado | null>(null)
  const [aMover, setAMover] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)
  const [aCriar, setACriar] = useState(false)
  const [aberta, setAberta] = useState<string | null>(null)
  const podeAlterar = !projeto.arquivado

  function filtrar(nome: 'estado' | 'prioridade', valor: string) {
    setParametros((atuais) => {
      const novos = new URLSearchParams(atuais)
      if (valor) {
        novos.set(nome, valor)
      } else {
        novos.delete(nome)
      }
      return novos
    }, { replace: true })
  }

  function limparFiltros() {
    setParametros(new URLSearchParams(), { replace: true })
  }

  /** Depois de qualquer alteração: o quadro e as contagens do projeto, sem recarregar a página. */
  function atualizar() {
    recarregar()
    aoAlterar()
  }

  async function mover(tarefa: Tarefa, estado: Estado) {
    setAMover(true)
    setAviso(null)
    try {
      await editarTarefa(tarefa.id, { versao: tarefa.versao, estado })
    } catch (e) {
      const erro = comoErroApi(e)
      setAviso(erro.codigo === 'VERSAO_DESATUALIZADA' ? MENSAGEM_VERSAO : erro.message)
    } finally {
      setAMover(false)
      atualizar()
    }
  }

  function passarPor(estado: Estado, evento: DragEvent<HTMLElement>) {
    if (aArrastar && aArrastar.estado !== estado) {
      evento.preventDefault()
      if (evento.dataTransfer) {
        evento.dataTransfer.dropEffect = 'move'
      }
      setAlvo(estado)
    }
  }

  function largar(estado: Estado, evento: DragEvent<HTMLElement>) {
    evento.preventDefault()
    const tarefa = aArrastar
    setAArrastar(null)
    setAlvo(null)
    if (tarefa && tarefa.estado !== estado) {
      void mover(tarefa, estado)
    }
  }

  function acabarArrastar() {
    setAArrastar(null)
    setAlvo(null)
  }

  const semTarefas = totalTarefas(projeto.contagens) === 0

  return (
    <section className="quadro-projeto" aria-label="Quadro de tarefas">
      <div className="barra-filtros">
        <strong>Filtrar</strong>
        <label className="filtro">
          Estado
          <select className="campo" value={estadoFiltro ?? ''} onChange={(e) => filtrar('estado', e.target.value)}>
            <option value="">Todos</option>
            {ESTADOS.map((e) => <option key={e.codigo} value={e.codigo}>{e.nome}</option>)}
          </select>
        </label>
        <label className="filtro">
          Prioridade
          <select className="campo" value={prioridade ?? ''} onChange={(e) => filtrar('prioridade', e.target.value)}>
            <option value="">Todas</option>
            {FILTROS_PRIORIDADE.map((p) => <option key={p.codigo} value={p.codigo}>{p.nome}</option>)}
          </select>
        </label>
        {(estadoFiltro || prioridade) && (
          <button type="button" className="btn" onClick={limparFiltros}>Limpar filtros</button>
        )}
        {podeAlterar && (
          <div className="direita">
            <button type="button" className="btn principal" onClick={() => setACriar(true)}>+ Nova tarefa</button>
          </div>
        )}
      </div>

      {aviso && <div className="aviso" role="alert">{aviso}</div>}
      {erroVerMais && <div className="aviso" role="alert">Não foi possível carregar mais tarefas. {erroVerMais.message}</div>}

      {dados.estado === 'a-carregar' && <p className="vazio">A carregar o quadro…</p>}
      {dados.estado === 'erro' && (
        <div className="vazio" role="alert">
          <p>Não foi possível carregar o quadro.</p>
          <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}
      {dados.estado === 'pronto' && semTarefas && (
        <p className="vazio">Este projeto ainda não tem tarefas.</p>
      )}
      {dados.estado === 'pronto' && !semTarefas && (
        <div className="quadro">
          {ESTADOS.filter((e) => dados.dados[e.codigo]).map(({ codigo, nome }) => {
            const coluna = dados.dados[codigo]!
            const restantes = coluna.total - coluna.itens.length
            return (
              <section
                key={codigo}
                className={alvo === codigo ? 'coluna alvo' : 'coluna'}
                aria-label={nome}
                onDragOver={(e) => passarPor(codigo, e)}
                onDragLeave={() => setAlvo((atual) => (atual === codigo ? null : atual))}
                onDrop={(e) => largar(codigo, e)}
              >
                <h2>
                  {nome} <span className="contagem">{coluna.total}</span>
                </h2>
                {alvo === codigo && <div className="largar">Largar aqui</div>}
                <ul className="cartoes-coluna">
                  {coluna.itens.map((tarefa) => (
                    <CartaoTarefa
                      key={tarefa.id}
                      tarefa={tarefa}
                      podeArrastar={podeAlterar && !aMover}
                      aArrastar={aArrastar?.id === tarefa.id}
                      aoComecarArrastar={setAArrastar}
                      aoAcabarArrastar={acabarArrastar}
                      aoAbrir={(t) => setAberta(t.id)}
                    />
                  ))}
                </ul>
                {restantes > 0 && (
                  <button type="button" className="ver-mais" disabled={aCarregarMais !== null}
                    onClick={() => void verMais(codigo)}>
                    {aCarregarMais === codigo ? 'A carregar…' : `Ver mais (${restantes})`}
                  </button>
                )}
              </section>
            )
          })}
        </div>
      )}

      {aCriar && (
        <NovaTarefa projetoId={projeto.id} aoFechar={() => setACriar(false)}
          aoCriar={() => { setACriar(false); atualizar() }} />
      )}
      {aberta && (
        <DetalheTarefa tarefaId={aberta} podeEliminar={projeto.souGestor} soLeitura={projeto.arquivado}
          aoFechar={() => setAberta(null)} aoAlterar={atualizar} />
      )}
    </section>
  )
}
