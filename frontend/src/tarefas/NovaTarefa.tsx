import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { criarTarefa } from '../api/tarefas'
import { Dialogo } from '../componentes/Dialogo'
import { CamposTarefa } from './CamposTarefa'
import { errosDoServidor, paraDados, validar, valoresDe, type CampoTarefa, type ErrosTarefa } from './formulario'

interface Props {
  projetoId: string
  aoFechar: () => void
  aoCriar: () => void
}

/** Criar uma tarefa (US05 crit. 1 a 3). Se o servidor recusar, o que foi escrito mantém-se. */
export function NovaTarefa({ projetoId, aoFechar, aoCriar }: Props) {
  const [valores, setValores] = useState(valoresDe)
  const [erros, setErros] = useState<ErrosTarefa>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aGuardar, setAGuardar] = useState(false)

  function mudar(campo: CampoTarefa, valor: string) {
    setValores((atuais) => ({ ...atuais, [campo]: valor }))
  }

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novosErros = validar(valores)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) {
      return
    }
    setErroGeral(null)
    setAGuardar(true)
    try {
      await criarTarefa(projetoId, paraDados(valores))
      aoCriar()
    } catch (e) {
      const erro = comoErroApi(e)
      const doServidor = errosDoServidor(erro)
      setErros(doServidor)
      setErroGeral(Object.keys(doServidor).length > 0 ? null : erro.message)
      setAGuardar(false)
    }
  }

  return (
    <Dialogo titulo="Nova tarefa" aoFechar={aoFechar}>
      <form onSubmit={submeter} noValidate>
        {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}
        <CamposTarefa valores={valores} erros={erros} aoMudar={mudar} />
        <div className="botoes">
          <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn principal" disabled={aGuardar}>
            {aGuardar ? 'A criar…' : 'Criar'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
