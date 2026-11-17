import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Conta } from '../api/contas'
import type { RespostaFalsa } from '../test/apiFalsa'
import { ADMIN, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ADMIN } }
const LISTAR = 'GET /api/contas?pagina=1&tamanho=20'
const ATIVAS_PARA_ESCOLHER = 'GET /api/contas?estado=ATIVO&pagina=1&tamanho=100'

const ana: Conta = { id: 'ana', nome: 'Ana Exemplo', email: 'ana@empresa.pt', estado: 'ATIVO' }
const bruno: Conta = { id: 'bruno', nome: 'Bruno Exemplo', email: 'bruno@empresa.pt', estado: 'ATIVO' }
const carla: Conta = { id: 'carla', nome: 'Carla Exemplo', email: 'carla@empresa.pt', estado: 'ATIVO' }
const daniel: Conta = { id: 'daniel', nome: 'Daniel Exemplo', email: 'daniel@empresa.pt', estado: 'DESATIVADO' }

function pagina(itens: Conta[], total = itens.length, numero = 1, tamanho = 20): RespostaFalsa {
  return { estado: 200, corpo: { itens, total, pagina: numero, tamanho } }
}

function linha(nome: string) {
  return screen.getByRole('cell', { name: nome }).closest('tr')!
}

/** O corpo JSON do pedido feito a um endereço. */
function corpoDe(fetchFalso: ReturnType<typeof simularApi>, metodo: string, url: string): unknown {
  const chamada = fetchFalso.mock.calls.find(([u, init]) => u === url && init?.method === metodo)
  expect(chamada, `${metodo} ${url}`).toBeDefined()
  return JSON.parse(String(chamada![1]!.body))
}

