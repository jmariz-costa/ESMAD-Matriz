import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, LISTAR_PROJETOS, paginaDe, projeto, SEM_PROJETOS, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ANA } }

describe('Painel inicial (US18, US10)', () => {
  it('mostra cada projeto com a percentagem e as contagens por estado', async () => {
    simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: paginaDe([
        projeto({ id: 'p1', nome: 'Website institucional', contagens: { A_FAZER: 2, EM_CURSO: 2, EM_REVISAO: 1, CONCLUIDA: 4 } }),
        projeto({ id: 'p2', nome: 'Portal do Cliente', souGestor: false, contagens: { A_FAZER: 3, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 } }),
      ]),
    })
    renderizarAplicacao('/')

    const website = (await screen.findByRole('link', { name: 'Website institucional' })).closest('li')!
    expect(within(website).getByText('Gestor')).toBeInTheDocument()
    expect(within(website).getByText('44% concluído · 9 tarefas')).toBeInTheDocument()
    expect(within(website).getByText('Em revisão').nextSibling).toHaveTextContent('1')
    expect(within(website).getByText('Concluída').nextSibling).toHaveTextContent('4')

    const portal = screen.getByRole('link', { name: 'Portal do Cliente' }).closest('li')!
    expect(within(portal).queryByText('Gestor')).not.toBeInTheDocument()
    expect(within(portal).getByText('0% concluído · 3 tarefas')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Portal do Cliente' })).toHaveAttribute('href', '/projetos/p2')
  })

  it('sem projetos mostra a mensagem e a opção de criar', async () => {
    simularApi({ ...SESSAO, [LISTAR_PROJETOS]: SEM_PROJETOS })
    renderizarAplicacao('/')

    expect(await screen.findByText('Ainda não faz parte de nenhum projeto.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '+ Novo projeto' })).toBeInTheDocument()
  })

  it('em caso de erro permite tentar novamente', async () => {
    let tentativas = 0
    simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: () => (++tentativas === 1
        ? { estado: 500 }
        : paginaDe([projeto({ nome: 'Recuperado' })])),
    })
    renderizarAplicacao('/')

    expect(await screen.findByText('Não foi possível carregar os projetos.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('link', { name: 'Recuperado' })).toBeInTheDocument()
  })

  it('com mais projetos do que cabem no painel mostra a ligação para todos', async () => {
    simularApi({ ...SESSAO, [LISTAR_PROJETOS]: paginaDe([projeto()], 25) })
    renderizarAplicacao('/')

    expect(await screen.findByRole('link', { name: 'Ver todos os projetos (25)' })).toHaveAttribute('href', '/projetos')
  })

  it('criar um projeto atualiza o painel', async () => {
    let criado = false
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: () => (criado ? paginaDe([projeto({ nome: 'Projeto novo' })]) : SEM_PROJETOS),
      'POST /api/projetos': () => {
        criado = true
        return { estado: 201, corpo: projeto({ nome: 'Projeto novo' }) }
      },
    })
    renderizarAplicacao('/')

    await userEvent.click(await screen.findByRole('button', { name: '+ Novo projeto' }))
    const dialogo = screen.getByRole('dialog', { name: 'Novo projeto' })
    await userEvent.type(within(dialogo).getByLabelText('Nome *'), '  Projeto novo ')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Criar' }))

    expect(await screen.findByRole('link', { name: 'Projeto novo' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    const pedidoCriar = fetchFalso.mock.calls.find(([url, init]) => url === '/api/projetos' && init?.method === 'POST')!
    expect(JSON.parse(pedidoCriar[1]!.body as string)).toEqual({ nome: 'Projeto novo', descricao: null })
  })
})
