import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Comentario } from '../api/comentarios'
import type { Membro } from '../api/membros'
import type { Tarefa } from '../api/tarefas'
import { ANA, projeto, quadro, rotaColuna, rotaComentarios, simularApi, tarefa, type RespostaFalsa } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const BRUNO = { id: 'b', nome: 'Bruno Exemplo' }
const CARLA = { id: 'c', nome: 'Carla Exemplo' }
const DANIEL = { id: 'd', nome: 'Daniel Exemplo' }

const MEMBROS: Membro[] = [
  { id: ANA.id, nome: ANA.nome, email: ANA.email, ativo: true, gestor: true },
  { ...BRUNO, email: 'bruno@empresa.pt', ativo: true, gestor: false },
  { ...CARLA, email: 'carla@empresa.pt', ativo: false, gestor: false },
  { ...DANIEL, email: 'daniel@empresa.pt', ativo: true, gestor: false },
]

const SEM_COMENTARIOS: RespostaFalsa = { estado: 200, corpo: { itens: [], total: 0, pagina: 1, tamanho: 20 } }

function base(t: Tarefa, dados: Parameters<typeof projeto>[0] = {}): Record<string, RespostaFalsa> {
  return {
    'GET /api/auth/me': { estado: 200, corpo: ANA },
    'GET /api/projetos/p1': {
      estado: 200, corpo: projeto({ id: 'p1', contagens: { A_FAZER: 1, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 }, ...dados }),
    },
    'GET /api/projetos/p1/membros': { estado: 200, corpo: MEMBROS },
    ...quadro('p1', [t]),
    'GET /api/tarefas/t1': { estado: 200, corpo: t },
    [rotaComentarios('t1')]: SEM_COMENTARIOS,
  }
}

const dialogo = () => screen.getByRole('dialog')
const responsaveis = () => within(dialogo()).getByRole('group', { name: 'Responsáveis' })
const comentarios = () => within(dialogo()).getByRole('region', { name: /Comentários/ })

async function abrir(titulo = 'Criar maquete') {
  await userEvent.click(await screen.findByRole('button', { name: titulo }))
  await within(dialogo()).findByRole('group', { name: 'Responsáveis' })
}

function comentario(dados: Partial<Comentario>): Comentario {
  return {
    id: 'c-' + Math.random().toString(36).slice(2),
    tarefaId: 't1',
    autor: BRUNO,
    texto: 'Olá',
    criadoEm: '2026-11-12T12:00:00Z',
    editadoEm: null,
    ...dados,
  }
}

