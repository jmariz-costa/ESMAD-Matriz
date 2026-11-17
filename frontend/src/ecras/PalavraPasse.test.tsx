import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ADMIN, ANA, LISTAR_PROJETOS, SEM_PROJETOS, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const MUDAR = 'POST /api/auth/palavra-passe'

/** O corpo JSON do pedido feito a um endereço. */
function corpoDe(fetchFalso: ReturnType<typeof simularApi>, metodo: string, url: string): unknown {
  const chamada = fetchFalso.mock.calls.find(([u, init]) => u === url && init?.method === metodo)
  expect(chamada, `${metodo} ${url}`).toBeDefined()
  return JSON.parse(String(chamada![1]!.body))
}

async function preencher(dentro: HTMLElement, atual: string, nova: string, confirmacao = nova) {
  const campos = within(dentro)
  if (atual) await userEvent.type(campos.getByLabelText(/^Palavra-passe (atual|temporária)$/), atual)
  await userEvent.type(campos.getByLabelText(/^Palavra-passe nova/), nova)
  await userEvent.type(campos.getByLabelText('Repita a palavra-passe nova'), confirmacao)
}

describe('Palavra-passe temporária (US19 crit. 3, PR20)', () => {
  it('obriga a definir uma palavra-passe nova antes de usar a Matriz', async () => {
    const fetchFalso = simularApi({
      'GET /api/auth/me': { estado: 200, corpo: { ...ANA, deveMudarPalavraPasse: true } },
      [MUDAR]: { estado: 204 },
      [LISTAR_PROJETOS]: SEM_PROJETOS,
    })
    renderizarAplicacao('/projetos')

    expect(await screen.findByRole('heading', { name: 'Defina uma palavra-passe nova' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
    const formulario = screen.getByRole('heading', { name: 'Defina uma palavra-passe nova' }).closest('section')!

    await preencher(formulario, 'temporaria-123', 'curta', 'outra')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar e continuar' }))
    expect(screen.getByText('A palavra-passe deve ter pelo menos 8 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('As palavras-passe não coincidem.')).toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText(/^Palavra-passe nova/))
    await userEvent.clear(screen.getByLabelText('Repita a palavra-passe nova'))
    await preencher(formulario, '', 'a-minha-nova-1')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar e continuar' }))

    expect(await screen.findByRole('heading', { name: 'Projetos' })).toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST', '/api/auth/palavra-passe'))
      .toEqual({ palavraPasseAtual: 'temporaria-123', palavraPasseNova: 'a-minha-nova-1' })
  })

  it('deixa sair sem mudar a palavra-passe', async () => {
    simularApi({
      'GET /api/auth/me': { estado: 200, corpo: { ...ANA, deveMudarPalavraPasse: true } },
      'POST /api/auth/logout': { estado: 204 },
    })
    renderizarAplicacao('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Iniciar sessão' })).toBeInTheDocument()
  })
})

describe('Mudar a palavra-passe (US20)', () => {
  it('o nome no cabeçalho abre o diálogo e os erros do servidor aparecem junto ao campo', async () => {
    let tentativas = 0
    const fetchFalso = simularApi({
      'GET /api/auth/me': { estado: 200, corpo: ANA },
      [LISTAR_PROJETOS]: SEM_PROJETOS,
      [MUDAR]: () => (++tentativas === 1
        ? {
            estado: 400,
            corpo: {
              codigo: 'VALIDACAO', mensagem: 'O pedido tem dados inválidos.',
              detalhes: [{ campo: 'palavraPasseAtual', mensagem: 'A palavra-passe atual está errada.' }],
            },
          }
        : { estado: 204 }),
    })
    renderizarAplicacao('/')

    await userEvent.click(await screen.findByRole('button', { name: /Ana Exemplo.*mudar a palavra-passe/ }))
    const dialogo = await screen.findByRole('dialog', { name: 'Mudar a palavra-passe' })
    expect(within(dialogo).getByText(/As outras sessões abertas com a sua conta vão terminar/)).toBeInTheDocument()

    await preencher(dialogo, 'errada-123', 'a-minha-nova-1')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Mudar a palavra-passe' }))
    expect(await within(dialogo).findByText('A palavra-passe atual está errada.')).toBeInTheDocument()

    await userEvent.type(within(dialogo).getByLabelText('Palavra-passe atual'), 'certa-1234')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Mudar a palavra-passe' }))

    expect(await screen.findByText('Palavra-passe mudada.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST', '/api/auth/palavra-passe'))
      .toEqual({ palavraPasseAtual: 'errada-123', palavraPasseNova: 'a-minha-nova-1' })
  })

  it('recusa uma nova igual à atual sem chamar o servidor', async () => {
    const fetchFalso = simularApi({ 'GET /api/auth/me': { estado: 200, corpo: ANA }, [LISTAR_PROJETOS]: SEM_PROJETOS })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: /mudar a palavra-passe/ }))
    const dialogo = await screen.findByRole('dialog')

    await preencher(dialogo, 'mesma-1234', 'mesma-1234')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Mudar a palavra-passe' }))

    expect(within(dialogo).getByText('A palavra-passe nova tem de ser diferente da atual.')).toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([u]) => u === '/api/auth/palavra-passe')).toBe(false)
  })

  it('o nome do Administrador não abre o diálogo (RN21)', async () => {
    simularApi({
      'GET /api/auth/me': { estado: 200, corpo: ADMIN },
      'GET /api/contas?pagina=1&tamanho=20': { estado: 200, corpo: { itens: [], total: 0, pagina: 1, tamanho: 20 } },
    })
    renderizarAplicacao('/')

    expect(await screen.findByText('Administrador')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /mudar a palavra-passe/ })).not.toBeInTheDocument()
  })
})
