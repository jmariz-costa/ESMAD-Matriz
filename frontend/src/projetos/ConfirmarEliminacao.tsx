import { useState } from 'react'
import { comoErroApi } from '../api/cliente'
import { eliminarProjeto, type Projeto } from '../api/projetos'
import { Dialogo } from '../componentes/Dialogo'
import { totalTarefas } from './percentagem'

interface Props {
  projeto: Projeto
  aoFechar: () => void
  aoEliminar: () => void
}

function oQueEApagado(tarefas: number): string {
  if (tarefas === 0) {
    return 'Esta ação é definitiva. O projeto não tem tarefas.'
  }
  const quais = tarefas === 1 ? 'a tarefa do projeto' : `as ${tarefas} tarefas do projeto`
  return `Esta ação é definitiva. Serão apagadas também ${quais}, com os seus responsáveis, comentários e histórico.`
}

/** Confirmação antes de eliminar, a explicar o que é apagado (ecrã 3, nota 10; RN22, L06). */
export function ConfirmarEliminacao({ projeto, aoFechar, aoEliminar }: Props) {
  const [aEliminar, setAEliminar] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function eliminar() {
    setErro(null)
    setAEliminar(true)
    try {
      await eliminarProjeto(projeto.id)
      aoEliminar()
    } catch (e) {
      setErro(comoErroApi(e).message)
      setAEliminar(false)
    }
  }

  return (
    <Dialogo titulo={`Eliminar o projeto "${projeto.nome}"?`} aoFechar={aoFechar}>
      {erro && <div className="aviso" role="alert">{erro}</div>}
      <p>{oQueEApagado(totalTarefas(projeto.contagens))}</p>
      <div className="botoes">
        <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
        <button type="button" className="btn principal" onClick={() => void eliminar()} disabled={aEliminar}>
          {aEliminar ? 'A eliminar…' : 'Eliminar definitivamente'}
        </button>
      </div>
    </Dialogo>
  )
}
