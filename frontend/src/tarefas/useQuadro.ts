import { useCallback, useEffect, useRef, useState } from 'react'
import { comoErroApi, type ErroApi } from '../api/cliente'
import type { Estado } from '../api/projetos'
import { listarTarefas, TAMANHO_COLUNA, type FiltroPrioridade, type Tarefa } from '../api/tarefas'
import type { Dados } from '../comum/useDados'
import { ESTADOS } from '../projetos/estados'

export interface Coluna {
  itens: Tarefa[]
  /** Total da coluna com os filtros, e não só das tarefas carregadas (US17 crit. 4). */
  total: number
  /** Páginas já carregadas ("Ver mais"). */
  paginas: number
}

export type Colunas = Partial<Record<Estado, Coluna>>

/** As primeiras {@code paginas} páginas de uma coluna, juntas e sem tarefas repetidas. */
async function carregarColuna(projetoId: string, estado: Estado, prioridade: FiltroPrioridade | null,
                              paginas: number): Promise<Coluna> {
  const respostas = await Promise.all(Array.from({ length: paginas }, (_, i) =>
    listarTarefas(projetoId, { estado, prioridade: prioridade ?? undefined, pagina: i + 1, tamanho: TAMANHO_COLUNA })))
  return {
    itens: semRepetidas(respostas.flatMap((r) => r.itens)),
    total: respostas[respostas.length - 1].total,
    paginas,
  }
}

function semRepetidas(tarefas: Tarefa[]): Tarefa[] {
  const vistas = new Set<string>()
  return tarefas.filter((t) => !vistas.has(t.id) && vistas.add(t.id))
}

/**
 * Tarefas do quadro: um pedido por coluna, com o estado e a prioridade filtrados no servidor
 * (openapi.yaml, listarTarefas). Filtrar por estado mostra só essa coluna (wireframe, nota 7).
 *
 * Ao recarregar (depois de cada operação), cada coluna volta a pedir as páginas que já tinha,
 * e o quadro anterior continua visível até chegarem os dados novos. Mudar os filtros volta à
 * primeira página de cada coluna.
 */
export function useQuadro(projetoId: string, estadoFiltro: Estado | null, prioridade: FiltroPrioridade | null,
                          sinal = 0) {
  const filtro = `${projetoId}|${estadoFiltro ?? ''}|${prioridade ?? ''}`
  const [versao, setVersao] = useState(0)
  const [resultado, setResultado] = useState<{ pedido: string; dados: Dados<Colunas> } | null>(null)
  const [aCarregarMais, setACarregarMais] = useState<Estado | null>(null)
  const [erroVerMais, setErroVerMais] = useState<ErroApi | null>(null)
  const paginas = useRef<{ filtro: string; porEstado: Partial<Record<Estado, number>> }>({ filtro, porEstado: {} })
  const pedido = `${filtro}#${versao}#${sinal}`

  useEffect(() => {
    let ativo = true
    if (paginas.current.filtro !== filtro) {
      paginas.current = { filtro, porEstado: {} }
    }
    const estados = estadoFiltro ? [estadoFiltro] : ESTADOS.map((e) => e.codigo)
    const porEstado = paginas.current.porEstado
    Promise.all(estados.map(async (estado) =>
      [estado, await carregarColuna(projetoId, estado, prioridade, porEstado[estado] ?? 1)] as const))
      .then((colunas) => ativo && setResultado({ pedido, dados: { estado: 'pronto', dados: Object.fromEntries(colunas) } }))
      .catch((erro: unknown) => ativo && setResultado({ pedido, dados: { estado: 'erro', erro: comoErroApi(erro) } }))
    return () => {
      ativo = false
    }
  }, [projetoId, estadoFiltro, prioridade, filtro, pedido])

  const recarregar = useCallback(() => {
    setErroVerMais(null)
    setVersao((v) => v + 1)
  }, [])

  const atual = resultado?.dados.estado === 'pronto' ? resultado.dados.dados : null

  async function verMais(estado: Estado) {
    const coluna = atual?.[estado]
    if (!coluna || aCarregarMais) {
      return
    }
    setACarregarMais(estado)
    setErroVerMais(null)
    try {
      const seguinte = await listarTarefas(projetoId, {
        estado, prioridade: prioridade ?? undefined, pagina: coluna.paginas + 1, tamanho: TAMANHO_COLUNA,
      })
      if (paginas.current.filtro === filtro) {
        paginas.current.porEstado[estado] = coluna.paginas + 1
        setResultado((anterior) => {
          const colunas = anterior?.dados.estado === 'pronto' ? anterior.dados.dados : null
          const antiga = colunas?.[estado]
          if (!anterior || !colunas || !antiga) {
            return anterior
          }
          const nova = { itens: semRepetidas([...antiga.itens, ...seguinte.itens]), total: seguinte.total, paginas: antiga.paginas + 1 }
          return { ...anterior, dados: { estado: 'pronto', dados: { ...colunas, [estado]: nova } } }
        })
      }
    } catch (erro) {
      setErroVerMais(comoErroApi(erro))
    } finally {
      setACarregarMais(null)
    }
  }

  let dados: Dados<Colunas> = { estado: 'a-carregar' }
  if (resultado?.pedido === pedido || resultado?.dados.estado === 'pronto') {
    dados = resultado.dados
  }
  return { dados, recarregar, verMais, aCarregarMais, erroVerMais }
}
