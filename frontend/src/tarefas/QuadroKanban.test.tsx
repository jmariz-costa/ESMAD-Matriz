import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Contagens } from '../api/projetos'
import { ANA, projeto, quadro, rotaColuna, simularApi, tarefa, type RespostaFalsa } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const COM_TAREFAS: Contagens = { A_FAZER: 2, EM_CURSO: 1, EM_REVISAO: 0, CONCLUIDA: 1 }

function base(dados: Parameters<typeof projeto>[0] = {}): Record<string, RespostaFalsa> {
  return {
    'GET /api/auth/me': { estado: 200, corpo: ANA },
    'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1', contagens: COM_TAREFAS, ...dados }) },
    'GET /api/projetos/p1/membros': { estado: 200, corpo: [] },
  }
}

const coluna = (nome: string) => screen.getByRole('region', { name: nome })

/** Nomes das colunas visíveis no quadro, pela ordem. */
const colunasVisiveis = () => within(screen.getByRole('region', { name: 'Quadro de tarefas' }))
  .queryAllByRole('region').map((r) => r.getAttribute('aria-label'))

describe('Quadro Kanban (US07)', () => {
  it('mostra as 4 colunas pela ordem, com o total e os cartões de cada uma', async () => {
    simularApi({
      ...base(),
      ...quadro('p1', [
        tarefa({ titulo: 'Definir mapa do site', prioridade: 'ALTA', prazo: '2026-11-10' }),
        tarefa({ titulo: 'Escolher fotografias', responsaveis: [{ id: 'b', nome: 'Bruno Exemplo' }] }),
        tarefa({ titulo: 'Criar maquete', estado: 'EM_CURSO',
          responsaveis: [{ id: 'a', nome: 'Ana Exemplo' }, { id: 'b', nome: 'Bruno Exemplo' }] }),
        tarefa({ titulo: 'Reunião de arranque', estado: 'CONCLUIDA', prioridade: 'MEDIA' }),
      ]),
    })
    renderizarAplicacao('/projetos/p1')

    await screen.findByText('Definir mapa do site')
    expect(colunasVisiveis()).toEqual(['A fazer', 'Em curso', 'Em revisão', 'Concluída'])

    const aFazer = coluna('A fazer')
    expect(within(aFazer).getByRole('heading')).toHaveTextContent('A fazer 2')
    const primeiro = within(aFazer).getAllByRole('listitem')[0]
    expect(primeiro).toHaveTextContent('Definir mapa do site')
    expect(primeiro).toHaveTextContent('Sem responsável')
    expect(primeiro).toHaveTextContent('Prioridade: Alta')
    expect(primeiro).toHaveTextContent('Prazo: 10/11/2026')
    expect(within(aFazer).getByText('Bruno Exemplo')).toBeInTheDocument()

    expect(within(coluna('Em curso')).getByText('Ana Exemplo, Bruno Exemplo')).toBeInTheDocument()
    expect(within(coluna('Em revisão')).queryAllByRole('listitem')).toHaveLength(0)
    expect(within(coluna('Concluída')).getByText('Média')).toBeInTheDocument()
  })

  it('"Ver mais" carrega a página seguinte da coluna', async () => {
    const primeiras = Array.from({ length: 20 }, (_, i) => tarefa({ titulo: `Tarefa ${i + 1}` }))
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: { estado: 200, corpo: { itens: primeiras, total: 21, pagina: 1, tamanho: 20 } },
      [rotaColuna('p1', 'A_FAZER', 2)]: {
        estado: 200, corpo: { itens: [tarefa({ titulo: 'Tarefa 21' })], total: 21, pagina: 2, tamanho: 20 },
      },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Ver mais (1)' }))

    expect(await within(coluna('A fazer')).findByText('Tarefa 21')).toBeInTheDocument()
    expect(within(coluna('A fazer')).getAllByRole('listitem')).toHaveLength(21)
    expect(screen.queryByRole('button', { name: /Ver mais/ })).not.toBeInTheDocument()
  })

  it('filtrar por estado mostra só essa coluna; a prioridade aplica-se a todas; "Limpar filtros" volta a mostrar tudo', async () => {
    simularApi({
      ...base(),
      ...quadro('p1', [tarefa({ titulo: 'Sem filtros' })]),
      ...quadro('p1', [tarefa({ titulo: 'Só sem prioridade' })], 'SEM_PRIORIDADE'),
      [rotaColuna('p1', 'EM_CURSO', 1, 'SEM_PRIORIDADE')]: {
        estado: 200, corpo: { itens: [tarefa({ titulo: 'Em curso sem prioridade', estado: 'EM_CURSO' })], total: 1, pagina: 1, tamanho: 20 },
      },
    })
    renderizarAplicacao('/projetos/p1')
    await screen.findByText('Sem filtros')

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Prioridade' }), 'Sem prioridade')
    expect(await screen.findByText('Só sem prioridade')).toBeInTheDocument()
    expect(colunasVisiveis()).toHaveLength(4)

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Estado' }), 'Em curso')
    expect(await screen.findByText('Em curso sem prioridade')).toBeInTheDocument()
    expect(colunasVisiveis()).toEqual(['Em curso'])

    await userEvent.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(await screen.findByText('Sem filtros')).toBeInTheDocument()
    expect(colunasVisiveis()).toHaveLength(4)
    expect(screen.queryByRole('button', { name: 'Limpar filtros' })).not.toBeInTheDocument()
  })

  it('arrastar um cartão para outra coluna muda o estado e atualiza o quadro e as contagens', async () => {
    const t = tarefa({ id: 't1', titulo: 'Criar maquete', versao: 3 })
    let movida = false
    let projetoPedido = 0
    const fetchFalso = simularApi({
      ...base(),
      'GET /api/projetos/p1': () => {
        projetoPedido++
        return { estado: 200, corpo: projeto({ id: 'p1', contagens: COM_TAREFAS }) }
      },
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200, corpo: { itens: movida ? [] : [t], total: movida ? 0 : 1, pagina: 1, tamanho: 20 },
      }),
      [rotaColuna('p1', 'EM_CURSO')]: () => ({
        estado: 200,
        corpo: { itens: movida ? [{ ...t, estado: 'EM_CURSO', versao: 4 }] : [], total: movida ? 1 : 0, pagina: 1, tamanho: 20 },
      }),
      'PATCH /api/tarefas/t1': () => {
        movida = true
        return { estado: 200, corpo: { ...t, estado: 'EM_CURSO', versao: 4 } }
      },
    })
    renderizarAplicacao('/projetos/p1')

    const cartao = (await screen.findByText('Criar maquete')).closest('li')!
    expect(cartao).toHaveAttribute('draggable', 'true')
    fireEvent.dragStart(cartao)
    fireEvent.dragOver(coluna('Em curso'))
    expect(within(coluna('Em curso')).getByText('Largar aqui')).toBeInTheDocument()
    fireEvent.drop(coluna('Em curso'))

    expect(await within(coluna('Em curso')).findByText('Criar maquete')).toBeInTheDocument()
    expect(within(coluna('A fazer')).queryByText('Criar maquete')).not.toBeInTheDocument()
    const patch = fetchFalso.mock.calls.find(([, init]) => init?.method === 'PATCH')!
    expect(JSON.parse(patch[1]!.body as string)).toEqual({ versao: 3, estado: 'EM_CURSO' })
    await waitFor(() => expect(projetoPedido).toBe(2))
  })

  it('se outra pessoa alterou a tarefa, avisa e atualiza o quadro', async () => {
    const t = tarefa({ id: 't1', titulo: 'Criar maquete' })
    let pedidosAFazer = 0
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => {
        pedidosAFazer++
        return { estado: 200, corpo: { itens: [t], total: 1, pagina: 1, tamanho: 20 } }
      },
      'PATCH /api/tarefas/t1': {
        estado: 409,
        corpo: { codigo: 'VERSAO_DESATUALIZADA', mensagem: 'A tarefa foi alterada por outra pessoa. Recarregue-a antes de guardar.' },
      },
    })
    renderizarAplicacao('/projetos/p1')

    const cartao = (await screen.findByText('Criar maquete')).closest('li')!
    fireEvent.dragStart(cartao)
    fireEvent.dragOver(coluna('Concluída'))
    fireEvent.drop(coluna('Concluída'))

    expect(await screen.findByRole('alert')).toHaveTextContent('A tarefa foi alterada por outra pessoa entretanto. O quadro foi atualizado')
    await waitFor(() => expect(pedidosAFazer).toBe(2))
  })

  it('largar na mesma coluna não faz pedidos', async () => {
    const fetchFalso = simularApi({ ...base(), ...quadro('p1', [tarefa({ id: 't1', titulo: 'Criar maquete' })]) })
    renderizarAplicacao('/projetos/p1')

    const cartao = (await screen.findByText('Criar maquete')).closest('li')!
    fireEvent.dragStart(cartao)
    fireEvent.dragOver(coluna('A fazer'))
    fireEvent.drop(coluna('A fazer'))

    expect(within(coluna('A fazer')).queryByText('Largar aqui')).not.toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'PATCH')).toBe(false)
  })

  it('num projeto arquivado os cartões não se arrastam', async () => {
    simularApi({ ...base({ arquivado: true }), ...quadro('p1', [tarefa({ titulo: 'Criar maquete' })]) })
    renderizarAplicacao('/projetos/p1')

    const cartao = (await screen.findByText('Criar maquete')).closest('li')!
    expect(cartao).toHaveAttribute('draggable', 'false')
    expect(within(cartao).queryByText('⠿')).not.toBeInTheDocument()
  })

  it('projeto sem tarefas mostra uma mensagem', async () => {
    simularApi({ ...base({ contagens: { A_FAZER: 0, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 } }), ...quadro('p1') })
    renderizarAplicacao('/projetos/p1')

    expect(await screen.findByText('Este projeto ainda não tem tarefas.')).toBeInTheDocument()
  })

  it('erro ao carregar o quadro permite tentar novamente', async () => {
    let falhar = true
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => falhar
        ? { estado: 500 }
        : { estado: 200, corpo: { itens: [tarefa({ titulo: 'Já carregou' })], total: 1, pagina: 1, tamanho: 20 } },
    })
    renderizarAplicacao('/projetos/p1')

    expect(await screen.findByText('Não foi possível carregar o quadro.')).toBeInTheDocument()
    falhar = false
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('Já carregou')).toBeInTheDocument()
  })
})
