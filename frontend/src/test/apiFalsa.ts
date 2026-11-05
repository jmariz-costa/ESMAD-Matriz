// Substitui o fetch nos testes por respostas definidas por rota ("MÉTODO /api/caminho").
import { vi } from 'vitest'
import type { Projeto } from '../api/projetos'

export interface RespostaFalsa {
  estado: number
  corpo?: unknown
}

type Rota = RespostaFalsa | (() => RespostaFalsa | Promise<RespostaFalsa>)

export function simularApi(rotas: Record<string, Rota>) {
  const fetchFalso = vi.fn(async (url: string, init?: RequestInit) => {
    const chave = `${init?.method ?? 'GET'} ${url}`
    const rota = rotas[chave]
    if (rota === undefined) {
      throw new Error(`Pedido não esperado no teste: ${chave}`)
    }
    const r = typeof rota === 'function' ? await rota() : rota
    return new Response(r.corpo === undefined ? null : JSON.stringify(r.corpo), {
      status: r.estado,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

export const ANA = {
  id: '3f2a9c4e-7b1d-4e8a-9c2f-1a2b3c4d5e6f',
  nome: 'Ana Exemplo',
  email: 'ana@empresa.pt',
  tipo: 'MEMBRO',
} as const

export const ADMIN = {
  id: '9b1c2d3e-4f5a-4b6c-8d7e-0f1a2b3c4d5e',
  nome: 'Administrador',
  email: 'admin@empresa.pt',
  tipo: 'ADMINISTRADOR',
} as const

export const SEM_SESSAO: RespostaFalsa = {
  estado: 401,
  corpo: { codigo: 'NAO_AUTENTICADO', mensagem: 'A sessão não é válida. Inicie sessão novamente.' },
}

export const CREDENCIAIS_INVALIDAS: RespostaFalsa = {
  estado: 401,
  corpo: { codigo: 'CREDENCIAIS_INVALIDAS', mensagem: 'Email ou palavra-passe incorretos.' },
}

/** GET da primeira página de projetos, como o painel e o ecrã de projetos o pedem. */
export const LISTAR_PROJETOS = 'GET /api/projetos?pagina=1&tamanho=20'

export const SEM_PROJETOS: RespostaFalsa = { estado: 200, corpo: { itens: [], total: 0, pagina: 1, tamanho: 20 } }

export function projeto(dados: Partial<Projeto> = {}): Projeto {
  return {
    id: 'p-' + Math.random().toString(36).slice(2),
    nome: 'Website institucional',
    descricao: null,
    arquivado: false,
    gestor: { id: ANA.id, nome: ANA.nome },
    souGestor: true,
    contagens: { A_FAZER: 0, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 },
    ...dados,
  }
}

export function paginaDe(itens: Projeto[], total = itens.length, pagina = 1): RespostaFalsa {
  return { estado: 200, corpo: { itens, total, pagina, tamanho: 20 } }
}