describe('Gestão de contas (US14)', () => {
  it('o Administrador vê as contas com o estado e a ação de cada uma', async () => {
    simularApi({ ...SESSAO, [LISTAR]: pagina([ana, daniel]) })
    renderizarAplicacao('/')

    expect(await screen.findByRole('heading', { name: 'Gestão de contas' })).toBeInTheDocument()
    await screen.findByRole('cell', { name: 'Ana Exemplo' })
    expect(within(linha('Ana Exemplo')).getByText('ana@empresa.pt')).toBeInTheDocument()
    expect(within(linha('Ana Exemplo')).getByText('Ativa')).toBeInTheDocument()
    expect(within(linha('Ana Exemplo')).getByRole('button', { name: 'Desativar Ana Exemplo' })).toBeInTheDocument()
    expect(within(linha('Daniel Exemplo')).getByText('Desativada')).toBeInTheDocument()
    expect(within(linha('Daniel Exemplo')).getByRole('button', { name: 'Reativar Daniel Exemplo' })).toBeInTheDocument()
    expect(screen.getByText('2 contas')).toBeInTheDocument()
    // Não há coluna "Papel": fora dos projetos só há Membros e Administradores
    expect(screen.queryByRole('columnheader', { name: 'Papel' })).not.toBeInTheDocument()
  })

  it('filtra por estado e pesquisa pelo nome ou email', async () => {
    simularApi({
      ...SESSAO,
      [LISTAR]: pagina([ana, daniel]),
      'GET /api/contas?estado=DESATIVADO&pagina=1&tamanho=20': pagina([daniel]),
      'GET /api/contas?q=dan&estado=DESATIVADO&pagina=1&tamanho=20': pagina([daniel]),
    })
    renderizarAplicacao('/')
    await screen.findByRole('cell', { name: 'Ana Exemplo' })

    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'Desativadas')
    await waitFor(() => expect(screen.queryByRole('cell', { name: 'Ana Exemplo' })).not.toBeInTheDocument())
    expect(screen.getByText('1 conta')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Pesquisar pelo nome ou email'), 'dan')
    expect(await screen.findByRole('cell', { name: 'Daniel Exemplo' })).toBeInTheDocument()
  })

  it('desativar quem não é Gestor pede confirmação e explica o que acontece', async () => {
    let desativado = false
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR]: () => pagina([desativado ? { ...bruno, estado: 'DESATIVADO' } : bruno]),
      'GET /api/contas/bruno/projetos-geridos': { estado: 200, corpo: [] },
      'POST /api/contas/bruno/desativacao': () => {
        desativado = true
        return { estado: 200, corpo: { ...bruno, estado: 'DESATIVADO' } }
      },
    })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: 'Desativar Bruno Exemplo' }))

    const dialogo = await screen.findByRole('dialog', { name: 'Desativar a conta de Bruno Exemplo?' })
    expect(within(dialogo).getByText(/deixa de conseguir entrar na Matriz/)).toBeInTheDocument()
    expect(within(dialogo).getByText(/Deixa de ser responsável pelas tarefas por concluir/)).toBeInTheDocument()
    const botao = within(dialogo).getByRole('button', { name: 'Desativar conta' })
    await waitFor(() => expect(botao).toBeEnabled())
    expect(within(dialogo).queryByRole('combobox')).not.toBeInTheDocument()

    await userEvent.click(botao)

    expect(await screen.findByText('Conta de Bruno Exemplo desativada.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST', '/api/contas/bruno/desativacao')).toEqual({ novosGestores: [] })
    expect(await within(linha('Bruno Exemplo')).findByText('Desativada')).toBeInTheDocument()
  })

  it('desativar um Gestor obriga a escolher o novo Gestor de cada projeto', async () => {
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR]: pagina([ana, bruno, carla]),
      'GET /api/contas/ana/projetos-geridos': {
        estado: 200,
        corpo: [{ id: 'p1', nome: 'Portal do Cliente', arquivado: true }, { id: 'p2', nome: 'Website institucional', arquivado: false }],
      },
      [ATIVAS_PARA_ESCOLHER]: pagina([ana, bruno, carla], 3, 1, 100),
      'POST /api/contas/ana/desativacao': { estado: 200, corpo: { ...ana, estado: 'DESATIVADO' } },
    })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: 'Desativar Ana Exemplo' }))

    const dialogo = await screen.findByRole('dialog', { name: 'Desativar a conta de Ana Exemplo?' })
    expect(await within(dialogo).findByText('É Gestor(a) de 2 projetos. Escolha o novo Gestor de cada um:')).toBeInTheDocument()
    expect(within(dialogo).getByText('Arquivado')).toBeInTheDocument()
    const portal = within(dialogo).getByLabelText(/Portal do Cliente/)
    const website = within(dialogo).getByLabelText('Website institucional')
    // A própria pessoa não pode ser escolhida
    expect(within(portal).queryByRole('option', { name: /Ana Exemplo/ })).not.toBeInTheDocument()
    const botao = within(dialogo).getByRole('button', { name: 'Desativar conta' })
    expect(botao).toBeDisabled()

    await userEvent.selectOptions(portal, 'Bruno Exemplo (bruno@empresa.pt)')
    expect(botao).toBeDisabled()
    await userEvent.selectOptions(website, 'Carla Exemplo (carla@empresa.pt)')
    expect(botao).toBeEnabled()
    await userEvent.click(botao)

    expect(await screen.findByText('Conta de Ana Exemplo desativada.')).toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST', '/api/contas/ana/desativacao')).toEqual({
      novosGestores: [{ projetoId: 'p1', gestorId: 'bruno' }, { projetoId: 'p2', gestorId: 'carla' }],
    })
  })

  it('mostra os erros devolvidos pelo servidor e mantém o diálogo aberto', async () => {
    simularApi({
      ...SESSAO,
      [LISTAR]: pagina([ana, bruno]),
      'GET /api/contas/ana/projetos-geridos': { estado: 200, corpo: [{ id: 'p1', nome: 'Portal', arquivado: false }] },
      [ATIVAS_PARA_ESCOLHER]: pagina([ana, bruno], 2, 1, 100),
      'POST /api/contas/ana/desativacao': {
        estado: 400,
        corpo: {
          codigo: 'VALIDACAO',
          mensagem: 'Há campos inválidos.',
          detalhes: [{ campo: 'novosGestores', mensagem: 'O novo Gestor do projeto «Portal» tem de ser um Membro ativo, diferente da pessoa a desativar.' }],
        },
      },
    })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: 'Desativar Ana Exemplo' }))
    const dialogo = await screen.findByRole('dialog')
    await userEvent.selectOptions(await within(dialogo).findByLabelText('Portal'), 'Bruno Exemplo (bruno@empresa.pt)')

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Desativar conta' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('O novo Gestor do projeto «Portal» tem de ser um Membro ativo')
    expect(within(dialogo).getByRole('button', { name: 'Desativar conta' })).toBeEnabled()
  })

  it('reativar é imediato e confirma com uma mensagem', async () => {
    let reativado = false
    simularApi({
      ...SESSAO,
      [LISTAR]: () => pagina([reativado ? { ...daniel, estado: 'ATIVO' } : daniel]),
      'POST /api/contas/daniel/reativacao': () => {
        reativado = true
        return { estado: 200, corpo: { ...daniel, estado: 'ATIVO' } }
      },
    })
    renderizarAplicacao('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Reativar Daniel Exemplo' }))

    expect(await screen.findByText('Conta de Daniel Exemplo reativada.')).toBeInTheDocument()
    expect(await within(linha('Daniel Exemplo')).findByText('Ativa')).toBeInTheDocument()
  })

  it('em caso de erro ao carregar deixa tentar novamente', async () => {
    let falhar = true
    simularApi({
      ...SESSAO,
      [LISTAR]: () => (falhar ? { estado: 500, corpo: { codigo: 'ERRO_INESPERADO', mensagem: 'Erro' } } : pagina([ana])),
    })
    renderizarAplicacao('/')

    expect(await screen.findByText('Não foi possível carregar as contas.')).toBeInTheDocument()
    falhar = false
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('cell', { name: 'Ana Exemplo' })).toBeInTheDocument()
  })

  it('editar altera o nome e mostra o email só para leitura (US19 crit. 1)', async () => {
    let nome = 'Ana Exemplo'
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR]: () => pagina([{ ...ana, nome }]),
      'PATCH /api/contas/ana': () => {
        nome = 'Ana Silva'
        return { estado: 200, corpo: { ...ana, nome } }
      },
    })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: 'Editar Ana Exemplo' }))

    const dialogo = await screen.findByRole('dialog', { name: 'Editar conta' })
    expect(within(dialogo).getByLabelText('Email')).toHaveAttribute('readonly')
    const campo = within(dialogo).getByLabelText('Nome *')
    await userEvent.clear(campo)
    await userEvent.type(campo, 'A')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar' }))
    expect(within(dialogo).getByText('O nome deve ter entre 2 e 150 caracteres.')).toBeInTheDocument()

    await userEvent.clear(campo)
    await userEvent.type(campo, '  Ana Silva ')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Nome alterado para Ana Silva.')).toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'PATCH', '/api/contas/ana')).toEqual({ nome: 'Ana Silva' })
    expect(await screen.findByRole('cell', { name: 'Ana Silva' })).toBeInTheDocument()
  })

  it('repor o acesso pede uma palavra-passe temporária e só aparece nas contas ativas (US19 crit. 2)', async () => {
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR]: pagina([ana, daniel]),
      'POST /api/contas/ana/reposicao-acesso': { estado: 204 },
    })
    renderizarAplicacao('/')
    await screen.findByRole('cell', { name: 'Ana Exemplo' })
    expect(within(linha('Daniel Exemplo')).queryByRole('button', { name: /Repor o acesso/ })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Repor o acesso de Ana Exemplo' }))
    const dialogo = await screen.findByRole('dialog', { name: 'Repor o acesso de Ana Exemplo?' })
    expect(within(dialogo).getByText(/vai ter de a trocar por uma nova/)).toBeInTheDocument()
    const campo = within(dialogo).getByLabelText(/Palavra-passe temporária/)
    expect(campo).toHaveAttribute('type', 'text')

    await userEvent.type(campo, '1234567')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Repor acesso' }))
    expect(within(dialogo).getByText('A palavra-passe deve ter pelo menos 8 caracteres.')).toBeInTheDocument()

    await userEvent.type(campo, '8')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Repor acesso' }))

    expect(await screen.findByText('Acesso de Ana Exemplo reposto. Comunique-lhe a palavra-passe temporária.'))
      .toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST', '/api/contas/ana/reposicao-acesso')).toEqual({ palavraPasseTemporaria: '12345678' })
  })

  it('repor o acesso mostra o erro do servidor (conta desativada entretanto)', async () => {
    simularApi({
      ...SESSAO,
      [LISTAR]: pagina([ana]),
      'POST /api/contas/ana/reposicao-acesso': {
        estado: 409,
        corpo: { codigo: 'CONTA_DESATIVADA', mensagem: 'Esta conta está desativada. Reative-a antes de repor o acesso.' },
      },
    })
    renderizarAplicacao('/')
    await userEvent.click(await screen.findByRole('button', { name: 'Repor o acesso de Ana Exemplo' }))
    const dialogo = await screen.findByRole('dialog')
    await userEvent.type(within(dialogo).getByLabelText(/Palavra-passe temporária/), 'temporaria-1')

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Repor acesso' }))

    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Reative-a antes de repor o acesso.')
  })
})
