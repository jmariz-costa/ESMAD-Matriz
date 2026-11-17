import { useState } from 'react'
import { comoErroApi } from '../api/cliente'
import {
  desativarConta, listarContas, listarProjetosGeridos, TAMANHO_MAXIMO, type Conta, type ProjetoGerido,
} from '../api/contas'
import { Dialogo } from '../componentes/Dialogo'
import { useDados } from '../comum/useDados'

interface Props {
  conta: Conta
  aoFechar: () => void
  aoDesativar: () => void
}

interface Preparacao {
  projetos: ProjetoGerido[]
  /** Membros ativos que podem ser o novo Gestor (vazio se a pessoa não gere projetos). */
  candidatos: Conta[]
}

async function preparar(conta: Conta): Promise<Preparacao> {
  const projetos = await listarProjetosGeridos(conta.id)
  if (projetos.length === 0) {
    return { projetos, candidatos: [] }
  }
  const ativos = await listarContas({ estado: 'ATIVO', tamanho: TAMANHO_MAXIMO })
  return { projetos, candidatos: ativos.itens.filter((c) => c.id !== conta.id) }
}

/**
 * Desativar uma conta (US14 crit. 1 a 3). Explica o que acontece e, se a pessoa for Gestora,
 * pede o novo Gestor de cada projeto; o botão só fica ativo quando todos estão escolhidos.
 */
export function DesativarConta({ conta, aoFechar, aoDesativar }: Props) {
  const [dados, recarregar] = useDados(() => preparar(conta), conta.id)
  const [escolhas, setEscolhas] = useState<Record<string, string>>({})
  const [aDesativar, setADesativar] = useState(false)
  const [erros, setErros] = useState<string[]>([])

  const projetos = dados.estado === 'pronto' ? dados.dados.projetos : []
  const completo = dados.estado === 'pronto' && projetos.every((p) => escolhas[p.id])

  async function desativar() {
    setErros([])
    setADesativar(true)
    try {
      await desativarConta(conta.id, projetos.map((p) => ({ projetoId: p.id, gestorId: escolhas[p.id] })))
      aoDesativar()
    } catch (e) {
      const erro = comoErroApi(e)
      setErros(erro.detalhes.length > 0 ? erro.detalhes.map((d) => d.mensagem) : [erro.message])
      setADesativar(false)
    }
  }

  return (
    <Dialogo titulo={`Desativar a conta de ${conta.nome}?`} aoFechar={aoFechar} largo={projetos.length > 0}>
      {erros.length > 0 && (
        <div className="aviso" role="alert">
          {erros.length === 1 ? erros[0] : <ul className="lista-erros">{erros.map((m) => <li key={m}>{m}</li>)}</ul>}
        </div>
      )}

      <p>
        {conta.nome} deixa de conseguir entrar na Matriz e as sessões abertas terminam. Deixa de ser
        responsável pelas tarefas por concluir, em todos os projetos. Continua na lista de membros e a
        conta pode ser reativada mais tarde, mas sem recuperar tarefas nem o papel de Gestor.
      </p>

      {dados.estado === 'a-carregar' && <p className="suave">A verificar os projetos desta pessoa…</p>}

      {dados.estado === 'erro' && (
        <div className="aviso" role="alert">
          Não foi possível verificar os projetos desta pessoa.{' '}
          <button type="button" className="btn pequeno" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}

      {dados.estado === 'pronto' && projetos.length > 0 && (
        <fieldset className="novos-gestores">
          <legend>
            {projetos.length === 1
              ? 'É Gestor(a) de 1 projeto. Escolha o novo Gestor:'
              : `É Gestor(a) de ${projetos.length} projetos. Escolha o novo Gestor de cada um:`}
          </legend>
          <p className="suave">
            Só Membros ativos. Se a pessoa escolhida ainda não for membro do projeto, passa a sê-lo.
          </p>
          {projetos.map((projeto) => (
            <div key={projeto.id} className="novo-gestor">
              <label htmlFor={`gestor-${projeto.id}`}>
                {projeto.nome}
                {projeto.arquivado && <span className="etiqueta-arquivado">Arquivado</span>}
              </label>
              <select
                id={`gestor-${projeto.id}`}
                className="campo"
                value={escolhas[projeto.id] ?? ''}
                onChange={(e) => setEscolhas((atuais) => ({ ...atuais, [projeto.id]: e.target.value }))}
              >
                <option value="">Escolher o novo Gestor…</option>
                {dados.dados.candidatos.map((c) => (
                  <option key={c.id} value={c.id}>{c.nome} ({c.email})</option>
                ))}
              </select>
            </div>
          ))}
        </fieldset>
      )}

      <div className="botoes">
        <button type="button" className="btn" onClick={aoFechar}>Cancelar</button>
        <button type="button" className="btn perigo" onClick={() => void desativar()} disabled={!completo || aDesativar}>
          {aDesativar ? 'A desativar…' : 'Desativar conta'}
        </button>
      </div>
    </Dialogo>
  )
}
