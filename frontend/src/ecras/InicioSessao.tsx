import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router'
import { ErroApi } from '../api/cliente'
import { useSessao } from '../sessao/sessao'

interface ErrosCampos {
  email?: string
  palavraPasse?: string
}

const FORMATO_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validar(email: string, palavraPasse: string): ErrosCampos {
  const erros: ErrosCampos = {}
  if (email.trim() === '') {
    erros.email = 'Indique o email.'
  } else if (!FORMATO_EMAIL.test(email.trim())) {
    erros.email = 'Indique um email válido.'
  }
  if (palavraPasse === '') {
    erros.palavraPasse = 'Indique a palavra-passe.'
  }
  return erros
}

/** Ecrã 1 · Início de sessão (docs/ui/wireframe-ecras.html, US02). */
export function InicioSessao() {
  const { estado, mensagem, entrar } = useSessao()
  const local = useLocation()
  const [email, setEmail] = useState('')
  const [palavraPasse, setPalavraPasse] = useState('')
  const [erros, setErros] = useState<ErrosCampos>({})
  const [erro, setErro] = useState<string | null>(null)
  const [aEntrar, setAEntrar] = useState(false)

  if (estado.tipo === 'autenticado') {
    const origem = (local.state as { origem?: string } | null)?.origem ?? '/'
    return <Navigate to={origem} replace />
  }

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novosErros = validar(email, palavraPasse)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) {
      return
    }
    setErro(null)
    setAEntrar(true)
    try {
      await entrar(email.trim(), palavraPasse)
    } catch (e) {
      // Mensagem genérica do servidor (PR03); o email mantém-se, a palavra-passe é limpa
      setErro(e instanceof ErroApi ? e.message : 'Ocorreu um erro inesperado. Tente novamente.')
      setPalavraPasse('')
      setAEntrar(false)
    }
  }

  const aviso = erro ?? mensagem

  return (
    <main className="login-fundo">
      <form className="login caixa" onSubmit={submeter} noValidate>
        <span className="logo">MATRIZ</span>
        <h1>Iniciar sessão</h1>

        {aviso && <div className="aviso" role="alert">{aviso}</div>}

        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          autoComplete="username"
          className={erros.email ? 'campo erro' : 'campo'}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={erros.email ? true : undefined}
          aria-describedby={erros.email ? 'email-erro' : undefined}
        />
        {erros.email && <div id="email-erro" className="msg-erro">{erros.email}</div>}

        <label htmlFor="palavra-passe">Palavra-passe</label>
        <input
          id="palavra-passe"
          type="password"
          autoComplete="current-password"
          className={erros.palavraPasse ? 'campo erro' : 'campo'}
          value={palavraPasse}
          onChange={(e) => setPalavraPasse(e.target.value)}
          aria-invalid={erros.palavraPasse ? true : undefined}
          aria-describedby={erros.palavraPasse ? 'palavra-passe-erro' : undefined}
        />
        {erros.palavraPasse && (
          <div id="palavra-passe-erro" className="msg-erro">{erros.palavraPasse}</div>
        )}

        <button type="submit" className="btn principal" disabled={aEntrar}>
          {aEntrar ? 'A entrar…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
