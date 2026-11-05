import { useState } from 'react'
import { comoErroApi } from '../api/cliente'
import { removerMembro, type Membro } from '../api/membros'
import { Dialogo } from '../componentes/Dialogo'

interface Props {
  projetoId: string
  membro: Membro
  aoFechar: () => void
  aoRemover: () => void
}

/** Confirmação antes de remover um membro, a explicar as consequências (US04 crit. 4, RN08). */
export function ConfirmarRemocaoMembro({ projetoId, membro, aoFechar, aoRemover }: Props) {
  const [aRemover, setARemover] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function remover() {
    setErro(null)
    setARemover(true)
    try {
      await removerMembro(projetoId, membro.id)
      aoRemover()
    } catch (e) {
      setErro(comoErroApi(e).message)
      setARemover(false)
    }
  }

  return (
    <Dialogo titulo={`Remover ${membro.nome} do projeto?`} aoFechar={aoFechar}>
      {erro && <div className="aviso" role="alert">{erro}</div>}
      <p>Deixa de ver o projeto e de ser responsável pelas tarefas não concluídas.</p>
      <div className="botoes">
        <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
        <button type="button" className="btn principal" onClick={() => void remover()} disabled={aRemover}>
          {aRemover ? 'A remover…' : 'Remover'}
        </button>
      </div>
    </Dialogo>
  )
}
