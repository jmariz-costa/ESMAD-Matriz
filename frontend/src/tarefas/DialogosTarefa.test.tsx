import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import type { Contagens } from '../api/projetos'
import type { Tarefa } from '../api/tarefas'
import { ANA, projeto, quadro, rotaColuna, simularApi, tarefa, type RespostaFalsa } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const COM_TAREFAS: Contagens = { A_FAZER: 1, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 }

function base(dados: Parameters<typeof projeto>[0] = {}): Record<string, RespostaFalsa> {
  return {
    'GET /api/auth/me': { estado: 200, corpo: ANA },
    'GET /api/projetos/p1': { estado: 200, corpo: projeto({ id: 'p1', contagens: COM_TAREFAS, ...dados }) },
    'GET /api/projetos/p1/membros': { estado: 200, corpo: [] },
  }
}

const corpoDe = (fetchFalso: ReturnType<typeof simularApi>, metodo: string) => {
  const chamada = fetchFalso.mock.calls.filter(([, init]) => init?.method === metodo).at(-1)!
  return JSON.parse(chamada[1]!.body as string) as unknown
}

const dialogo = () => screen.getByRole('dialog')

/** Campo do diálogo (os filtros do quadro também têm "Estado" e "Prioridade"). */
const campo = (rotulo: string) => within(dialogo()).getByLabelText(rotulo)

async function abrir(titulo: string) {
  await userEvent.click(await screen.findByRole('button', { name: titulo }))
  await within(dialogo()).findByLabelText('Título *')
}

describe('Criar tarefa (US05 crit. 1 a 3)', () => {
  it('cria a tarefa e ela aparece em "A fazer" sem recarregar a página', async () => {
    let criada = false
    const nova = tarefa({ id: 't9', titulo: 'Preparar apresentação', prioridade: 'ALTA', prazo: '2026-11-15' })
    const fetchFalso = simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200, corpo: { itens: criada ? [nova] : [], total: criada ? 1 : 0, pagina: 1, tamanho: 20 },
      }),
      'POST /api/projetos/p1/tarefas': () => {
        criada = true
        return { estado: 201, corpo: nova }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: '+ Nova tarefa' }))
    expect(within(dialogo()).queryByLabelText('Estado')).not.toBeInTheDocument()
    await userEvent.type(campo('Título *'), '  Preparar apresentação ')
    await userEvent.selectOptions(campo('Prioridade'), 'Alta')
    await userEvent.type(campo('Prazo'), '2026-11-15')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    expect(await screen.findByRole('button', { name: 'Preparar apresentação' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'POST')).toEqual({
      titulo: 'Preparar apresentação', descricao: null, prazo: '2026-11-15', prioridade: 'ALTA',
    })
  })

  it('recusa um título só com espaços sem enviar o pedido', async () => {
    const fetchFalso = simularApi({ ...base(), ...quadro('p1') })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: '+ Nova tarefa' }))
    await userEvent.type(campo('Título *'), '    ')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    expect(screen.getByText('O título é obrigatório.')).toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'POST')).toBe(false)
  })

  it('se a API devolver erro, o formulário mantém os dados escritos', async () => {
    let resposta: RespostaFalsa = {
      estado: 400,
      corpo: { codigo: 'VALIDACAO', mensagem: 'O pedido tem dados inválidos.',
        detalhes: [{ campo: 'descricao', mensagem: 'A descrição não pode ter mais de 2000 caracteres.' }] },
    }
    simularApi({ ...base(), ...quadro('p1'), 'POST /api/projetos/p1/tarefas': () => resposta })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: '+ Nova tarefa' }))
    await userEvent.type(campo('Título *'), 'Rever contrato')
    await userEvent.type(campo('Descrição'), 'Cláusulas')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    expect(await screen.findByText('A descrição não pode ter mais de 2000 caracteres.')).toBeInTheDocument()
    expect(campo('Título *')).toHaveValue('Rever contrato')
    expect(campo('Descrição')).toHaveValue('Cláusulas')

    resposta = { estado: 409, corpo: { codigo: 'PROJETO_ARQUIVADO', mensagem: 'O projeto está arquivado e não pode ser alterado.' } }
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))
    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('O projeto está arquivado')
    expect(campo('Título *')).toHaveValue('Rever contrato')
    expect(screen.getByRole('button', { name: 'Criar' })).toBeEnabled()
  })
})

