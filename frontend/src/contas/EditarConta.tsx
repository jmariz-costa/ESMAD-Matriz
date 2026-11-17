import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { editarConta, type Conta } from '../api/contas'
import { Dialogo } from '../componentes/Dialogo'
import { validarTexto } from '../comum/texto'

interface Props {
  conta: Conta
  aoFechar: () => void
  aoGuardar: (conta: Conta) => void
}

function validar(nome: string): string | null {
  return validarTexto(nome, {
    obrigatorio: 'O nome é obrigatório.', minimo: 2, maximo: 150,
    foraDosLimites: 'O nome deve ter entre 2 e 150 caracteres.',
  })
}

/** Editar o nome de uma conta de Membro (US19 crit. 1); o email não se altera. */
export function EditarConta({ conta, aoFechar, aoGuardar }: Props) {
  const [nome, setNome] = useState(conta.nome)
  const [erro, setErro] = useState<string | null>(null)
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aGuardar, setAGuardar] = useState(false)

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const erroNome = validar(nome)
    setErro(erroNome)
    if (erroNome) {
      return
    }
    setErroGeral(null)
    setAGuardar(true)
    try {
      aoGuardar(await editarConta(conta.id, nome.trim()))
    } catch (e) {
      const erroApi = comoErroApi(e)
      const doNome = erroApi.detalhes.find((d) => d.campo === 'nome')
      setErro(doNome?.mensagem ?? null)
      setErroGeral(doNome ? null : erroApi.message)
      setAGuardar(false)
    }
  }

  return (
    <Dialogo titulo="Editar conta" aoFechar={aoFechar}>
      <form onSubmit={submeter} noValidate>
        {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}

        <label htmlFor="conta-nome">Nome *</label>
        <input
          id="conta-nome"
          className={erro ? 'campo erro' : 'campo'}
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? 'conta-nome-erro' : undefined}
        />
        {erro && <div id="conta-nome-erro" className="msg-erro">{erro}</div>}

        <label htmlFor="conta-email">Email</label>
        <input id="conta-email" className="campo" value={conta.email} readOnly aria-describedby="conta-email-nota" />
        <div id="conta-email-nota" className="suave nota-campo">O email é a credencial de entrada e não se altera.</div>

        <div className="botoes">
          <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn principal" disabled={aGuardar}>
            {aGuardar ? 'A guardar…' : 'Guardar'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
