import { useEffect, useState, type FormEvent } from 'react'
import { comoErroApi, type ErroApi } from '../api/cliente'
import { useNotificar } from '../comum/notificacao'
import { comentarTarefa, listarComentarios, type Comentario } from '../api/comentarios'
import { formatarDataHora } from './textos'

interface Props {
  tarefaId: string
  /** Projeto arquivado: os comentários leem-se, mas não se publicam (RN15). */
  soLeitura: boolean
}

interface Lista {
  itens: Comentario[]
  total: number
  paginas: number
}

/** Caracteres como o utilizador os vê (um emoji conta como um), igual ao Backend. */
const comprimento = (texto: string) => [...texto].length

function validar(texto: string): string | null {
  const n = comprimento(texto.trim())
  if (n === 0) return 'O comentário não pode estar vazio.'
  if (n > 2000) return 'O comentário não pode ter mais de 2000 caracteres.'
  return null
}

/**
 * Comentários da tarefa (US12 crit. 1 e 2): do mais antigo para o mais recente, com o autor e a
 * data definidos pelo Backend; os primeiros 20 e "Ver mais comentários".
 */
export function ComentariosTarefa({ tarefaId, soLeitura }: Props) {
  const [lista, setLista] = useState<Lista | null>(null)
  const [erroCarregar, setErroCarregar] = useState<ErroApi | null>(null)
  const [tentativa, setTentativa] = useState(0)
  const [aCarregarMais, setACarregarMais] = useState(false)
  const [texto, setTexto] = useState('')
  const [erroTexto, setErroTexto] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [aPublicar, setAPublicar] = useState(false)
  const notificar = useNotificar()

  useEffect(() => {
    let ativo = true
    listarComentarios(tarefaId, 1)
      .then((pagina) => ativo && setLista({ itens: pagina.itens, total: pagina.total, paginas: 1 }))
      .catch((e: unknown) => ativo && setErroCarregar(comoErroApi(e)))
    return () => {
      ativo = false
    }
  }, [tarefaId, tentativa])

  function tentarNovamente() {
    setErroCarregar(null)
    setTentativa((t) => t + 1)
  }

  async function verMais() {
    if (!lista) return
    setACarregarMais(true)
    setAviso(null)
    try {
      const seguinte = await listarComentarios(tarefaId, lista.paginas + 1)
      setLista((atual) => {
        if (!atual) return atual
        const vistos = new Set(atual.itens.map((c) => c.id))
        return {
          itens: [...atual.itens, ...seguinte.itens.filter((c) => !vistos.has(c.id))],
          total: seguinte.total,
          paginas: atual.paginas + 1,
        }
      })
    } catch (e) {
      setAviso(comoErroApi(e).message)
    } finally {
      setACarregarMais(false)
    }
  }

  async function publicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const erro = validar(texto)
    setErroTexto(erro)
    if (erro) return
    setAviso(null)
    setAPublicar(true)
    try {
      const novo = await comentarTarefa(tarefaId, texto.trim())
      setTexto('')
      notificar('Comentário publicado.')
      // O novo é o mais recente: só aparece já se todos os anteriores estiverem carregados
      setLista((atual) => atual && {
        ...atual,
        itens: atual.itens.length === atual.total ? [...atual.itens, novo] : atual.itens,
        total: atual.total + 1,
      })
    } catch (e) {
      // O texto escrito mantém-se
      const erroApi = comoErroApi(e)
      const doCampo = erroApi.detalhes.find((d) => d.campo === 'texto')
      setErroTexto(doCampo?.mensagem ?? null)
      setAviso(doCampo ? null : erroApi.message)
    } finally {
      setAPublicar(false)
    }
  }

  const restantes = lista ? lista.total - lista.itens.length : 0

  return (
    <section className="comentarios" aria-labelledby="titulo-comentarios">
      <h3 id="titulo-comentarios">Comentários{lista && ` (${lista.total})`}</h3>

      {!lista && !erroCarregar && <p className="suave">A carregar os comentários…</p>}
      {erroCarregar && (
        <div role="alert">
          <p>Não foi possível carregar os comentários.</p>
          <button type="button" className="btn pequeno" onClick={tentarNovamente}>Tentar novamente</button>
        </div>
      )}
      {lista && lista.total === 0 && <p className="suave">Ainda não há comentários.</p>}
      {lista && lista.itens.length > 0 && (
        <ol className="lista-comentarios">
          {lista.itens.map((c) => (
            <li key={c.id}>
              <div className="cabecalho-comentario">
                <strong>{c.autor.nome}</strong>
                <time dateTime={c.criadoEm}>{formatarDataHora(c.criadoEm)}</time>
              </div>
              <p>{c.texto}</p>
            </li>
          ))}
        </ol>
      )}
      {restantes > 0 && (
        <button type="button" className="ver-mais" disabled={aCarregarMais} onClick={() => void verMais()}>
          {aCarregarMais ? 'A carregar…' : `Ver mais comentários (${restantes})`}
        </button>
      )}

      {aviso && <div className="aviso" role="alert">{aviso}</div>}

      {!soLeitura && (
        <form onSubmit={publicar} noValidate>
          <label htmlFor="comentario-texto">Escrever comentário</label>
          <textarea id="comentario-texto" className={erroTexto ? 'campo erro' : 'campo'} rows={3} value={texto}
            onChange={(e) => setTexto(e.target.value)} aria-invalid={erroTexto ? true : undefined}
            aria-describedby={erroTexto ? 'comentario-texto-erro' : undefined} />
          {erroTexto && <div id="comentario-texto-erro" className="msg-erro">{erroTexto}</div>}
          <div className="botoes">
            <button type="submit" className="btn principal" disabled={aPublicar}>
              {aPublicar ? 'A publicar…' : 'Publicar'}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}
