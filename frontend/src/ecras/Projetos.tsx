import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { listarProjetos, TAMANHO_PAGINA, type Projeto } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { useNotificar } from '../comum/notificacao'
import { ConfirmarEliminacao } from '../projetos/ConfirmarEliminacao'
import { FormularioProjeto } from '../projetos/FormularioProjeto'
import { percentagemConclusao, totalTarefas } from '../projetos/percentagem'

const ESPERA_PESQUISA_MS = 300

type Dialogo = { tipo: 'criar' } | { tipo: 'editar'; projeto: Projeto } | { tipo: 'eliminar'; projeto: Projeto }

function paginaDoUrl(valor: string | null): number {
  const pagina = Number(valor)
  return Number.isInteger(pagina) && pagina >= 1 ? pagina : 1
}

function textoTotal(total: number): string {
  if (total === 0) return 'Nenhum projeto encontrado.'
  return total === 1 ? '1 projeto encontrado' : `${total} projetos encontrados`
}

/**
 * Ecrã 3 · Projetos (US16, US17): pesquisa pelo nome e paginação, guardadas no URL
 * para que recarregar a página as mantenha.
 */
export function Projetos() {
  const [parametros, setParametros] = useSearchParams()
  const q = parametros.get('q') ?? ''
  const pagina = paginaDoUrl(parametros.get('pagina'))
  const [texto, setTexto] = useState(q)
  const [dialogo, setDialogo] = useState<Dialogo | null>(null)
  const notificar = useNotificar()
  const [dados, recarregar] = useDados(() => listarProjetos({ q: q.trim(), pagina }), `${q}|${pagina}`)

  // Pesquisa enquanto se escreve, 300 ms depois da última tecla; volta à página 1
  useEffect(() => {
    if (texto === q) {
      return
    }
    const espera = setTimeout(() => {
      setParametros(texto.trim() === '' ? {} : { q: texto }, { replace: true })
    }, ESPERA_PESQUISA_MS)
    return () => clearTimeout(espera)
  }, [texto, q, setParametros])

  function irParaPagina(nova: number) {
    const seguintes: Record<string, string> = {}
    if (q) seguintes.q = q
    if (nova > 1) seguintes.pagina = String(nova)
    setParametros(seguintes)
  }

  function depoisDeEliminar() {
    setDialogo(null)
    notificar('Projeto eliminado.')
    // Se a página ficou vazia, volta à anterior
    if (dados.estado === 'pronto' && dados.dados.itens.length === 1 && pagina > 1) {
      irParaPagina(pagina - 1)
    } else {
      recarregar()
    }
  }

  const totalPaginas = dados.estado === 'pronto'
    ? Math.max(1, Math.ceil(dados.dados.total / TAMANHO_PAGINA))
    : 1

  return (
    <section>
      <h1>Projetos</h1>
      <div className="ferramentas">
        <label htmlFor="pesquisa" className="so-leitores">Pesquisar pelo nome</label>
        <input
          id="pesquisa"
          type="search"
          className="campo pesquisa"
          placeholder="Pesquisar pelo nome"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
        />
        <button type="button" className="btn principal direita" onClick={() => setDialogo({ tipo: 'criar' })}>
          + Novo projeto
        </button>
      </div>

      {dados.estado === 'a-carregar' && <p className="vazio">A carregar os projetos…</p>}

      {dados.estado === 'erro' && (
        <div className="vazio" role="alert">
          <p>Não foi possível carregar os projetos.</p>
          <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}

      {dados.estado === 'pronto' && (
        <>
          {dados.dados.itens.length > 0 && (
            <div className="tabela-rolagem">
            <table className="tabela caixa">
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">Gestor</th>
                  <th scope="col">Tarefas</th>
                  <th scope="col">Concluído</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                {dados.dados.itens.map((projeto) => (
                  <tr key={projeto.id}>
                    <td><Link to={`/projetos/${projeto.id}`}>{projeto.nome}</Link></td>
                    <td>{projeto.gestor.nome}</td>
                    <td>{totalTarefas(projeto.contagens)}</td>
                    <td>{percentagemConclusao(projeto.contagens)}%</td>
                    <td className="acoes">
                      {projeto.souGestor ? (
                        <>
                          <button type="button" className="btn gestor" onClick={() => setDialogo({ tipo: 'editar', projeto })}
                            aria-label={`Editar ${projeto.nome}`}>Editar</button>
                          <button type="button" className="btn gestor" onClick={() => setDialogo({ tipo: 'eliminar', projeto })}
                            aria-label={`Eliminar ${projeto.nome}`}>Eliminar</button>
                        </>
                      ) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          )}

          <div className="paginacao">
            <span>{textoTotal(dados.dados.total)}</span>
            {totalPaginas > 1 && (
              <nav className="paginas" aria-label="Páginas">
                <button type="button" className="btn" onClick={() => irParaPagina(pagina - 1)}
                  disabled={pagina <= 1} aria-label="Página anterior">‹</button>
                <span aria-current="page">Página {pagina} de {totalPaginas}</span>
                <button type="button" className="btn" onClick={() => irParaPagina(pagina + 1)}
                  disabled={pagina >= totalPaginas} aria-label="Página seguinte">›</button>
              </nav>
            )}
          </div>
        </>
      )}

      {dialogo?.tipo === 'criar' && (
        <FormularioProjeto aoFechar={() => setDialogo(null)}
          aoGuardar={() => { setDialogo(null); recarregar(); notificar('Projeto criado.') }} />
      )}
      {dialogo?.tipo === 'editar' && (
        <FormularioProjeto projeto={dialogo.projeto} aoFechar={() => setDialogo(null)}
          aoGuardar={() => { setDialogo(null); recarregar(); notificar('Alterações guardadas.') }} />
      )}
      {dialogo?.tipo === 'eliminar' && (
        <ConfirmarEliminacao projeto={dialogo.projeto} aoFechar={() => setDialogo(null)} aoEliminar={depoisDeEliminar} />
      )}
    </section>
  )
}
