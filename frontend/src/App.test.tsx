import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { pedido } from './api/cliente'
import { ADMIN, ANA, LISTAR_PROJETOS, SEM_PROJETOS, SEM_SESSAO, simularApi } from './test/apiFalsa'
import { renderizarAplicacao } from './test/renderizar'

describe('Aplicação', () => {
  it('sem sessão encaminha para o início de sessão, sem mensagem', async () => {
    simularApi({ 'GET /api/auth/me': SEM_SESSAO })
    renderizarAplicacao('/')

    expect(await screen.findByRole('heading', { name: 'Iniciar sessão' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('com sessão de Membro mostra a página inicial e o cabeçalho', async () => {
    simularApi({ 'GET /api/auth/me': { estado: 200, corpo: ANA }, [LISTAR_PROJETOS]: SEM_PROJETOS })
    renderizarAplicacao('/')

    expect(await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Início' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Projetos' })).toBeInTheDocument()
    expect(screen.getByText('AE')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument()
  })

  it('o Administrador só vê a gestão de contas', async () => {
    simularApi({ 'GET /api/auth/me': { estado: 200, corpo: ADMIN } })
    renderizarAplicacao('/')

    expect(await screen.findByRole('heading', { name: 'Gestão de contas' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })

  it('o Administrador não acede aos ecrãs de projetos', async () => {
    simularApi({ 'GET /api/auth/me': { estado: 200, corpo: ADMIN } })
    renderizarAplicacao('/projetos')

    expect(await screen.findByRole('heading', { name: 'Gestão de contas' })).toBeInTheDocument()
  })

  it('depois de entrar volta ao ecrã de origem', async () => {
    simularApi({ 'GET /api/auth/me': SEM_SESSAO, 'POST /api/auth/login': { estado: 200, corpo: ANA }, [LISTAR_PROJETOS]: SEM_PROJETOS })
    renderizarAplicacao('/')
    await screen.findByRole('heading', { name: 'Iniciar sessão' })

    await userEvent.type(screen.getByLabelText('Email'), 'ana@empresa.pt')
    await userEvent.type(screen.getByLabelText('Palavra-passe'), 'segredo')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })).toBeInTheDocument()
  })

  it('sair termina a sessão e volta ao início de sessão', async () => {
    const fetchFalso = simularApi({
      'GET /api/auth/me': { estado: 200, corpo: ANA },
      [LISTAR_PROJETOS]: SEM_PROJETOS,
      'POST /api/auth/logout': { estado: 204 },
    })
    renderizarAplicacao('/')
    await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })

    await userEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sessão' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(fetchFalso).toHaveBeenCalledWith('/api/auth/logout', expect.objectContaining({ method: 'POST' }))
  })

  it('quando a sessão deixa de ser válida mostra a mensagem no início de sessão', async () => {
    simularApi({ 'GET /api/auth/me': { estado: 200, corpo: ANA }, [LISTAR_PROJETOS]: SEM_PROJETOS, 'GET /api/projetos': SEM_SESSAO })
    renderizarAplicacao('/')
    await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })

    await act(async () => {
      await pedido('/projetos').catch(() => {})
    })

    expect(await screen.findByRole('heading', { name: 'Iniciar sessão' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('A sua sessão terminou. Inicie sessão novamente.')
  })
})
