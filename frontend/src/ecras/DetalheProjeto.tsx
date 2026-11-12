import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { consultarProjeto } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { useNotificar } from '../comum/notificacao'
import { ConfirmarEliminacao } from '../projetos/ConfirmarEliminacao'
import { ContagensProjeto } from '../projetos/ContagensProjeto'
import { FormularioProjeto } from '../projetos/FormularioProjeto'
import { MembrosProjeto } from '../projetos/MembrosProjeto'
import { percentagemConclusao, textoTarefas, totalTarefas } from '../projetos/percentagem'
import { QuadroKanban } from '../tarefas/QuadroKanban'

function mensagemDeErro(estado: number): string {
  if (estado === 404) return 'Este projeto não existe.'
  if (estado === 403) return 'Não tem acesso a este projeto.'
  return 'Não foi possível carregar o projeto.'
}

/**
 * Detalhe de um projeto (US18 crit. 2) com o quadro Kanban das tarefas (US07, wireframe.html).
 * Depois de cada alteração no quadro, o projeto é recarregado para atualizar as contagens.
 */
export function DetalheProjeto() {
  const { projetoId = '' } = useParams()
  const navegar = useNavigate()
  const [dados, recarregar] = useDados(() => consultarProjeto(projetoId), projetoId)
  const [dialogo, setDialogo] = useState<'editar' | 'eliminar' | null>(null)
  // Aumenta quando outra parte do ecrã muda as tarefas (ex.: remover um membro tira-o dos responsáveis)
  const [sinalQuadro, setSinalQuadro] = useState(0)
  const notificar = useNotificar()

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
      <QuadroKanban projeto={projeto} aoAlterar={recarregar} sinal={sinalQuadro} />
      <MembrosProjeto projeto={projeto} aoAlterarTarefas={() => setSinalQuadro((s) => s + 1)} />

      {dialogo === 'editar' && (
        <FormularioProjeto projeto={projeto} aoFechar={() => setDialogo(null)}
          aoGuardar={() => { setDialogo(null); recarregar(); notificar('Alterações guardadas.') }} />
      )}
      {dialogo === 'eliminar' && (
        <ConfirmarEliminacao projeto={projeto} aoFechar={() => setDialogo(null)}
          aoEliminar={() => { notificar('Projeto eliminado.'); navegar('/projetos', { replace: true }) }} />
      )}
    </section>
  )
}
