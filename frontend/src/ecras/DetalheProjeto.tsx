import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { consultarProjeto } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { ConfirmarEliminacao } from '../projetos/ConfirmarEliminacao'
import { ContagensProjeto } from '../projetos/ContagensProjeto'
import { FormularioProjeto } from '../projetos/FormularioProjeto'
import { MembrosProjeto } from '../projetos/MembrosProjeto'
import { percentagemConclusao, textoTarefas, totalTarefas } from '../projetos/percentagem'

function mensagemDeErro(estado: number): string {
  if (estado === 404) return 'Este projeto não existe.'
  if (estado === 403) return 'Não tem acesso a este projeto.'
  return 'Não foi possível carregar o projeto.'
}

/**
 * Detalhe de um projeto (US18 crit. 2). Provisório no I2: o quadro Kanban
 * das tarefas (wireframe.html) é acrescentado no I4.
 */
export function DetalheProjeto() {
  const { projetoId = '' } = useParams()
  const navegar = useNavigate()
  const [dados, recarregar] = useDados(() => consultarProjeto(projetoId), projetoId)
  const [dialogo, setDialogo] = useState<'editar' | 'eliminar' | null>(null)

  if (dados.estado === 'a-carregar') {
    return <p className="vazio">A carregar o projeto…</p>
  }
  if (dados.estado === 'erro') {
    const podeRepetir = dados.erro.estado !== 403 && dados.erro.estado !== 404
    return (
      <div className="vazio" role="alert">
        <p>{mensagemDeErro(dados.erro.estado)}</p>
        {podeRepetir && <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>}
      </div>
    )
  }

  const projeto = dados.dados
  const total = totalTarefas(projeto.contagens)

  return (
    <section>
      <div className="barra-titulo">
        <h1>
          {projeto.nome}
          {projeto.arquivado && <span className="etiqueta-gestor">Arquivado</span>}
        </h1>
        {projeto.souGestor && (
          <div className="direita">
            {!projeto.arquivado && (
              <button type="button" className="btn gestor" onClick={() => setDialogo('editar')}>Editar</button>
            )}
            <button type="button" className="btn gestor" onClick={() => setDialogo('eliminar')}>Eliminar</button>
          </div>
        )}
      </div>

      <p>{projeto.descricao ?? <span className="suave">Sem descrição.</span>}</p>
      <p>Gestor: {projeto.gestor.nome}</p>
      <p><small>{percentagemConclusao(projeto.contagens)}% concluído · {textoTarefas(total)}</small></p>
      <ContagensProjeto contagens={projeto.contagens} />
      <MembrosProjeto projeto={projeto} />
      <p className="vazio">As tarefas do projeto vão aparecer aqui.</p>

      {dialogo === 'editar' && (
        <FormularioProjeto projeto={projeto} aoFechar={() => setDialogo(null)}
          aoGuardar={() => { setDialogo(null); recarregar() }} />
      )}
      {dialogo === 'eliminar' && (
        <ConfirmarEliminacao projeto={projeto} aoFechar={() => setDialogo(null)}
          aoEliminar={() => navegar('/projetos', { replace: true })} />
      )}
    </section>
  )
}
