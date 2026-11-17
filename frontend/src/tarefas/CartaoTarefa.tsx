import type { DragEvent } from 'react'
import type { Tarefa } from '../api/tarefas'
import { formatarPrazo, nomePrioridade } from './textos'

interface Props {
  tarefa: Tarefa
  /** Falso num projeto arquivado ou enquanto outra mudança de estado está em curso. */
  podeArrastar: boolean
  aArrastar: boolean
  aoComecarArrastar: (tarefa: Tarefa) => void
  aoAcabarArrastar: () => void
  /** Abre o detalhe; o título é um botão, para abrir também pelo teclado. */
  aoAbrir: (tarefa: Tarefa) => void
}

/**
 * Cartão do quadro: título, responsáveis (ou "Sem responsável") e, quando existam,
 * a prioridade e o prazo (US07 crit. 3, PR06).
 */
export function CartaoTarefa({ tarefa, podeArrastar, aArrastar, aoComecarArrastar, aoAcabarArrastar, aoAbrir }: Props) {
  function comecar(evento: DragEvent<HTMLLIElement>) {
    // O Firefox só começa a arrastar se houver dados no dataTransfer
    evento.dataTransfer?.setData('text/plain', tarefa.id)
    if (evento.dataTransfer) {
      evento.dataTransfer.effectAllowed = 'move'
    }
    aoComecarArrastar(tarefa)
  }

  return (
    <li
      className={aArrastar ? 'cartao caixa a-arrastar' : 'cartao caixa'}
      draggable={podeArrastar}
      onDragStart={podeArrastar ? comecar : undefined}
      onDragEnd={aoAcabarArrastar}
      onClick={() => aoAbrir(tarefa)}
    >
      {podeArrastar && <span className="pega" aria-hidden="true">⠿</span>}
      <button type="button" className="titulo-cartao">{tarefa.titulo}</button>
      <div className={tarefa.responsaveis.length === 0 ? 'responsaveis sem-resp' : 'responsaveis'}>
        {tarefa.responsaveis.length === 0
          ? 'Sem responsável'
          : tarefa.responsaveis.map((r) => r.nome).join(', ')}
      </div>
      {(tarefa.prioridade || tarefa.prazo) && (
        <div className="meta">
          {tarefa.prioridade && (
            <span className="etiqueta" data-prioridade={tarefa.prioridade}>
              <span className="so-leitores">Prioridade: </span>{nomePrioridade(tarefa.prioridade)}
            </span>
          )}
          {tarefa.prazo && (
            <span>
              <span className="so-leitores">Prazo: </span>{formatarPrazo(tarefa.prazo)}
            </span>
          )}
        </div>
      )}
    </li>
  )
}
