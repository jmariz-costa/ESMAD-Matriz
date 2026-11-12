import { useState, type FormEvent } from 'react'
import { comoErroApi } from '../api/cliente'
import { consultarTarefa, editarTarefa, eliminarTarefa, type Tarefa } from '../api/tarefas'
import { Dialogo } from '../componentes/Dialogo'
import { useDados } from '../comum/useDados'
import { CamposTarefa } from './CamposTarefa'
import { ComentariosTarefa } from './ComentariosTarefa'
import { ResponsaveisTarefa } from './ResponsaveisTarefa'
import {
  alteracoesDe, errosDoServidor, juntarComNovaVersao, validar, valoresDe,
  type CampoTarefa, type ErrosTarefa,
} from './formulario'

interface Props {
  tarefaId: string
  /** Só o Gestor tem a opção de eliminar (US09 crit. 3); o servidor verifica sempre (S03). */
  podeEliminar: boolean
  /** Projeto arquivado: só leitura (RN15). */
  soLeitura: boolean
  aoFechar: () => void
  /** Depois de guardar ou eliminar, para atualizar o quadro e as contagens. */
  aoAlterar: () => void
}

const MENSAGEM_VERSAO =
  'A tarefa foi alterada por outra pessoa depois de a abrir. Recarregue-a para ver as alterações; o que escreveu mantém-se.'

/**
 * Detalhe de uma tarefa (wireframe.html, "Detalhe ao clicar num cartão"): editar os campos e
 * mudar o estado no seletor, a alternativa ao arrastar (nota 4); eliminar, só o Gestor, com
 * confirmação (US09 crit. 1); responsáveis e comentários (Ficha PL 03, Exercício 6).
 */
export function DetalheTarefa({ tarefaId, podeEliminar, soLeitura, aoFechar, aoAlterar }: Props) {
  const [dados, recarregar] = useDados(() => consultarTarefa(tarefaId), tarefaId)

  return (
    <Dialogo titulo="Tarefa" aoFechar={aoFechar} largo>
      {dados.estado === 'a-carregar' && <p className="suave">A carregar a tarefa…</p>}
      {dados.estado === 'erro' && (
        <>
          <div className="aviso" role="alert">
            {dados.erro.estado === 404 ? 'Esta tarefa já não existe.' : dados.erro.message}
          </div>
          <div className="botoes">
            <button type="button" className="btn" onClick={aoFechar}>Fechar</button>
            {dados.erro.estado !== 404 && dados.erro.estado !== 403 && (
              <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
            )}
          </div>
        </>
      )}
      {dados.estado === 'pronto' && (
        <EditarTarefa tarefa={dados.dados} podeEliminar={podeEliminar} soLeitura={soLeitura}
          aoFechar={aoFechar} aoAlterar={aoAlterar} />
      )}
    </Dialogo>
  )
}

interface PropsEditar {
  tarefa: Tarefa
  podeEliminar: boolean
  soLeitura: boolean
  aoFechar: () => void
  aoAlterar: () => void
}

