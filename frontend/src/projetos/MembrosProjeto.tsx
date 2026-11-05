import { useMemo, useState } from 'react'
import { listarMembros, type Membro } from '../api/membros'
import type { Projeto } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { AdicionarMembro } from './AdicionarMembro'
import { ConfirmarRemocaoMembro } from './ConfirmarRemocaoMembro'

/**
 * Membros do projeto (US04). Todos os membros veem a lista; só o Gestor adiciona e
 * remove, e nunca num projeto arquivado (RN07, RI10). O servidor verifica sempre (S03).
 */
export function MembrosProjeto({ projeto }: { projeto: Projeto }) {
  const [dados, recarregar] = useDados(() => listarMembros(projeto.id), projeto.id)
  const [aAdicionar, setAAdicionar] = useState(false)
  const [aRemover, setARemover] = useState<Membro | null>(null)
  const podeGerir = projeto.souGestor && !projeto.arquivado

  const ids = useMemo(
    () => new Set(dados.estado === 'pronto' ? dados.dados.map((m) => m.id) : []),
    [dados],
  )

  return (
    <section className="membros" aria-labelledby="titulo-membros">
      <div className="barra-titulo">
        <h2 id="titulo-membros">
          Membros{dados.estado === 'pronto' && ` (${dados.dados.length})`}
        </h2>
        {podeGerir && (
          <button type="button" className="btn gestor direita" onClick={() => setAAdicionar(true)}>
            + Adicionar membro
          </button>
        )}
      </div>

      {dados.estado === 'a-carregar' && <p className="suave">A carregar os membros…</p>}
      {dados.estado === 'erro' && (
        <div role="alert">
          <p>Não foi possível carregar os membros.</p>
          <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}
      {dados.estado === 'pronto' && (
        <ul className="lista-pessoas">
          {dados.dados.map((membro) => (
            <li key={membro.id}>
              <span>
                {membro.nome}
                <small className="suave"> {membro.email}</small>
                {membro.gestor && <span className="etiqueta-gestor">Gestor</span>}
                {!membro.ativo && <span className="etiqueta-gestor">Conta desativada</span>}
              </span>
              {podeGerir && !membro.gestor && (
                <button type="button" className="btn gestor" onClick={() => setARemover(membro)}
                  aria-label={`Remover ${membro.nome}`}>Remover</button>
              )}
            </li>
          ))}
        </ul>
      )}

      {aAdicionar && (
        <AdicionarMembro projetoId={projeto.id} membros={ids}
          aoFechar={() => setAAdicionar(false)} aoAdicionar={recarregar} />
      )}
      {aRemover && (
        <ConfirmarRemocaoMembro projetoId={projeto.id} membro={aRemover}
          aoFechar={() => setARemover(null)}
          aoRemover={() => {
            setARemover(null)
            recarregar()
          }} />
      )}
    </section>
  )
}
