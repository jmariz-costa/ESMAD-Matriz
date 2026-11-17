import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { TAMANHO_PAGINA } from '../api/projetos'
import { listarMinhasTarefas, type MinhaTarefa } from '../api/tarefas'
import { useDados } from '../comum/useDados'
import { ESTADOS } from '../projetos/estados'
import { DetalheTarefa } from '../tarefas/DetalheTarefa'
import { formatarDataHora, formatarPrazo, nomePrioridade } from '../tarefas/textos'

type Vista = 'por-concluir' | 'concluidas'

function paginaDoUrl(valor: string | null): number {
  const pagina = Number(valor)
  return Number.isInteger(pagina) && pagina >= 1 ? pagina : 1
}

function nomeEstado(tarefa: MinhaTarefa): string {
  return ESTADOS.find((e) => e.codigo === tarefa.estado)?.nome ?? tarefa.estado
}

function textoTotal(total: number, vista: Vista): string {
  if (vista === 'concluidas') {
    return total === 1 ? '1 tarefa concluída' : `${total} tarefas concluídas`
  }
  return total === 1 ? '1 tarefa por concluir' : `${total} tarefas por concluir`
}

/**
 * "As minhas tarefas" (US11): as tarefas atribuídas a mim em todos os projetos não arquivados,
 * em dois separadores, por concluir (por prazo) e concluídas (crit. 5). O separador e a página
 * ficam no URL. Clicar no título abre o detalhe da tarefa, como no quadro.
 */
export function MinhasTarefas() {
  const [parametros, setParametros] = useSearchParams()
  const vista: Vista = parametros.get('vista') === 'concluidas' ? 'concluidas' : 'por-concluir'
  const pagina = paginaDoUrl(parametros.get('pagina'))
  const concluidas = vista === 'concluidas'
  const [dados, recarregar] = useDados(() => listarMinhasTarefas({ concluidas, pagina }), `${vista}|${pagina}`)
  const [aberta, setAberta] = useState<string | null>(null)

  function irPara(novaVista: Vista, novaPagina = 1) {
    const seguintes: Record<string, string> = {}
    if (novaVista === 'concluidas') seguintes.vista = 'concluidas'
    if (novaPagina > 1) seguintes.pagina = String(novaPagina)
    setParametros(seguintes)
  }

  const totalPaginas = dados.estado === 'pronto'
    ? Math.max(1, Math.ceil(dados.dados.total / TAMANHO_PAGINA))
    : 1

  return (
    <section>
      <h1>As minhas tarefas</h1>

      <div className="separadores" role="tablist" aria-label="Tarefas">
        <button type="button" role="tab" id="sep-por-concluir" aria-controls="lista-tarefas"
          aria-selected={!concluidas} className={concluidas ? 'separador' : 'separador ativo'}
          onClick={() => irPara('por-concluir')}>
          Por concluir
        </button>
        <button type="button" role="tab" id="sep-concluidas" aria-controls="lista-tarefas"
          aria-selected={concluidas} className={concluidas ? 'separador ativo' : 'separador'}
          onClick={() => irPara('concluidas')}>
          Concluídas
        </button>
      </div>

      <div role="tabpanel" id="lista-tarefas" aria-labelledby={concluidas ? 'sep-concluidas' : 'sep-por-concluir'}>
        {dados.estado === 'a-carregar' && <p className="vazio">A carregar as tarefas…</p>}

        {dados.estado === 'erro' && (
          <div className="vazio" role="alert">
            <p>Não foi possível carregar as tarefas.</p>
            <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
          </div>
        )}

        {dados.estado === 'pronto' && dados.dados.total === 0 && (
          <p className="vazio">
            {concluidas ? 'Ainda não tem tarefas concluídas.' : 'Não tem tarefas por concluir.'}
          </p>
        )}

        {dados.estado === 'pronto' && dados.dados.total > 0 && (
          <>
            <div className="tabela-rolagem">
              <table className="tabela caixa minhas-tarefas">
                <thead>
                  <tr>
                    <th scope="col">Tarefa</th>
                    <th scope="col">Projeto</th>
                    {!concluidas && <th scope="col">Estado</th>}
                    <th scope="col">Prioridade</th>
                    <th scope="col">Prazo</th>
                    {concluidas && <th scope="col">Concluída em</th>}
                  </tr>
                </thead>
                <tbody>
                  {dados.dados.itens.map((tarefa) => (
                    <tr key={tarefa.id}>
                      <td>
                        <button type="button" className="titulo-cartao" onClick={() => setAberta(tarefa.id)}>
                          {tarefa.titulo}
                        </button>
                      </td>
                      <td><Link to={`/projetos/${tarefa.projeto.id}`}>{tarefa.projeto.nome}</Link></td>
                      {!concluidas && (
                        <td><span className="estado-tarefa" data-estado={tarefa.estado}>{nomeEstado(tarefa)}</span></td>
                      )}
                      <td>
                        {tarefa.prioridade
                          ? <span className="etiqueta" data-prioridade={tarefa.prioridade}>{nomePrioridade(tarefa.prioridade)}</span>
                          : <span className="suave">—</span>}
                      </td>
                      <td className="numeros">{tarefa.prazo ? formatarPrazo(tarefa.prazo) : <span className="suave">Sem prazo</span>}</td>
                      {concluidas && (
                        <td className="numeros">
                          {tarefa.concluidaEm ? formatarDataHora(tarefa.concluidaEm) : <span className="suave">Sem data</span>}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="paginacao">
              <span>{textoTotal(dados.dados.total, vista)}</span>
              {totalPaginas > 1 && (
                <nav className="paginas" aria-label="Páginas">
                  <button type="button" className="btn" onClick={() => irPara(vista, pagina - 1)}
                    disabled={pagina <= 1} aria-label="Página anterior">‹</button>
                  <span aria-current="page">Página {pagina} de {totalPaginas}</span>
                  <button type="button" className="btn" onClick={() => irPara(vista, pagina + 1)}
                    disabled={pagina >= totalPaginas} aria-label="Página seguinte">›</button>
                </nav>
              )}
            </div>
          </>
        )}
      </div>

      {aberta && (
        <DetalheTarefa tarefaId={aberta} podeEliminar={false} soLeitura={false}
          aoFechar={() => setAberta(null)} aoAlterar={recarregar} />
      )}
    </section>
  )
}
