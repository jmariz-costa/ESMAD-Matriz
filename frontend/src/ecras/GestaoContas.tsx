import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { comoErroApi } from '../api/cliente'
import { listarContas, reativarConta, type Conta, type EstadoConta } from '../api/contas'
import { TAMANHO_PAGINA } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { useNotificar } from '../comum/notificacao'
import { CriarConta } from '../contas/CriarConta'
import { DesativarConta } from '../contas/DesativarConta'
import { EditarConta } from '../contas/EditarConta'
import { ReporAcesso } from '../contas/ReporAcesso'

const ESPERA_PESQUISA_MS = 300

function paginaDoUrl(valor: string | null): number {
  const pagina = Number(valor)
  return Number.isInteger(pagina) && pagina >= 1 ? pagina : 1
}

function estadoDoUrl(valor: string | null): EstadoConta | undefined {
  return valor === 'ATIVO' || valor === 'DESATIVADO' ? valor : undefined
}

function textoTotal(total: number): string {
  if (total === 0) return 'Nenhuma conta encontrada.'
  return total === 1 ? '1 conta' : `${total} contas`
}

/**
 * Gestão de contas do Administrador (US14, US19, US21): contas de Membro com pesquisa, filtro por
 * estado e paginação, guardados no URL. Criar uma conta, editar o nome e repor o acesso abrem um
 * diálogo; desativar abre a confirmação (e a escolha dos novos Gestores); reativar é imediato.
 */
