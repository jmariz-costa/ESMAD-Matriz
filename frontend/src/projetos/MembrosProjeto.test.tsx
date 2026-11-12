import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Membro } from '../api/membros'
import { ANA, projeto, quadro, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ANA } }

const anaGestora: Membro = { id: ANA.id, nome: 'Ana Exemplo', email: 'ana@empresa.pt', ativo: true, gestor: true }
const bruno: Membro = { id: 'b', nome: 'Bruno Exemplo', email: 'bruno@empresa.pt', ativo: true, gestor: false }
const carla: Membro = { id: 'c', nome: 'Carla Exemplo', email: 'carla@empresa.pt', ativo: false, gestor: false }

function paginaUtilizadores(itens: { id: string; nome: string; email: string }[]) {
  return { estado: 200, corpo: { itens, total: itens.length, pagina: 1, tamanho: 10 } }
}

async function abrirDetalhe() {
  renderizarAplicacao('/projetos/p1')
  return screen.findByRole('heading', { name: /^Membros \(\d+\)$/ })
}

/** O item da lista de membros (o nome de quem tem sessão aparece também no cabeçalho). */
function itemDe(nome: string) {
  const seccao = screen.getByRole('region', { name: /^Membros/ })
  return within(seccao).getByText(nome).closest('li')!
}

describe('Membros do projeto (US04)', () => {
  it('qualquer membro vê a lista, sem opções de gestão', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1', souGestor: false }) },
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [anaGestora, bruno, carla] },
    })
    await abrirDetalhe()

    expect(screen.getByRole('heading', { name: 'Membros (3)' })).toBeInTheDocument()
    expect(within(itemDe('Ana Exemplo')).getByText('Gestor')).toBeInTheDocument()
    expect(within(itemDe('Carla Exemplo')).getByText('Conta desativada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '+ Adicionar membro' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Remover/ })).not.toBeInTheDocument()
  })

  it('o Gestor pode remover os outros membros, mas não a si próprio', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1' }) },
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [anaGestora, bruno] },
    })
    await abrirDetalhe()

    expect(screen.getByRole('button', { name: 'Remover Bruno Exemplo' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remover Ana Exemplo' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '+ Adicionar membro' })).toBeInTheDocument()
  })

  it('num projeto arquivado ninguém adiciona nem remove', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1', arquivado: true }) },
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [anaGestora, bruno] },
    })
    await abrirDetalhe()

    expect(screen.queryByRole('button', { name: '+ Adicionar membro' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Remover Bruno Exemplo' })).not.toBeInTheDocument()
  })

  it('remover pede confirmação e atualiza a lista', async () => {
    let removido = false
    const fetchFalso = simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1' }) },
      'GET /api/projetos/p1/membros': () => ({ estado: 200, corpo: removido ? [anaGestora] : [anaGestora, bruno] }),
      'DELETE /api/projetos/p1/membros/b': () => {
        removido = true
        return { estado: 204 }
      },
    })
    await abrirDetalhe()

    await userEvent.click(screen.getByRole('button', { name: 'Remover Bruno Exemplo' }))
    const dialogo = screen.getByRole('dialog', { name: 'Remover Bruno Exemplo do projeto?' })
    expect(dialogo).toHaveTextContent('Deixa de ver o projeto e de ser responsável pelas tarefas não concluídas.')

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))
    expect(fetchFalso).not.toHaveBeenCalledWith('/api/projetos/p1/membros/b', expect.anything())

    await userEvent.click(screen.getByRole('button', { name: 'Remover Bruno Exemplo' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Remover' }))

    expect(await screen.findByRole('heading', { name: 'Membros (1)' })).toBeInTheDocument()
    expect(screen.queryByText('Bruno Exemplo')).not.toBeInTheDocument()
  })

  it('adicionar pesquisa Membros ativos, esconde quem já é membro e atualiza a lista', async () => {
    let adicionado = false
    const fetchFalso = simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1' }) },
      'GET /api/projetos/p1/membros': () => ({ estado: 200, corpo: adicionado ? [anaGestora, bruno] : [anaGestora] }),
      'GET /api/utilizadores?pagina=1&tamanho=10': paginaUtilizadores([anaGestora, bruno, { id: 'd', nome: 'Daniel Exemplo', email: 'daniel@empresa.pt' }]),
      'GET /api/utilizadores?q=bru&pagina=1&tamanho=10': paginaUtilizadores([bruno]),
      'POST /api/projetos/p1/membros': () => {
        adicionado = true
        return { estado: 201, corpo: bruno }
      },
    })
    await abrirDetalhe()

    await userEvent.click(screen.getByRole('button', { name: '+ Adicionar membro' }))
    const dialogo = screen.getByRole('dialog', { name: 'Adicionar membro' })
    expect(await within(dialogo).findByText('Daniel Exemplo')).toBeInTheDocument()
    expect(within(dialogo).queryByText('Ana Exemplo')).not.toBeInTheDocument()

    await userEvent.type(within(dialogo).getByLabelText('Pesquisar pelo nome ou email'), 'bru')
    await waitFor(() => expect(within(dialogo).queryByText('Daniel Exemplo')).not.toBeInTheDocument())
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Adicionar Bruno Exemplo' }))

    expect(await within(dialogo).findByRole('status')).toHaveTextContent('Bruno Exemplo foi adicionado ao projeto.')
    expect(await screen.findByRole('heading', { name: 'Membros (2)' })).toBeInTheDocument()
    expect(within(dialogo).getByText('Nenhum Membro encontrado.')).toBeInTheDocument()
    const pedido = fetchFalso.mock.calls.find(([url, init]) => url === '/api/projetos/p1/membros' && init?.method === 'POST')!
    expect(JSON.parse(pedido[1]!.body as string)).toEqual({ utilizadorId: 'b' })
  })

  it('mostra o erro do servidor ao adicionar', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1' }) },
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [anaGestora] },
      'GET /api/utilizadores?pagina=1&tamanho=10': paginaUtilizadores([bruno]),
      'POST /api/projetos/p1/membros': {
        estado: 409, corpo: { codigo: 'JA_EXISTE', mensagem: 'Esta pessoa já é membro do projeto.' },
      },
    })
    await abrirDetalhe()

    await userEvent.click(screen.getByRole('button', { name: '+ Adicionar membro' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Adicionar Bruno Exemplo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Esta pessoa já é membro do projeto.')
  })
})