function EditarTarefa({ tarefa, podeEliminar, soLeitura, aoFechar, aoAlterar }: PropsEditar) {
  const [base, setBase] = useState(tarefa)
  const [valores, setValores] = useState(() => valoresDe(tarefa))
  const [erros, setErros] = useState<ErrosTarefa>({})
  const [aviso, setAviso] = useState<string | null>(null)
  const [versaoAntiga, setVersaoAntiga] = useState(false)
  const [aGuardar, setAGuardar] = useState(false)
  const [aConfirmar, setAConfirmar] = useState(false)

  function mudar(campo: CampoTarefa, valor: string) {
    setValores((atuais) => ({ ...atuais, [campo]: valor }))
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const novosErros = validar(valores)
    setErros(novosErros)
    if (Object.keys(novosErros).length > 0) {
      return
    }
    const alteracoes = alteracoesDe(base, valores)
    if (Object.keys(alteracoes).length === 1) {
      // Só a versão: não há nada para guardar
      aoFechar()
      return
    }
    setAviso(null)
    setAGuardar(true)
    try {
      await editarTarefa(base.id, alteracoes)
      aoAlterar()
      aoFechar()
    } catch (e) {
      // O que foi escrito mantém-se (US05 crit. 3 e 8)
      const erro = comoErroApi(e)
      const doServidor = errosDoServidor(erro)
      setErros(doServidor)
      setVersaoAntiga(erro.codigo === 'VERSAO_DESATUALIZADA')
      if (erro.codigo === 'VERSAO_DESATUALIZADA') {
        setAviso(MENSAGEM_VERSAO)
      } else {
        setAviso(Object.keys(doServidor).length > 0 ? null : erro.message)
      }
      setAGuardar(false)
    }
  }

  async function recarregar() {
    setAGuardar(true)
    try {
      const recente = await consultarTarefa(base.id)
      setValores(juntarComNovaVersao(base, recente, valores))
      setBase(recente)
      setVersaoAntiga(false)
      setAviso(null)
      // O quadro também pode estar desatualizado
      aoAlterar()
    } catch (e) {
      setAviso(comoErroApi(e).message)
    } finally {
      setAGuardar(false)
    }
  }

  /** Depois de atribuir ou remover um responsável: a tarefa tem uma versão nova. */
  function aoMudarResponsaveis(recente: Tarefa) {
    setValores(juntarComNovaVersao(base, recente, valores))
    setBase(recente)
    if (versaoAntiga) {
      setVersaoAntiga(false)
      setAviso(null)
    }
    aoAlterar()
  }

  async function eliminar() {
    setAviso(null)
    setAGuardar(true)
    try {
      await eliminarTarefa(base.id)
      aoAlterar()
      aoFechar()
    } catch (e) {
      setAviso(comoErroApi(e).message)
      setAConfirmar(false)
      setAGuardar(false)
    }
  }

  return (
    <>
    <form onSubmit={guardar} noValidate>
      {aviso && (
        <div className="aviso" role="alert">
          {aviso}
          {versaoAntiga && (
            <div>
              <button type="button" className="btn" onClick={() => void recarregar()} disabled={aGuardar}>
                Recarregar tarefa
              </button>
            </div>
          )}
        </div>
      )}
      <CamposTarefa valores={valores} erros={erros} aoMudar={mudar} comEstado soLeitura={soLeitura} />
      <ResponsaveisTarefa tarefa={base} soLeitura={soLeitura} aoMudar={aoMudarResponsaveis} />

      {aConfirmar ? (
        <div className="confirmar caixa" role="group" aria-label="Confirmar eliminação">
          <p>
            Eliminar a tarefa <strong>{base.titulo}</strong>? Esta ação é definitiva: a tarefa, os seus
            comentários e o seu histórico deixam de estar disponíveis.
          </p>
          <div className="botoes">
            <button type="button" className="btn" onClick={() => setAConfirmar(false)} disabled={aGuardar}>
              Cancelar
            </button>
            <button type="button" className="btn principal" onClick={() => void eliminar()} disabled={aGuardar}>
              {aGuardar ? 'A eliminar…' : 'Eliminar definitivamente'}
            </button>
          </div>
        </div>
      ) : (
        <div className="botoes">
          {podeEliminar && !soLeitura && (
            <button type="button" className="btn gestor esquerda" onClick={() => setAConfirmar(true)} disabled={aGuardar}>
              Eliminar
            </button>
          )}
          <button type="button" className="btn" onClick={aoFechar}>{soLeitura ? 'Fechar' : 'Cancelar'}</button>
          {!soLeitura && (
            <button type="submit" className="btn principal" disabled={aGuardar || versaoAntiga}>
              {aGuardar ? 'A guardar…' : 'Guardar'}
            </button>
          )}
        </div>
      )}
    </form>
    <ComentariosTarefa tarefaId={base.id} soLeitura={soLeitura} />
    </>
  )
}