export function GestaoContas() {
  const [parametros, setParametros] = useSearchParams()
  const q = parametros.get('q') ?? ''
  const estado = estadoDoUrl(parametros.get('estado'))
  const pagina = paginaDoUrl(parametros.get('pagina'))
  const [texto, setTexto] = useState(q)
  const [aCriar, setACriar] = useState(false)
  const [aDesativar, setADesativar] = useState<Conta | null>(null)
  const [aEditar, setAEditar] = useState<Conta | null>(null)
  const [aReporAcesso, setAReporAcesso] = useState<Conta | null>(null)
  const [aReativar, setAReativar] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const notificar = useNotificar()
  const [dados, recarregar] = useDados(
    () => listarContas({ q: q.trim(), estado, pagina }), `${q}|${estado ?? ''}|${pagina}`)

  function irPara(alteracoes: { estado?: EstadoConta; pagina?: number }) {
    const seguinte = { q, estado, pagina: 1, ...alteracoes }
    const novos: Record<string, string> = {}
    if (seguinte.q.trim()) novos.q = seguinte.q
    if (seguinte.estado) novos.estado = seguinte.estado
    if (seguinte.pagina > 1) novos.pagina = String(seguinte.pagina)
    setParametros(novos)
  }

  // Pesquisa enquanto se escreve, 300 ms depois da última tecla; volta à página 1
  useEffect(() => {
    if (texto === q) {
      return
    }
    const espera = setTimeout(() => {
      const novos: Record<string, string> = {}
      if (texto.trim()) novos.q = texto
      if (estado) novos.estado = estado
      setParametros(novos, { replace: true })
    }, ESPERA_PESQUISA_MS)
    return () => clearTimeout(espera)
  }, [texto, q, estado, setParametros])

  async function reativar(conta: Conta) {
    setErro(null)
    setAReativar(conta.id)
    try {
      await reativarConta(conta.id)
      notificar(`Conta de ${conta.nome} reativada.`)
      recarregar()
    } catch (e) {
      setErro(comoErroApi(e).message)
    } finally {
      setAReativar(null)
    }
  }

  const totalPaginas = dados.estado === 'pronto'
    ? Math.max(1, Math.ceil(dados.dados.total / TAMANHO_PAGINA))
    : 1

  return (
    <section>
      <h1>Gestão de contas</h1>
      <div className="ferramentas">
        <label htmlFor="pesquisa-contas" className="so-leitores">Pesquisar pelo nome ou email</label>
        <input
          id="pesquisa-contas"
          type="search"
          className="campo pesquisa"
          placeholder="Pesquisar pelo nome ou email"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
        />
        <label className="filtro-estado" htmlFor="filtro-estado">
          Estado
          <select id="filtro-estado" className="campo" value={estado ?? ''}
            onChange={(e) => irPara({ estado: estadoDoUrl(e.target.value) })}>
            <option value="">Todas</option>
            <option value="ATIVO">Ativas</option>
            <option value="DESATIVADO">Desativadas</option>
          </select>
        </label>
        <button type="button" className="btn principal direita" onClick={() => setACriar(true)}>
          + Criar conta
        </button>
      </div>

      {erro && <div className="aviso" role="alert">{erro}</div>}

      {dados.estado === 'a-carregar' && <p className="vazio">A carregar as contas…</p>}

      {dados.estado === 'erro' && (
        <div className="vazio" role="alert">
          <p>Não foi possível carregar as contas.</p>
          <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}

      {dados.estado === 'pronto' && (
        <>
          {dados.dados.itens.length > 0 && (
            <div className="tabela-rolagem">
              <table className="tabela caixa contas">
                <thead>
                  <tr>
                    <th scope="col">Nome</th>
                    <th scope="col">Email</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {dados.dados.itens.map((conta) => (
                    <tr key={conta.id} className={conta.estado === 'DESATIVADO' ? 'desativada' : undefined}>
                      <td>{conta.nome}</td>
                      <td>{conta.email}</td>
                      <td>
                        <span className="estado-conta" data-estado={conta.estado}>
                          {conta.estado === 'ATIVO' ? 'Ativa' : 'Desativada'}
                        </span>
                      </td>
                      <td className="acoes">
                        <button type="button" className="btn" onClick={() => setAEditar(conta)}
                          aria-label={`Editar ${conta.nome}`}>Editar</button>
                        {conta.estado === 'ATIVO' && (
                          <button type="button" className="btn" onClick={() => setAReporAcesso(conta)}
                            aria-label={`Repor o acesso de ${conta.nome}`}>Repor acesso</button>
                        )}
                        {conta.estado === 'ATIVO' ? (
                          <button type="button" className="btn perigo-contorno" onClick={() => setADesativar(conta)}
                            aria-label={`Desativar ${conta.nome}`}>Desativar</button>
                        ) : (
                          <button type="button" className="btn" onClick={() => void reativar(conta)}
                            disabled={aReativar === conta.id} aria-label={`Reativar ${conta.nome}`}>
                            {aReativar === conta.id ? 'A reativar…' : 'Reativar'}
                          </button>
                        )}
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
                <button type="button" className="btn" onClick={() => irPara({ pagina: pagina - 1 })}
                  disabled={pagina <= 1} aria-label="Página anterior">‹</button>
                <span aria-current="page">Página {pagina} de {totalPaginas}</span>
                <button type="button" className="btn" onClick={() => irPara({ pagina: pagina + 1 })}
                  disabled={pagina >= totalPaginas} aria-label="Página seguinte">›</button>
              </nav>
            )}
          </div>
        </>
      )}

      {aCriar && (
        <CriarConta
          aoFechar={() => setACriar(false)}
          aoCriar={(conta) => {
            notificar(`Conta de ${conta.nome} criada. Comunique-lhe a palavra-passe temporária.`)
            setACriar(false)
            recarregar()
          }}
        />
      )}
      {aEditar && (
        <EditarConta
          conta={aEditar}
          aoFechar={() => setAEditar(null)}
          aoGuardar={(conta) => {
            notificar(`Nome alterado para ${conta.nome}.`)
            setAEditar(null)
            recarregar()
          }}
        />
      )}
      {aReporAcesso && (
        <ReporAcesso
          conta={aReporAcesso}
          aoFechar={() => setAReporAcesso(null)}
          aoRepor={() => {
            notificar(`Acesso de ${aReporAcesso.nome} reposto. Comunique-lhe a palavra-passe temporária.`)
            setAReporAcesso(null)
          }}
        />
      )}
      {aDesativar && (
        <DesativarConta
          conta={aDesativar}
          aoFechar={() => setADesativar(null)}
          aoDesativar={() => {
            notificar(`Conta de ${aDesativar.nome} desativada.`)
            setADesativar(null)
            recarregar()
          }}
        />
      )}
    </section>
  )
}
