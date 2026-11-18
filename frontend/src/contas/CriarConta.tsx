import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { criarConta, type Conta } from '../api/contas'
import { Dialogo } from '../componentes/Dialogo'
import { comprimento, limpar, validarTexto } from '../comum/texto'

const TAMANHO_MINIMO = 8 // RN04
const EMAIL_MAXIMO = 254

type Campo = 'nome' | 'email' | 'palavraPasseTemporaria'
type Erros = Partial<Record<Campo, string>>

interface Props {
  aoFechar: () => void
  aoCriar: (conta: Conta) => void
}

/**
 * As mesmas regras do Backend (Utilizador.novoMembro), para avisar antes de enviar. O domínio da
 * empresa (R06) só o servidor conhece: esse erro vem na resposta.
 */
function validar(nome: string, email: string, temporaria: string): Erros {
  const erros: Erros = {}
  const erroNome = validarTexto(nome, {
    obrigatorio: 'O nome é obrigatório.', minimo: 2, maximo: 150,
    foraDosLimites: 'O nome deve ter entre 2 e 150 caracteres.',
  })
  if (erroNome) erros.nome = erroNome
  const emailLimpo = limpar(email)
  const arroba = emailLimpo.indexOf('@')
  if (emailLimpo === '') {
    erros.email = 'O email é obrigatório.'
  } else if (comprimento(emailLimpo) > EMAIL_MAXIMO) {
    erros.email = 'O email deve ter no máximo 254 caracteres.'
  } else if (arroba <= 0 || arroba !== emailLimpo.lastIndexOf('@') || arroba === emailLimpo.length - 1
    || /[\s\p{Zs}]/u.test(emailLimpo)) {
    erros.email = 'Indique um email válido, no formato nome@dominio.'
  }
  if (comprimento(temporaria) < TAMANHO_MINIMO) {
    erros.palavraPasseTemporaria = temporaria === ''
      ? 'A palavra-passe é obrigatória.'
      : 'A palavra-passe deve ter pelo menos 8 caracteres.'
  }
  return erros
}

/**
 * Criar a conta de uma pessoa nova (US21). O Administrador escolhe uma palavra-passe temporária e
 * comunica-a à pessoa, que a troca no primeiro login (PR20). Como em "Repor acesso", o campo é de
 * texto visível, porque o Administrador precisa de a ler para a comunicar.
 */
export function CriarConta({ aoFechar, aoCriar }: Props) {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [temporaria, setTemporaria] = useState('')
  const [erros, setErros] = useState<Erros>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aCriar, setACriar] = useState(false)

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novos = validar(nome, email, temporaria)
    setErros(novos)
    setErroGeral(null)
    if (Object.keys(novos).length > 0) {
      return
    }
    setACriar(true)
    try {
      aoCriar(await criarConta({ nome: limpar(nome), email: limpar(email), palavraPasseTemporaria: temporaria }))
    } catch (e) {
      const erroApi = comoErroApi(e)
      const doServidor: Erros = {}
      for (const detalhe of erroApi.detalhes) {
        if (detalhe.campo === 'nome' || detalhe.campo === 'email' || detalhe.campo === 'palavraPasseTemporaria') {
          doServidor[detalhe.campo] ??= detalhe.mensagem
        }
      }
      // Email que já tem conta (409): o problema é do campo email
      if (erroApi.codigo === 'JA_EXISTE') {
        doServidor.email = erroApi.message
      }
      setErros(doServidor)
      setErroGeral(Object.keys(doServidor).length > 0 ? null : erroApi.message)
      setACriar(false)
    }
  }

  function campo(id: string, nomeCampo: Campo) {
    const erro = erros[nomeCampo]
    return {
      id,
      className: erro ? 'campo erro' : 'campo',
      'aria-invalid': erro ? true : undefined,
      'aria-describedby': erro ? `${id}-erro` : undefined,
    }
  }

  return (
    <Dialogo titulo="Criar conta" aoFechar={aoFechar}>
      <form onSubmit={submeter} noValidate>
        {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}
        <p>
          A conta fica ativa, como Membro. Comunique à pessoa o email e a palavra-passe temporária:
          no primeiro login, vai ter de a trocar por uma nova.
        </p>

        <label htmlFor="nova-conta-nome">Nome *</label>
        <input {...campo('nova-conta-nome', 'nome')} value={nome} onChange={(e) => setNome(e.target.value)} />
        {erros.nome && <div id="nova-conta-nome-erro" className="msg-erro">{erros.nome}</div>}

        <label htmlFor="nova-conta-email">Email *</label>
        <input {...campo('nova-conta-email', 'email')} type="email" autoComplete="off" spellCheck={false}
          value={email} onChange={(e) => setEmail(e.target.value)} />
        {erros.email && <div id="nova-conta-email-erro" className="msg-erro">{erros.email}</div>}

        <label htmlFor="nova-conta-palavra-passe">Palavra-passe temporária * (pelo menos 8 caracteres)</label>
        <input {...campo('nova-conta-palavra-passe', 'palavraPasseTemporaria')} type="text" autoComplete="off"
          spellCheck={false} value={temporaria} onChange={(e) => setTemporaria(e.target.value)} />
        {erros.palavraPasseTemporaria && (
          <div id="nova-conta-palavra-passe-erro" className="msg-erro">{erros.palavraPasseTemporaria}</div>
        )}

        <div className="botoes">
          <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn principal" disabled={aCriar}>
            {aCriar ? 'A criar…' : 'Criar conta'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
