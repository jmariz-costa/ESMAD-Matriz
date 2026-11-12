import { ESTADOS } from '../projetos/estados'
import type { CampoTarefa, ErrosTarefa, ValoresTarefa } from './formulario'
import { PRIORIDADES } from './textos'

interface Props {
  valores: ValoresTarefa
  erros: ErrosTarefa
  aoMudar: (campo: CampoTarefa, valor: string) => void
  /** O estado só aparece ao editar: uma tarefa nova começa sempre em "A fazer" (PR05). */
  comEstado?: boolean
  /** Projeto arquivado: só leitura (RN15). */
  soLeitura?: boolean
}

/** Campos de uma tarefa (US05 crit. 2): só o título é obrigatório. */
export function CamposTarefa({ valores, erros, aoMudar, comEstado = false, soLeitura = false }: Props) {
  function propsErro(campo: CampoTarefa) {
    return {
      'aria-invalid': erros[campo] ? true : undefined,
      'aria-describedby': erros[campo] ? `tarefa-${campo}-erro` : undefined,
    }
  }

  function mensagem(campo: CampoTarefa) {
    return erros[campo] && <div id={`tarefa-${campo}-erro`} className="msg-erro">{erros[campo]}</div>
  }

  const classe = (campo: CampoTarefa) => (erros[campo] ? 'campo erro' : 'campo')

  return (
    <>
      <label htmlFor="tarefa-titulo">Título *</label>
      <input id="tarefa-titulo" className={classe('titulo')} value={valores.titulo} disabled={soLeitura}
        onChange={(e) => aoMudar('titulo', e.target.value)} {...propsErro('titulo')} />
      {mensagem('titulo')}

      <label htmlFor="tarefa-descricao">Descrição</label>
      <textarea id="tarefa-descricao" className={classe('descricao')} rows={4} value={valores.descricao}
        disabled={soLeitura} onChange={(e) => aoMudar('descricao', e.target.value)} {...propsErro('descricao')} />
      {mensagem('descricao')}

      <div className="campos-lado">
        {comEstado && (
          <div>
            <label htmlFor="tarefa-estado">Estado</label>
            <select id="tarefa-estado" className={classe('estado')} value={valores.estado} disabled={soLeitura}
              onChange={(e) => aoMudar('estado', e.target.value)} {...propsErro('estado')}>
              {ESTADOS.map((e) => <option key={e.codigo} value={e.codigo}>{e.nome}</option>)}
            </select>
            {mensagem('estado')}
          </div>
        )}
        <div>
          <label htmlFor="tarefa-prioridade">Prioridade</label>
          <select id="tarefa-prioridade" className={classe('prioridade')} value={valores.prioridade}
            disabled={soLeitura} onChange={(e) => aoMudar('prioridade', e.target.value)} {...propsErro('prioridade')}>
            <option value="">Sem prioridade</option>
            {PRIORIDADES.map((p) => <option key={p.codigo} value={p.codigo}>{p.nome}</option>)}
          </select>
          {mensagem('prioridade')}
        </div>
        <div>
          <label htmlFor="tarefa-prazo">Prazo</label>
          <input id="tarefa-prazo" type="date" className={classe('prazo')} value={valores.prazo} disabled={soLeitura}
            onChange={(e) => aoMudar('prazo', e.target.value)} {...propsErro('prazo')} />
          {mensagem('prazo')}
        </div>
      </div>
    </>
  )
}