describe('Detalhe e edição da tarefa (US05, US06)', () => {
  const t1 = tarefa({ id: 't1', titulo: 'Criar maquete', descricao: 'Em papel.', prioridade: 'MEDIA', versao: 2 })

  it('abrir um cartão mostra a tarefa; guardar envia só os campos alterados, incluindo o estado', async () => {
    let alterada: Tarefa | null = null
    const fetchFalso = simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200, corpo: { itens: alterada ? [] : [t1], total: alterada ? 0 : 1, pagina: 1, tamanho: 20 },
      }),
      [rotaColuna('p1', 'EM_REVISAO')]: () => ({
        estado: 200, corpo: { itens: alterada ? [alterada] : [], total: alterada ? 1 : 0, pagina: 1, tamanho: 20 },
      }),
      'GET /api/tarefas/t1': { estado: 200, corpo: t1 },
      'PATCH /api/tarefas/t1': () => {
        alterada = { ...t1, titulo: 'Criar maquete final', estado: 'EM_REVISAO', versao: 3 }
        return { estado: 200, corpo: alterada }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    expect(campo('Título *')).toHaveValue('Criar maquete')
    expect(campo('Descrição')).toHaveValue('Em papel.')
    expect(campo('Estado')).toHaveValue('A_FAZER')
    expect(campo('Prioridade')).toHaveValue('MEDIA')

    await userEvent.type(campo('Título *'), ' final')
    await userEvent.selectOptions(campo('Estado'), 'Em revisão')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await within(screen.getByRole('region', { name: 'Em revisão' }))
      .findByRole('button', { name: 'Criar maquete final' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(corpoDe(fetchFalso, 'PATCH')).toEqual({ versao: 2, titulo: 'Criar maquete final', estado: 'EM_REVISAO' })
  })

  it('se outra pessoa alterou a tarefa, pede para recarregar e mantém o que foi escrito', async () => {
    const recente = { ...t1, descricao: 'Em papel e digital.', versao: 3 }
    let versaoAtual = 2
    const fetchFalso = simularApi({
      ...base(),
      ...quadro('p1', [t1]),
      'GET /api/tarefas/t1': () => ({ estado: 200, corpo: versaoAtual === 2 ? t1 : recente }),
      'PATCH /api/tarefas/t1': () => {
        if (versaoAtual === 2) {
          versaoAtual = 3
          return { estado: 409, corpo: { codigo: 'VERSAO_DESATUALIZADA', mensagem: 'A tarefa foi alterada por outra pessoa. Recarregue-a antes de guardar.' } }
        }
        return { estado: 200, corpo: { ...recente, titulo: 'Maquete v2', versao: 4 } }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    await userEvent.clear(campo('Título *'))
    await userEvent.type(campo('Título *'), 'Maquete v2')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('Recarregue-a para ver as alterações')
    expect(campo('Título *')).toHaveValue('Maquete v2')
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Recarregar tarefa' }))
    await waitFor(() => expect(campo('Descrição')).toHaveValue('Em papel e digital.'))
    expect(campo('Título *')).toHaveValue('Maquete v2')

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(corpoDe(fetchFalso, 'PATCH')).toEqual({ versao: 3, titulo: 'Maquete v2' })
  })

  it('guardar sem alterações fecha sem enviar pedidos', async () => {
    const fetchFalso = simularApi({ ...base(), ...quadro('p1', [t1]), 'GET /api/tarefas/t1': { estado: 200, corpo: t1 } })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'PATCH')).toBe(false)
  })

  it('tarefa que já não existe mostra uma mensagem', async () => {
    simularApi({
      ...base(),
      ...quadro('p1', [t1]),
      'GET /api/tarefas/t1': { estado: 404, corpo: { codigo: 'NAO_ENCONTRADO', mensagem: 'O recurso pedido não existe.' } },
    })
    renderizarAplicacao('/projetos/p1')

    await userEvent.click(await screen.findByRole('button', { name: 'Criar maquete' }))
    expect(await within(dialogo()).findByRole('alert')).toHaveTextContent('Esta tarefa já não existe.')
  })
})

describe('Eliminar tarefa (US09 crit. 1 a 3)', () => {
  const t1 = tarefa({ id: 't1', titulo: 'Criar maquete' })

  it('o Gestor confirma e a tarefa desaparece do quadro', async () => {
    let eliminada = false
    simularApi({
      ...base(),
      ...quadro('p1'),
      [rotaColuna('p1', 'A_FAZER')]: () => ({
        estado: 200, corpo: { itens: eliminada ? [] : [t1], total: eliminada ? 0 : 1, pagina: 1, tamanho: 20 },
      }),
      'GET /api/tarefas/t1': { estado: 200, corpo: t1 },
      'DELETE /api/tarefas/t1': () => {
        eliminada = true
        return { estado: 204 }
      },
    })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Eliminar' }))
    expect(within(dialogo()).getByRole('group', { name: 'Confirmar eliminação' }))
      .toHaveTextContent('Eliminar a tarefa Criar maquete? Esta ação é definitiva')
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar definitivamente' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Criar maquete' })).not.toBeInTheDocument())
  })

  it('cancelar a confirmação não elimina', async () => {
    const fetchFalso = simularApi({ ...base(), ...quadro('p1', [t1]), 'GET /api/tarefas/t1': { estado: 200, corpo: t1 } })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Eliminar' }))
    await userEvent.click(within(dialogo()).getByRole('button', { name: 'Cancelar' }))

    expect(within(dialogo()).queryByRole('group', { name: 'Confirmar eliminação' })).not.toBeInTheDocument()
    expect(fetchFalso.mock.calls.some(([, init]) => init?.method === 'DELETE')).toBe(false)
  })

  it('um Membro que não é Gestor não tem a opção de eliminar', async () => {
    simularApi({ ...base({ souGestor: false }), ...quadro('p1', [t1]), 'GET /api/tarefas/t1': { estado: 200, corpo: t1 } })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    expect(within(dialogo()).queryByRole('button', { name: 'Eliminar' })).not.toBeInTheDocument()
    expect(within(dialogo()).getByRole('button', { name: 'Guardar' })).toBeInTheDocument()
  })
})

describe('Projeto arquivado (RN15)', () => {
  it('sem "+ Nova tarefa" e com o detalhe só de leitura', async () => {
    const t1 = tarefa({ id: 't1', titulo: 'Criar maquete' })
    simularApi({ ...base({ arquivado: true }), ...quadro('p1', [t1]), 'GET /api/tarefas/t1': { estado: 200, corpo: t1 } })
    renderizarAplicacao('/projetos/p1')

    await abrir('Criar maquete')
    expect(screen.queryByRole('button', { name: '+ Nova tarefa' })).not.toBeInTheDocument()
    expect(campo('Título *')).toBeDisabled()
    expect(campo('Estado')).toBeDisabled()
    expect(within(dialogo()).queryByRole('button', { name: 'Guardar' })).not.toBeInTheDocument()
    expect(within(dialogo()).queryByRole('button', { name: 'Eliminar' })).not.toBeInTheDocument()
    expect(within(dialogo()).getByRole('button', { name: 'Fechar' })).toBeInTheDocument()
  })
})
