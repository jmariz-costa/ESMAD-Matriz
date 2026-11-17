import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { RespostaFalsa } from '../test/apiFalsa'
import type { MinhaTarefa } from '../api/tarefas'
import { ANA, rotaComentarios, simularApi, tarefa } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ANA } }
const POR_CONCLUIR = 'GET /api/minhas-tarefas?concluidas=false&pagina=1&tamanho=20'
const CONCLUIDAS = 'GET /api/minhas-tarefas?concluidas=true&pagina=1&tamanho=20'

function minha(dados: Partial<MinhaTarefa> = {}): MinhaTarefa {
  return {
    ...tarefa(),
    projeto: { id: 'p1', nome: 'Website institucional' },
    concluidaEm: null,
    ...dados,
  }
}

function pagina(itens: MinhaTarefa[], total = itens.length, numero = 1): RespostaFalsa {
  return { estado: 200, corpo: { itens, total, pagina: numero, tamanho: 20 } }
}

const maquete = minha({
  id: 't1', titulo: 'Criar maquete da página inicial', estado: 'EM_CURSO', prioridade: 'ALTA', prazo: '2026-11-20',
  responsaveis: [{ id: ANA.id, nome: ANA.nome }],
})
const requisitos = minha({
  id: 't2', titulo: 'Definir requisitos de autenticação', projeto: { id: 'p2', nome: 'Portal do Cliente' },
})

function linha(titulo: string) {
  return screen.getByRole('button', { name: titulo }).closest('tr')!
}

describe('As minhas tarefas (US11)', () => {
  it('o menu tem o separador e a lista mostra projeto, estado, prioridade e prazo', async () => {
    simularApi({ ...SESSAO, [POR_CONCLUIR]: pagina([maquete, requisitos]) })
    renderizarAplicacao('/minhas-tarefas')

    await screen.findByRole('button', { name: 'Criar maquete da página inicial' })
    expect(screen.getByRole('link', { name: 'As minhas tarefas' })).toHaveClass('active')
    expect(screen.getByRole('tab', { name: 'Por concluir' })).toHaveAttribute('aria-selected', 'true')

    const primeira = within(linha('Criar maquete da página inicial'))
    expect(primeira.getByRole('link', { name: 'Website institucional' })).toHaveAttribute('href', '/projetos/p1')
    expect(primeira.getByText('Em curso')).toBeInTheDocument()
    expect(primeira.getByText('Alta')).toBeInTheDocument()
    expect(primeira.getByText('20/11/2026')).toBeInTheDocument()

    const segunda = within(linha('Definir requisitos de autenticação'))
    expect(segunda.getByRole('link', { name: 'Portal do Cliente' })).toHaveAttribute('href', '/projetos/p2')
    expect(segunda.getByText('A fazer')).toBeInTheDocument()
    expect(segunda.getByText('Sem prazo')).toBeInTheDocument()
    expect(screen.getByText('2 tarefas por concluir')).toBeInTheDocument()
  })

  it('sem tarefas por concluir mostra a mensagem (crit. 4)', async () => {
    simularApi({ ...SESSAO, [POR_CONCLUIR]: pagina([]) })
    renderizarAplicacao('/minhas-tarefas')

    expect(await screen.findByText('Não tem tarefas por concluir.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('o separador Concluídas mostra o histórico com a data de conclusão (crit. 5)', async () => {
    const paleta = minha({ id: 't3', titulo: 'Definir paleta de cores', estado: 'CONCLUIDA', concluidaEm: '2026-11-10T15:30:00Z' })
    const arranque = minha({ id: 't4', titulo: 'Reunião de arranque', estado: 'CONCLUIDA', concluidaEm: null })
    simularApi({ ...SESSAO, [POR_CONCLUIR]: pagina([maquete]), [CONCLUIDAS]: pagina([paleta, arranque]) })
    renderizarAplicacao('/minhas-tarefas')
    await screen.findByRole('button', { name: 'Criar maquete da página inicial' })

    await userEvent.click(screen.getByRole('tab', { name: 'Concluídas' }))

    await screen.findByRole('button', { name: 'Definir paleta de cores' })
    expect(screen.getByRole('tab', { name: 'Concluídas' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('columnheader', { name: 'Concluída em' })).toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Estado' })).not.toBeInTheDocument()
    expect(within(linha('Definir paleta de cores')).getByText(/10\/11\/2026/)).toBeInTheDocument()
    expect(within(linha('Reunião de arranque')).getByText('Sem data')).toBeInTheDocument()
    expect(screen.getByText('2 tarefas concluídas')).toBeInTheDocument()
  })

  it('abre no separador do URL e mostra a mensagem quando não há concluídas', async () => {
    simularApi({ ...SESSAO, [CONCLUIDAS]: pagina([]) })
    renderizarAplicacao('/minhas-tarefas?vista=concluidas')

    expect(await screen.findByText('Ainda não tem tarefas concluídas.')).toBeInTheDocument()
  })

  it('clicar no título abre o detalhe da tarefa, sem a opção de eliminar', async () => {
    simularApi({
      ...SESSAO,
      [POR_CONCLUIR]: pagina([maquete]),
      'GET /api/tarefas/t1': { estado: 200, corpo: maquete },
      [rotaComentarios('t1')]: { estado: 200, corpo: { itens: [], total: 0, pagina: 1, tamanho: 20 } },
    })
    renderizarAplicacao('/minhas-tarefas')

    await userEvent.click(await screen.findByRole('button', { name: 'Criar maquete da página inicial' }))

    const dialogo = await screen.findByRole('dialog', { name: 'Tarefa' })
    expect(await within(dialogo).findByDisplayValue('Criar maquete da página inicial')).toBeInTheDocument()
    expect(within(dialogo).queryByRole('button', { name: /Eliminar/ })).not.toBeInTheDocument()
  })

  it('pagina a lista e guarda a página no URL', async () => {
    const seguinte = minha({ id: 't9', titulo: 'Tarefa da página 2' })
    simularApi({
      ...SESSAO,
      [POR_CONCLUIR]: pagina([maquete], 21),
      'GET /api/minhas-tarefas?concluidas=false&pagina=2&tamanho=20': pagina([seguinte], 21, 2),
    })
    renderizarAplicacao('/minhas-tarefas')
    await screen.findByText('Página 1 de 2')

    await userEvent.click(screen.getByRole('button', { name: 'Página seguinte' }))

    expect(await screen.findByRole('button', { name: 'Tarefa da página 2' })).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
  })

  it('em caso de erro deixa tentar novamente', async () => {
    let falhar = true
    simularApi({
      ...SESSAO,
      [POR_CONCLUIR]: () => (falhar
        ? { estado: 500, corpo: { codigo: 'ERRO_INESPERADO', mensagem: 'Erro' } }
        : pagina([maquete])),
    })
    renderizarAplicacao('/minhas-tarefas')

    expect(await screen.findByText('Não foi possível carregar as tarefas.')).toBeInTheDocument()
    falhar = false
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('button', { name: 'Criar maquete da página inicial' })).toBeInTheDocument()
  })
})
