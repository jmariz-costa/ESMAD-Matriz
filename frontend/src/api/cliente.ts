// Cliente HTTP central da Matriz: todos os pedidos à API passam por aqui.
// O endereço é sempre o caminho relativo /api; em desenvolvimento, o proxy do
// Vite encaminha-o para o Backend (vite.config.ts).

const BASE = '/api'

export interface DetalheErro {
  campo: string
  mensagem: string
}

/** Formato único de erro do Backend (openapi.yaml, schema Erro). */
export interface CorpoErro {
  codigo: string
  mensagem: string
  detalhes?: DetalheErro[]
}

export class ErroApi extends Error {
  readonly estado: number
  readonly codigo: string
  readonly detalhes: DetalheErro[]

  constructor(estado: number, corpo: CorpoErro) {
    super(corpo.mensagem)
    this.name = 'ErroApi'
    this.estado = estado
    this.codigo = corpo.codigo
    this.detalhes = corpo.detalhes ?? []
  }
}

const SEM_LIGACAO: CorpoErro = {
  codigo: 'SEM_LIGACAO',
  mensagem: 'Não foi possível contactar o servidor. Tente novamente.',
}

export const ERRO_INESPERADO: CorpoErro = {
  codigo: 'ERRO_INESPERADO',
  mensagem: 'Ocorreu um erro inesperado. Tente novamente.',
}

let aoTerminarSessao: (() => void) | null = null

/**
 * Regista quem deve ser avisado quando um pedido devolve 401 (sessão expirada
 * ou terminada, D06). Devolve a função que cancela o registo.
 */
export function definirAoTerminarSessao(ouvinte: () => void): () => void {
  aoTerminarSessao = ouvinte
  return () => {
    if (aoTerminarSessao === ouvinte) {
      aoTerminarSessao = null
    }
  }
}

export interface OpcoesPedido {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  corpo?: unknown
  /** O 401 faz parte da resposta esperada (login, verificação inicial) e não termina a sessão. */
  ignorar401?: boolean
}

export async function pedido<T>(caminho: string, opcoes: OpcoesPedido = {}): Promise<T> {
  const { metodo = 'GET', corpo, ignorar401 = false } = opcoes
  let resposta: Response
  try {
    resposta = await fetch(BASE + caminho, {
      method: metodo,
      credentials: 'same-origin',
      headers: corpo === undefined ? undefined : { 'Content-Type': 'application/json' },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new ErroApi(0, SEM_LIGACAO)
  }

  if (resposta.status === 401 && !ignorar401) {
    aoTerminarSessao?.()
  }
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, await lerErro(resposta))
  }
  if (resposta.status === 204) {
    return undefined as T
  }
  return (await resposta.json()) as T
}

async function lerErro(resposta: Response): Promise<CorpoErro> {
  try {
    const corpo: unknown = await resposta.json()
    if (
      typeof corpo === 'object' && corpo !== null &&
      typeof (corpo as CorpoErro).codigo === 'string' &&
      typeof (corpo as CorpoErro).mensagem === 'string'
    ) {
      return corpo as CorpoErro
    }
  } catch {
    // corpo vazio ou que não é JSON
  }
  return ERRO_INESPERADO
}

/** Qualquer erro apanhado num pedido, como ErroApi (para mostrar sempre uma mensagem compreensível). */
export function comoErroApi(erro: unknown): ErroApi {
  return erro instanceof ErroApi ? erro : new ErroApi(0, ERRO_INESPERADO)
}
