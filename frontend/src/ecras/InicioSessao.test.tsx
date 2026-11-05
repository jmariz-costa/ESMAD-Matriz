import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, CREDENCIAIS_INVALIDAS, LISTAR_PROJETOS, SEM_PROJETOS, SEM_SESSAO, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

async function abrirInicioSessao() {
  renderizarAplicacao('/entrar')
  return screen.findByRole('heading', { name: 'Iniciar sessão' })
}

describe('Início de sessão', () => {
  it('valida os campos antes de enviar', async () => {
    const fetchFalso = simularApi({ 'GET /api/auth/me': SEM_SESSAO })
    await abrirInicioSessao()

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByText('Indique o email.')).toBeInTheDocument()
    expect(screen.getByText('Indique a palavra-passe.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchFalso).toHaveBeenCalledTimes(1) // só o GET /auth/me inicial
  })

  it('recusa um email com formato inválido', async () => {
    simularApi({ 'GET /api/auth/me': SEM_SESSAO })
    await abrirInicioSessao()

    await userEvent.type(screen.getByLabelText('Email'), 'ana')
    await userEvent.type(screen.getByLabelText('Palavra-passe'), 'segredo')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByText('Indique um email válido.')).toBeInTheDocument()
  })

  it('desativa o botão enquanto o pedido está em curso', async () => {
    let responder: (r: { estado: number; corpo: unknown }) => void = () => {}
    simularApi({
      'GET /api/auth/me': SEM_SESSAO,
      'POST /api/auth/login': () => new Promise((resolve) => { responder = resolve }),
      [LISTAR_PROJETOS]: SEM_PROJETOS,
    })
    await abrirInicioSessao()

    await userEvent.type(screen.getByLabelText('Email'), 'ana@empresa.pt')
    await userEvent.type(screen.getByLabelText('Palavra-passe'), 'segredo')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(screen.getByRole('button', { name: 'A entrar…' })).toBeDisabled()
    responder({ estado: 200, corpo: ANA })
    expect(await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })).toBeInTheDocument()
  })

  it('mostra a mensagem genérica e mantém o email', async () => {
    simularApi({ 'GET /api/auth/me': SEM_SESSAO, 'POST /api/auth/login': CREDENCIAIS_INVALIDAS })
    await abrirInicioSessao()

    await userEvent.type(screen.getByLabelText('Email'), 'ana@empresa.pt')
    await userEvent.type(screen.getByLabelText('Palavra-passe'), 'errada')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email ou palavra-passe incorretos.')
    expect(screen.getByLabelText('Email')).toHaveValue('ana@empresa.pt')
    expect(screen.getByLabelText('Palavra-passe')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled()
  })

  it('envia o email sem espaços nas pontas', async () => {
    const fetchFalso = simularApi({
      'GET /api/auth/me': SEM_SESSAO,
      'POST /api/auth/login': { estado: 200, corpo: ANA },
      [LISTAR_PROJETOS]: SEM_PROJETOS,
    })
    await abrirInicioSessao()

    await userEvent.type(screen.getByLabelText('Email'), '  ana@empresa.pt ')
    await userEvent.type(screen.getByLabelText('Palavra-passe'), 'segredo')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await screen.findByRole('heading', { name: 'Olá, Ana Exemplo' })
    const [, init] = fetchFalso.mock.calls[1]
    expect(JSON.parse(init?.body as string)).toEqual({ email: 'ana@empresa.pt', palavraPasse: 'segredo' })
  })
})
