import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Membro } from '../api/membros'
import {
  ANA, LISTAR_PROJETOS, paginaDe, projeto, quadro, rotaColuna, rotaComentarios, simularApi, tarefa, type RespostaFalsa,
} from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const BRUNO: Membro = { id: 'b', nome: 'Bruno Exemplo', email: 'bruno@empresa.pt', ativo: true, gestor: false }
const MEMBROS: Membro[] = [{ id: ANA.id, nome: ANA.nome, email: ANA.email, ativo: true, gestor: true }, BRUNO]

function base(): Record<string, RespostaFalsa> {
  return {
    'GET /api/auth/me': { estado: 200, corpo: ANA },
    'GET /api/projetos/p1': {
      estado: 200, corpo: projeto({ id: 'p1', nome: 'Portal', contagens: { A_FAZER: 1, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 } }),
    },
    'GET /api/projetos/p1/membros': { estado: 200, corpo: MEMBROS },
    [rotaComentarios('t1')]: { estado: 200, corpo: { itens: [], total: 0, pagina: 1, tamanho: 20 } },
  }
}

const notificacao = () => screen.getAllByRole('status').find((e) => e.classList.contains('notificacao-zona'))!

describe('Mensagens de sucesso (Exercício 7)', () => {
  it('criar uma tarefa mostra "Tarefa criada."', async () => {
    simularApi({
      ...base(),
      ...quadro('p1', [tarefa({ id: 't1', titulo: 'Existente' })]),
      'POST /api/projetos/p1/tarefas': { estado: 201, corpo: tarefa({ titulo: 'Nova' }) },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: '+ Nova tarefa' }))
    await userEvent.type(within(screen.getByRole('dialog')).getByLabelText('Título *'), 'Nova')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    await waitFor(() => expect(notificacao()).toHaveTextContent('Tarefa criada.'))
  })

  it('guardar uma tarefa mostra "Alterações guardadas."', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Rever' })
    simularApi({
      ...base(),
      ...quadro('p1', [t1]),
      'GET /api/tarefas/t1': { estado: 200, corpo: t1 },
      'PATCH /api/tarefas/t1': { estado: 200, corpo: { ...t1, titulo: 'Rever tudo', versao: 1 } },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Rever' }))
    const titulo = await within(screen.getByRole('dialog')).findByLabelText('Título *')
    await userEvent.type(titulo, ' tudo')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    await waitFor(() => expect(notificacao()).toHaveTextContent('Alterações guardadas.'))
  })

  it('eliminar o projeto mostra "Projeto eliminado." já na lista de projetos', async () => {
    simularApi({
      ...base(),
      ...quadro('p1'),
      'DELETE /api/projetos/p1': { estado: 204 },
      [LISTAR_PROJETOS]: paginaDe([]),
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar definitivamente' }))

    expect(await screen.findByRole('heading', { name: 'Projetos' })).toBeInTheDocument()
    expect(notificacao()).toHaveTextContent('Projeto eliminado.')
  })
})

describe('Sincronização do quadro (Exercício 7)', () => {
  it('remover um membro atualiza o quadro, que deixa de o mostrar como responsável (F2)', async () => {
    let removido = false
    const comBruno = tarefa({ id: 't1', titulo: 'Rever', responsaveis: [{ id: 'b', nome: 'Bruno Exemplo' }] })
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200,
        corpo: { itens: [removido ? { ...comBruno, responsaveis: [] } : comBruno], total: 1, pagina: 1, tamanho: 20 },
      }),
      'GET /api/projetos/p1/membros': () => ({ estado: 200, corpo: removido ? MEMBROS.slice(0, 1) : MEMBROS }),
      'DELETE /api/projetos/p1/membros/b': () => {
        removido = true
        return { estado: 204 }
      },
    })
    renderizarAplicacao('/projetos/p1')

    const cartao = (await screen.findByRole('button', { name: 'Rever' })).closest('li')!
    expect(cartao).toHaveTextContent('Bruno Exemplo')
    await userEvent.click(screen.getByRole('button', { name: 'Remover Bruno Exemplo' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Remover' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Rever' }).closest('li')).toHaveTextContent('Sem responsável'))
    expect(notificacao()).toHaveTextContent('Bruno Exemplo foi removido do projeto.')
  })

  it('abrir uma tarefa que outra pessoa eliminou tira-a do quadro (F3)', async () => {
    let pedidosAFazer = 0
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => {
        pedidosAFazer++
        const itens = pedidosAFazer === 1 ? [tarefa({ id: 't1', titulo: 'Já eliminada' })] : []
        return { estado: 200, corpo: { itens, total: itens.length, pagina: 1, tamanho: 20 } }
      },
      'GET /api/tarefas/t1': { estado: 404, corpo: { codigo: 'NAO_ENCONTRADO', mensagem: 'O recurso pedido não existe.' } },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Já eliminada' }))

    expect(await within(screen.getByRole('dialog')).findByRole('alert')).toHaveTextContent('Esta tarefa já não existe.')
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Já eliminada' })).not.toBeInTheDocument())
  })
})

describe('Erros da API (Exercício 8, caso 09)', () => {
  it('um erro ao criar, guardar ou eliminar não mostra mensagem de sucesso e mantém o formulário', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Rever' })
    const erro = (codigo: string, mensagem: string): RespostaFalsa => ({ estado: 409, corpo: { codigo, mensagem } })
    simularApi({
      ...base(),
      ...quadro('p1', [t1]),
      'POST /api/projetos/p1/tarefas': erro('PROJETO_ARQUIVADO', 'O projeto está arquivado e não pode ser alterado.'),
      'GET /api/tarefas/t1': { estado: 200, corpo: t1 },
      'PATCH /api/tarefas/t1': erro('VERSAO_DESATUALIZADA', 'A tarefa foi alterada por outra pessoa. Recarregue-a antes de guardar.'),
      'DELETE /api/tarefas/t1': erro('PROJETO_ARQUIVADO', 'O projeto está arquivado e não pode ser alterado.'),
      'DELETE /api/projetos/p1': { estado: 500 },
    })
    renderizarAplicacao('/projetos/p1')
    const dialogo = () => screen.getByRole('dialog')

    // Criar
    await userEvent.click(await screen.findByRole('button', { name: '+ Nova tarefa' }))
    await userEvent.type(within(dialogo()).getByLabelText('Título *'), 'Nova tarefa')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Criar' }))
    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('O projeto está arquivado')
    expect(within(dialogo()).getByLabelText('Título *')).toHaveValue('Nova tarefa')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Cancelar' }))

    // Guardar
    await userEvent.click(screen.getByRole('button', { name: 'Rever' }))
    const titulo = await within(dialogo()).findByLabelText('Título *')
    await userEvent.type(titulo, ' tudo')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Guardar' }))
    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('Recarregue-a')
    expect(titulo).toHaveValue('Rever tudo')

    // Eliminar a tarefa
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Eliminar' }))
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Eliminar definitivamente' }))
    await waitFor(() => expect(within(dialogo()).getAllByRole('alert').at(-1)).toHaveTextContent('O projeto está arquivado'))
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Cancelar' }))

    // Eliminar o projeto
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Eliminar definitivamente' }))
    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('Ocorreu um erro inesperado')
    expect(screen.getByRole('heading', { name: 'Portal' })).toBeInTheDocument()

    expect(notificacao()).toBeEmptyDOMElement()
  })
})
