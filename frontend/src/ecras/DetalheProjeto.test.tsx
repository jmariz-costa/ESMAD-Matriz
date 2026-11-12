import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, LISTAR_PROJETOS, paginaDe, projeto, quadro, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ANA } }

describe('Detalhe do projeto (US18 crit. 2)', () => {
  it('mostra os dados do projeto', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [] },
      'GET /api/projetos/p1': {
        estado: 200,
        corpo: projeto({ id: 'p1', nome: 'Portal do Cliente', descricao: 'Área reservada.', souGestor: false,
          gestor: { id: 'b', nome: 'Bruno Exemplo' }, contagens: { A_FAZER: 1, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 3 } }),
      },
    })
    renderizarAplicacao('/projetos/p1')

    expect(await screen.findByRole('heading', { name: 'Portal do Cliente' })).toBeInTheDocument()
    expect(screen.getByText('Área reservada.')).toBeInTheDocument()
    expect(screen.getByText('Gestor: Bruno Exemplo')).toBeInTheDocument()
    expect(screen.getByText('75% concluído · 4 tarefas')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument()
  })

  it('projeto inexistente ou sem acesso mostra uma mensagem', async () => {
    simularApi({
      ...SESSAO,
      'GET /api/projetos/nao-existe': { estado: 404, corpo: { codigo: 'NAO_ENCONTRADO', mensagem: 'O recurso pedido não existe.' } },
      'GET /api/projetos/de-outro': { estado: 403, corpo: { codigo: 'SEM_PERMISSAO', mensagem: 'Não tem permissão para esta ação.' } },
    })
    renderizarAplicacao('/projetos/nao-existe')
    expect(await screen.findByText('Este projeto não existe.')).toBeInTheDocument()

    renderizarAplicacao('/projetos/de-outro')
    expect(await screen.findByText('Não tem acesso a este projeto.')).toBeInTheDocument()
  })

  it('o Gestor elimina e volta à lista de projetos', async () => {
    simularApi({
      ...SESSAO,
      ...quadro('p1'),
      'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1', nome: 'Portal do Cliente' }) },
      'GET /api/projetos/p1/membros': { estado: 200, corpo: [] },
      'DELETE /api/projetos/p1': { estado: 204 },
      [LISTAR_PROJETOS]: paginaDe([]),
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar' }))
    expect(screen.getByRole('dialog')).toHaveTextContent('Esta ação é definitiva. O projeto não tem tarefas.')
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar definitivamente' }))

    expect(await screen.findByRole('heading', { name: 'Projetos' })).toBeInTheDocument()
  })
})
