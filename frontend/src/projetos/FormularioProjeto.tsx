import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { criarProjeto, editarProjeto, type Projeto } from '../api/projetos'
import { Dialogo } from '../componentes/Dialogo'

interface Props {
  /** Sem projeto, o formulário cria um novo; com projeto, edita-o. */
  projeto?: Projeto
  aoFechar: () => void
  aoGuardar: (projeto: Projeto) => void
}

interface Erros {
  nome?: string
  descricao?: string
}

/** Caracteres como o utilizador os vê (um emoji conta como um), igual ao Backend. */
const comprimento = (texto: string) => [...texto].length

function validar(nome: string, descricao: string): Erros {
  const erros: Erros = {}
  const n = comprimento(nome.trim())
  if (n === 0) {
    erros.nome = 'O nome é obrigatório.'
  } else if (n < 3 || n > 100) {
    erros.nome = 'O nome deve ter entre 3 e 100 caracteres.'
  }
  if (comprimento(descricao.trim()) > 1000) {
    erros.descricao = 'A descrição não pode ter mais de 1000 caracteres.'
  }
  return erros
}

/** Criar e editar projeto no mesmo formulário (ecrã 3, nota 9; RN23). */
export function FormularioProjeto({ projeto, aoFechar, aoGuardar }: Props) {
  const [nome, setNome] = useState(projeto?.nome ?? '')
  const [descricao, setDescricao] = useState(projeto?.descricao ?? '')
  const [erros, setErros] = useState<Erros>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [aGuardar, setAGuardar] = useState(false)

  async function submeter(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novosErros = validar(nome, descricao)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) {
      return
    }
    const dados = { nome: nome.trim(), descricao: descricao.trim() === '' ? null : descricao.trim() }
    setErroGeral(null)
    setAGuardar(true)
    try {
      aoGuardar(projeto ? await editarProjeto(projeto.id, dados) : await criarProjeto(dados))
    } catch (e) {
      // O texto escrito mantém-se; os erros do servidor aparecem junto ao campo
      const erro = comoErroApi(e)
      const doServidor: Erros = {}
      for (const detalhe of erro.detalhes) {
        if (detalhe.campo === 'nome' || detalhe.campo === 'descricao') {
          doServidor[detalhe.campo] = detalhe.mensagem
        }
      }
      setErros(doServidor)
      setErroGeral(Object.keys(doServidor).length > 0 ? null : erro.message)
      setAGuardar(false)
    }
  }

  return (
    <Dialogo titulo={projeto ? 'Editar projeto' : 'Novo projeto'} aoFechar={aoFechar}>
      <form onSubmit={submeter} noValidate>
        {erroGeral && <div className="aviso" role="alert">{erroGeral}</div>}

        <label htmlFor="projeto-nome">Nome *</label>
        <input
          id="projeto-nome"
          className={erros.nome ? 'campo erro' : 'campo'}
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          aria-invalid={erros.nome ? true : undefined}
          aria-describedby={erros.nome ? 'projeto-nome-erro' : undefined}
        />
        {erros.nome && <div id="projeto-nome-erro" className="msg-erro">{erros.nome}</div>}

        <label htmlFor="projeto-descricao">Descrição</label>
        <textarea
          id="projeto-descricao"
          className={erros.descricao ? 'campo erro' : 'campo'}
          rows={4}
          value={descricao}
          onChange={(e) => setDescricao(e.target.value)}
          aria-invalid={erros.descricao ? true : undefined}
          aria-describedby={erros.descricao ? 'projeto-descricao-erro' : undefined}
        />
        {erros.descricao && <div id="projeto-descricao-erro" className="msg-erro">{erros.descricao}</div>}

        <div className="botoes">
          <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn principal" disabled={aGuardar}>
            {aGuardar ? 'A guardar…' : projeto ? 'Guardar' : 'Criar'}
          </button>
        </div>
      </form>
    </Dialogo>
  )
}
