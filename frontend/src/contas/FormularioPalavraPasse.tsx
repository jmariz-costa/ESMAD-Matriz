import { useState, type FormEvent } from 'react'
import { mudarPalavraPasse } from '../api/autenticacao'
import { comoErroApi } from '../api/cliente'

const TAMANHO_MINIMO = 8 // RN04

interface Erros {
  atual?: string
  nova?: string
  confirmacao?: string
}

function validar(atual: string, nova: string, confirmacao: string): Erros {
  const erros: Erros = {}
  if (atual === '') {
    erros.atual = 'Indique a palavra-passe atual.'
  }
  if ([...nova].length < TAMANHO_MINIMO) {
    erros.nova = 'A palavra-passe deve ter pelo menos 8 caracteres.'
  } else if (nova === atual) {
    erros.nova = 'A palavra-passe nova tem de ser diferente da atual.'
  }
  if (confirmacao !== nova) {
    erros.confirmacao = 'As palavras-passe não coincidem.'
  }
  return erros
}

interface Props {
  /** "Palavra-passe atual" ou, depois de repor o acesso, "Palavra-passe temporária". */
  rotuloAtual: string
  textoBotao: string
  aoMudar: () => void
  aoCancelar?: () => void
}

/**
 * Palavra-passe atual, nova e confirmação (US20). A confirmação só é verificada aqui; o resto
 * o Backend volta a verificar, e os erros dele aparecem junto ao campo.
 */
export function FormularioPalavraPasse({ rotuloAtual, textoBotao, aoMudar, aoCancelar }: Props) {
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erros, setErros] = useState<Erros>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aGuardar, setAGuardar] = useState(false)

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novosErros = validar(atual, nova, confirmacao)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) {
      return
    }
    setErroGeral(null)
    setAGuardar(true)
    try {
      await mudarPalavraPasse(atual, nova)
      aoMudar()
    } catch (e) {
      const erro = comoErroApi(e)
      const doServidor: Erros = {}
      for (const detalhe of erro.detalhes) {
        if (detalhe.campo === 'palavraPasseAtual') doServidor.atual = detalhe.mensagem
        if (detalhe.campo === 'palavraPasseNova') doServidor.nova = detalhe.mensagem
      }
      setErros(doServidor)
      setErroGeral(Object.keys(doServidor).length > 0 ? null : erro.message)
      setAtual('')
      setAGuardar(false)
    }
  }

  function campo(id: string, rotulo: string, valor: string, mudar: (v: string) => void, erro: string | undefined,
                 autoComplete: string) {
    return (
      <>
        <label htmlFor={id}>{rotulo}</label>
        <input
          id={id}
          type="password"
          autoComplete={autoComplete}
          className={erro ? 'campo erro' : 'campo'}
          value={valor}
          onChange={(e) => mudar(e.target.value)}
          aria-invalid={erro ? true : undefined}
          aria-describedby={erro ? `${id}-erro` : undefined}
        />
        {erro && <div id={`${id}-erro`} className="msg-erro">{erro}</div>}
      </>
    )
  }

  return (
    <form onSubmit={submeter} noValidate>
      {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}
      {campo('palavra-passe-atual', rotuloAtual, atual, setAtual, erros.atual, 'current-password')}
      {campo('palavra-passe-nova', 'Palavra-passe nova (pelo menos 8 caracteres)', nova, setNova, erros.nova, 'new-password')}
      {campo('palavra-passe-confirmacao', 'Repita a palavra-passe nova', confirmacao, setConfirmacao,
        erros.confirmacao, 'new-password')}
      <div className="botoes">
        {aoCancelar && <button type="button" className="btn" onClick={aoCancelar}>Cancelar</button>}
        <button type="submit" className="btn principal" disabled={aGuardar}>
          {aGuardar ? 'A guardar…' : textoBotao}
        </button>
      </div>
    </form>
  )
}
