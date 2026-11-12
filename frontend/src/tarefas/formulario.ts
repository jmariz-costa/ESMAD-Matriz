import type { ErroApi } from '../api/cliente'
import { validarTexto } from '../comum/texto'
import type { Estado } from '../api/projetos'
import type { AlteracoesTarefa, DadosTarefa, Prioridade, Tarefa } from '../api/tarefas'

/** O que está escrito no formulário; campos vazios são texto vazio. */
export interface ValoresTarefa {
  titulo: string
  descricao: string
  /** AAAA-MM-DD, como o campo de data o devolve. */
  prazo: string
  prioridade: Prioridade | ''
  estado: Estado
}

export type CampoTarefa = keyof ValoresTarefa

export type ErrosTarefa = Partial<Record<CampoTarefa, string>>

const CAMPOS: CampoTarefa[] = ['titulo', 'descricao', 'prazo', 'prioridade', 'estado']

export function valoresDe(tarefa?: Tarefa): ValoresTarefa {
  return {
    titulo: tarefa?.titulo ?? '',
    descricao: tarefa?.descricao ?? '',
    prazo: tarefa?.prazo ?? '',
    prioridade: tarefa?.prioridade ?? '',
    estado: tarefa?.estado ?? 'A_FAZER',
  }
}

/** As mesmas regras do Backend (RN10, RN23); o Backend volta a verificar. */
export function validar(valores: ValoresTarefa): ErrosTarefa {
  const erros: ErrosTarefa = {}
  const erroTitulo = validarTexto(valores.titulo, {
    obrigatorio: 'O título é obrigatório.', minimo: 3, maximo: 150,
    foraDosLimites: 'O título deve ter entre 3 e 150 caracteres.',
  })
  const erroDescricao = validarTexto(valores.descricao, {
    maximo: 2000, foraDosLimites: 'A descrição não pode ter mais de 2000 caracteres.',
  })
  if (erroTitulo) erros.titulo = erroTitulo
  if (erroDescricao) erros.descricao = erroDescricao
  return erros
}

export function paraDados(valores: ValoresTarefa): DadosTarefa {
  const descricao = valores.descricao.trim()
  return {
    titulo: valores.titulo.trim(),
    descricao: descricao === '' ? null : descricao,
    prazo: valores.prazo === '' ? null : valores.prazo,
    prioridade: valores.prioridade === '' ? null : valores.prioridade,
  }
}

/** Só os campos que mudaram em relação à tarefa carregada, com a versão dela (D08). */
export function alteracoesDe(base: Tarefa, valores: ValoresTarefa): AlteracoesTarefa {
  const dados = paraDados(valores)
  const alteracoes: AlteracoesTarefa = { versao: base.versao }
  if (dados.titulo !== base.titulo) alteracoes.titulo = dados.titulo
  if (dados.descricao !== base.descricao) alteracoes.descricao = dados.descricao
  if (dados.prazo !== base.prazo) alteracoes.prazo = dados.prazo
  if (dados.prioridade !== base.prioridade) alteracoes.prioridade = dados.prioridade
  if (valores.estado !== base.estado) alteracoes.estado = valores.estado
  return alteracoes
}

/**
 * Depois de recarregar uma tarefa que outra pessoa alterou (D08): os campos que o utilizador
 * mudou mantêm o que ele escreveu; os outros passam a ter os valores mais recentes.
 */
export function juntarComNovaVersao(anterior: Tarefa, recente: Tarefa, valores: ValoresTarefa): ValoresTarefa {
  const original = valoresDe(anterior)
  const novos = valoresDe(recente)
  const resultado = { ...novos }
  for (const campo of CAMPOS) {
    if (valores[campo] !== original[campo]) {
      Object.assign(resultado, { [campo]: valores[campo] })
    }
  }
  return resultado
}

/** Os erros por campo devolvidos pelo Backend (400 VALIDACAO). */
export function errosDoServidor(erro: ErroApi): ErrosTarefa {
  const erros: ErrosTarefa = {}
  for (const detalhe of erro.detalhes) {
    if ((CAMPOS as string[]).includes(detalhe.campo)) {
      erros[detalhe.campo as CampoTarefa] = detalhe.mensagem
    }
  }
  return erros
}
