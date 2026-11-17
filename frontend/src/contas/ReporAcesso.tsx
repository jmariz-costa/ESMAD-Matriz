import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { reporAcesso, type Conta } from '../api/contas'
import { Dialogo } from '../componentes/Dialogo'

const TAMANHO_MINIMO = 8 // RN04

interface Props {
  conta: Conta
  aoFechar: () => void
  aoRepor: () => void
}

/**
 * Repor o acesso de quem se esqueceu da palavra-passe (US19 crit. 2 e 3): o Administrador
 * escolhe uma palavra-passe temporária e comunica-a à pessoa, que a troca no próximo login (PR20).
 * O campo é de texto visível, porque o Administrador precisa de a ler para a comunicar.
 */
export function ReporAcesso({ conta, aoFechar, aoRepor }: Props) {
  const [temporaria, setTemporaria] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aRepor, setARepor] = useState(false)

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if ([...temporaria].length < TAMANHO_MINIMO) {
      setErro('A palavra-passe deve ter pelo menos 8 caracteres.')
      return
    }
    setErro(null)
    setErroGeral(null)
    setARepor(true)
    try {
      await reporAcesso(conta.id, temporaria)
      aoRepor()
    } catch (e) {
      const erroApi = comoErroApi(e)
      const doCampo = erroApi.detalhes.find((d) => d.campo === 'palavraPasseTemporaria')
      setErro(doCampo?.mensagem ?? null)
      setErroGeral(doCampo ? null : erroApi.message)
      setARepor(false)
    }
  }

  return (
    <Dialogo titulo={`Repor o acesso de ${conta.nome}?`} aoFechar={aoFechar}>
      <form onSubmit={submeter} noValidate>
        {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}
        <p>
          A palavra-passe atual deixa de funcionar e as sessões abertas de {conta.nome} terminam.
          Comunique-lhe a palavra-passe temporária: no próximo login, vai ter de a trocar por uma nova.
        </p>

        <label htmlFor="palavra-passe-temporaria">Palavra-passe temporária (pelo menos 8 caracteres)</label>
        <input
          id="palavra-passe-temporaria"
          type="text"
          autoComplete="off"
          spellCheck={false}
          className={erro ? 'campo erro' : 'campo'}
          value={temporaria}
          onChange={(e) => setTemporaria(e.target.value)}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? 'palavra-passe-temporaria-erro' : undefined}
        />
        {erro && <div id="palavra-passe-temporaria-erro" className="msg-erro">{erro}</div>}

        <div className="botoes">
          <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn principal" disabled={aRepor}>
            {aRepor ? 'A repor…' : 'Repor acesso'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