describe('Responsáveis da tarefa (Exercício 6; US05 crit. 4 e 7)', () => {
  it('"+ Adicionar" mostra só os membros ativos que ainda não são responsáveis e atribui', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete', responsaveis: [{ id: ANA.id, nome: ANA.nome }], versao: 1 })
    const fetchFalso = simularApi({
      ...base(t1),
      'POST /api/tarefas/t1/responsaveis': { estado: 201, corpo: { ...t1, responsaveis: [{ id: ANA.id, nome: ANA.nome }, BRUNO], versao: 2 } },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(within(responsaveis()).getByText('Ana Exemplo')).toBeInTheDocument()
    await userEvent.click(within(responsaveis()).getByRole('button', { name: '+ Adicionar' }))

    const lista = await within(responsaveis()).findByRole('list', { name: 'Membros que podem ser responsáveis' })
    expect(within(lista).getAllByRole('button').map((b) => b.textContent)).toEqual(['Bruno Exemplo', 'Daniel Exemplo'])
    await userEvent.click(within(lista).getByRole('button', { name: 'Bruno Exemplo' }))

    expect(await within(responsaveis()).findByRole('button', { name: 'Remover Bruno Exemplo' })).toBeInTheDocument()
    expect(within(responsaveis()).queryByRole('list', { name: 'Membros que podem ser responsáveis' })).not.toBeInTheDocument()
    const post = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST')!
    expect(JSON.parse(post[1]!.body as string)).toEqual({ utilizadorId: 'b' })
  })

  it('✕ remove só essa atribuição', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete', responsaveis: [BRUNO, DANIEL], versao: 2 })
    let removido = false
    simularApi({
      ...base(t1),
      'GET /api/tarefas/t1': () => ({ estado: 200, corpo: removido ? { ...t1, responsaveis: [DANIEL], versao: 3 } : t1 }),
      'DELETE /api/tarefas/t1/responsaveis/b': () => {
        removido = true
        return { estado: 204 }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    await userEvent.click(within(responsaveis()).getByRole('button', { name: 'Remover Bruno Exemplo' }))

    await waitFor(() => expect(within(responsaveis()).queryByText('Bruno Exemplo')).not.toBeInTheDocument())
    expect(within(responsaveis()).getByText('Daniel Exemplo')).toBeInTheDocument()
  })

  it('sem responsáveis mostra "Sem responsável"; um erro do servidor aparece na secção', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete' })
    simularApi({
      ...base(t1),
      'POST /api/tarefas/t1/responsaveis': {
        estado: 409, corpo: { codigo: 'JA_EXISTE', mensagem: 'Esta pessoa já é responsável pela tarefa.' },
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(within(responsaveis()).getByText('Sem responsável')).toBeInTheDocument()
    await userEvent.click(within(responsaveis()).getByRole('button', { name: '+ Adicionar' }))
    await userEvent.click(await within(responsaveis()).findByRole('button', { name: 'Daniel Exemplo' }))

    expect(await within(responsaveis()).findByRole('alert')).toHaveTextContent('Esta pessoa já é responsável pela tarefa.')
  })

  it('depois de atribuir, "Guardar" usa a versão nova e mantém o que foi escrito', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete', versao: 4 })
    const fetchFalso = simularApi({
      ...base(t1),
      'POST /api/tarefas/t1/responsaveis': { estado: 201, corpo: { ...t1, responsaveis: [BRUNO], versao: 5 } },
      'PATCH /api/tarefas/t1': { estado: 200, corpo: { ...t1, titulo: 'Maquete final', responsaveis: [BRUNO], versao: 6 } },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    await userEvent.clear(within(dialogo()).getByLabelText('Título *'))
    await userEvent.type(within(dialogo()).getByLabelText('Título *'), 'Maquete final')
    await userEvent.click(within(responsaveis()).getByRole('button', { name: '+ Adicionar' }))
    await userEvent.click(await within(responsaveis()).findByRole('button', { name: 'Bruno Exemplo' }))
    await within(responsaveis()).findByRole('button', { name: 'Remover Bruno Exemplo' })

    expect(within(dialogo()).getByLabelText('Título *')).toHaveValue('Maquete final')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    const patch = fetchFalso.mock.calls.find(([, init]) => init?.method === 'PATCH')!
    expect(JSON.parse(patch[1]!.body as string)).toEqual({ versao: 5, titulo: 'Maquete final' })
  })

  it('o cartão do quadro mostra o responsável atribuído sem recarregar a página', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete' })
    let atribuido = false
    const comBruno = { ...t1, responsaveis: [BRUNO], versao: 1 }
    simularApi({
      ...base(t1),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200, corpo: { itens: [atribuido ? comBruno : t1], total: 1, pagina: 1, tamanho: 20 },
      }),
      'POST /api/tarefas/t1/responsaveis': () => {
        atribuido = true
        return { estado: 201, corpo: comBruno }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    await userEvent.click(within(responsaveis()).getByRole('button', { name: '+ Adicionar' }))
    await userEvent.click(await within(responsaveis()).findByRole('button', { name: 'Bruno Exemplo' }))
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Cancelar' }))

    const cartao = (await screen.findByRole('button', { name: 'Criar maquete' })).closest('li')!
    await waitFor(() => expect(cartao).toHaveTextContent('Bruno Exemplo'))
  })

  it('num projeto arquivado não há ✕ nem "+ Adicionar"', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete', responsaveis: [BRUNO] })
    simularApi(base(t1, { arquivado: true }))
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(within(responsaveis()).getByText('Bruno Exemplo')).toBeInTheDocument()
    expect(within(responsaveis()).queryByRole('button')).not.toBeInTheDocument()
  })
})

describe('Comentários da tarefa (Exercício 6; US12 crit. 1 e 2)', () => {
  const t1 = tarefa({ id: 't1', titulo: 'Criar maquete' })

  it('mostra os comentários com o autor e a data, e "Ver mais comentários"', async () => {
    const primeiros = Array.from({ length: 20 }, (_, i) => comentario({ texto: `Comentário ${i + 1}` }))
    simularApi({
      ...base(t1),
      [rotaComentarios('t1')]: { estado: 200, corpo: { itens: primeiros, total: 21, pagina: 1, tamanho: 20 } },
      [rotaComentarios('t1', 2)]: {
        estado: 200, corpo: { itens: [comentario({ texto: 'O último', autor: { id: ANA.id, nome: ANA.nome } })], total: 21, pagina: 2, tamanho: 20 },
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(await within(comentarios()).findByText('Comentário 1')).toBeInTheDocument()
    expect(within(comentarios()).getByRole('heading')).toHaveTextContent('Comentários (21)')
    const primeiro = within(comentarios()).getAllByRole('listitem')[0]
    expect(primeiro).toHaveTextContent('Bruno Exemplo')
    expect(primeiro).toHaveTextContent('12/11/2026')

    await userEvent.click(within(comentarios()).getByRole('button', { name: 'Ver mais comentários (1)' }))
    expect(await within(comentarios()).findByText('O último')).toBeInTheDocument()
    expect(within(comentarios()).queryByRole('button', { name: /Ver mais/ })).not.toBeInTheDocument()
  })

  it('publicar envia só o texto e o comentário aparece no fim', async () => {
    const fetchFalso = simularApi({
      ...base(t1),
      [rotaComentarios('t1')]: { estado: 200, corpo: { itens: [comentario({ texto: 'Primeiro' })], total: 1, pagina: 1, tamanho: 20 } },
      'POST /api/tarefas/t1/comentarios': {
        estado: 201, corpo: comentario({ texto: 'Já enviei os slides.', autor: { id: ANA.id, nome: ANA.nome } }),
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    await within(comentarios()).findByText('Primeiro')
    await userEvent.type(within(comentarios()).getByLabelText('Escrever comentário'), '  Já enviei os slides. ')
    await userEvent.click(within(comentarios()).getByRole('button', { name: 'Publicar' }))

    expect(await within(comentarios()).findByText('Já enviei os slides.')).toBeInTheDocument()
    expect(within(comentarios()).getAllByRole('listitem').map((li) => li.querySelector('p')?.textContent))
      .toEqual(['Primeiro', 'Já enviei os slides.'])
    expect(within(comentarios()).getByRole('heading')).toHaveTextContent('Comentários (2)')
    expect(within(comentarios()).getByLabelText('Escrever comentário')).toHaveValue('')
    const post = fetchFalso.mock.calls.find(([, init]) => init?.method === 'POST')!
    expect(JSON.parse(post[1]!.body as string)).toEqual({ texto: 'Já enviei os slides.' })
  })

  it('recusa um comentário só com espaços e mantém o texto se o servidor recusar', async () => {
    const fetchFalso = simularApi({
      ...base(t1),
      'POST /api/tarefas/t1/comentarios': {
        estado: 409, corpo: { codigo: 'PROJETO_ARQUIVADO', mensagem: 'O projeto está arquivado e não pode ser alterado.' },
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    const caixa = await within(comentarios()).findByLabelText('Escrever comentário')
    await userEvent.type(caixa, '   ')
    await userEvent.click(within(comentarios()).getByRole('button', { name: 'Publicar' }))
    expect(within(comentarios()).getByText('O comentário não pode estar vazio.')).toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)

    await userEvent.clear(caixa)
    await userEvent.type(caixa, 'Olá equipa')
    await userEvent.click(within(comentarios()).getByRole('button', { name: 'Publicar' }))
    expect(await within(comentarios()).findByRole('alert')).toHaveTextContent('O projeto está arquivado')
    expect(caixa).toHaveValue('Olá equipa')
  })

  it('num projeto arquivado os comentários leem-se, mas não há caixa para publicar', async () => {
    simularApi({
      ...base(t1, { arquivado: true }),
      [rotaComentarios('t1')]: { estado: 200, corpo: { itens: [comentario({ texto: 'Antigo' })], total: 1, pagina: 1, tamanho: 20 } },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(await within(comentarios()).findByText('Antigo')).toBeInTheDocument()
    expect(within(comentarios()).queryByLabelText('Escrever comentário')).not.toBeInTheDocument()
  })

  it('tarefa sem comentários mostra uma mensagem', async () => {
    simularApi(base(t1))
    renderizarAplicacao('/projetos/p1')

    await abrir()
    expect(await within(comentarios()).findByText('Ainda não há comentários.')).toBeInTheDocument()
  })
})
