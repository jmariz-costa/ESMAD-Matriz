import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { ANA, LISTAR_PROJETOS, paginaDe, projeto, simularApi } from '../test/apiFalsa'
import { renderizarAplicacao } from '../test/renderizar'

const SESSAO = { 'GET /api/auth/me': { estado: 200, corpo: ANA } }

const portalRh = projeto({
  id: 'rh', nome: 'Portal interno de RH', contagens: { A_FAZER: 2, EM_CURSO: 2, EM_REVISAO: 1, CONCLUIDA: 7 },
})
const portalCliente = projeto({
  id: 'cliente', nome: 'Portal do Cliente', souGestor: false, gestor: { id: 'b', nome: 'Bruno Exemplo' },
  contagens: { A_FAZER: 3, EM_CURSO: 0, EM_REVISAO: 0, CONCLUIDA: 0 },
})

function linha(nome: string) {
  return screen.getByRole('link', { name: nome }).closest('tr')!
}

describe('Ecrã de projetos (US16, US17)', () => {
  it('mostra a tabela e as ações só nos projetos de que sou Gestor', async () => {
    simularApi({ ...SESSAO, [LISTAR_PROJETOS]: paginaDe([portalCliente, portalRh]) })
    renderizarAplicacao('/projetos')

    await screen.findByRole('link', { name: 'Portal do Cliente' })
    expect(within(linha('Portal do Cliente')).getByText('Bruno Exemplo')).toBeInTheDocument()
    expect(within(linha('Portal do Cliente')).getByText('0%')).toBeInTheDocument()
    expect(within(linha('Portal do Cliente')).queryByRole('button')).not.toBeInTheDocument()
    expect(within(linha('Portal interno de RH')).getByText('58%')).toBeInTheDocument()
    expect(within(linha('Portal interno de RH')).getByText('12')).toBeInTheDocument()
    expect(within(linha('Portal interno de RH')).getByRole('button', { name: 'Editar Portal interno de RH' })).toBeInTheDocument()
    expect(screen.getByText('2 projetos encontrados')).toBeInTheDocument()
  })

  it('pesquisa enquanto se escreve e guarda a pesquisa no URL', async () => {
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: paginaDe([portalCliente, portalRh]),
      'GET /api/projetos?q=cliente&pagina=1&tamanho=20': paginaDe([portalCliente]),
    })
    renderizarAplicacao('/projetos')
    await screen.findByRole('link', { name: 'Portal interno de RH' })

    await userEvent.type(screen.getByLabelText('Pesquisar pelo nome'), 'cliente')

    await waitFor(() => expect(screen.queryByRole('link', { name: 'Portal interno de RH' })).not.toBeInTheDocument())
    expect(screen.getByText('1 projeto encontrado')).toBeInTheDocument()
    // Um único pedido depois de escrever, e não um por tecla
    expect(fetchFalso.mock.calls.filter(([url]) => String(url).includes('q='))).toHaveLength(1)
  })

  it('abre com a pesquisa e a página do URL', async () => {
    simularApi({ ...SESSAO, 'GET /api/projetos?q=portal&pagina=2&tamanho=20': paginaDe([portalRh], 21, 2) })
    renderizarAplicacao('/projetos?q=portal&pagina=2')

    expect(await screen.findByRole('link', { name: 'Portal interno de RH' })).toBeInTheDocument()
    expect(screen.getByLabelText('Pesquisar pelo nome')).toHaveValue('portal')
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
  })

  it('sem resultados mostra "Nenhum projeto encontrado."', async () => {
    simularApi({ ...SESSAO, 'GET /api/projetos?q=xyz&pagina=1&tamanho=20': paginaDe([]) })
    renderizarAplicacao('/projetos?q=xyz')

    expect(await screen.findByText('Nenhum projeto encontrado.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('muda de página', async () => {
    simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: paginaDe([portalCliente], 25),
      'GET /api/projetos?pagina=2&tamanho=20': paginaDe([portalRh], 25, 2),
    })
    renderizarAplicacao('/projetos')
    await screen.findByText('Página 1 de 2')
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Página seguinte' }))

    expect(await screen.findByRole('link', { name: 'Portal interno de RH' })).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(screen.getByText('25 projetos encontrados')).toBeInTheDocument()
  })

  it('eliminar pede confirmação e explica o que é apagado', async () => {
    let eliminado = false
    const fetchFalso = simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: () => paginaDe(eliminado ? [portalCliente] : [portalCliente, portalRh]),
      'DELETE /api/projetos/rh': () => {
        eliminado = true
        return { estado: 204 }
      },
    })
    renderizarAplicacao('/projetos')
    await screen.findByRole('link', { name: 'Portal interno de RH' })

    await userEvent.click(screen.getByRole('button', { name: 'Eliminar Portal interno de RH' }))
    const dialogo = screen.getByRole('dialog', { name: 'Eliminar o projeto "Portal interno de RH"?' })
    expect(dialogo).toHaveTextContent('Serão apagadas também as 12 tarefas do projeto, com os seus responsáveis, comentários e histórico.')

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }))
    expect(fetchFalso).not.toHaveBeenCalledWith('/api/projetos/rh', expect.anything())

    await userEvent.click(screen.getByRole('button', { name: 'Eliminar Portal interno de RH' }))
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar definitivamente' }))

    await waitFor(() => expect(screen.queryByRole('link', { name: 'Portal interno de RH' })).not.toBeInTheDocument())
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('editar atualiza a tabela', async () => {
    let editado = false
    simularApi({
      ...SESSAO,
      [LISTAR_PROJETOS]: () => paginaDe([editado ? { ...portalRh, nome: 'RH 2.0' } : portalRh]),
      'PATCH /api/projetos/rh': () => {
        editado = true
        return { estado: 200, corpo: { ...portalRh, nome: 'RH 2.0' } }
      },
    })
    renderizarAplicacao('/projetos')

    await userEvent.click(await screen.findByRole('button', { name: 'Editar Portal interno de RH' }))
    await userEvent.clear(screen.getByLabelText('Nome *'))
    await userEvent.type(screen.getByLabelText('Nome *'), 'RH 2.0')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('link', { name: 'RH 2.0' })).toBeInTheDocument()
  })
})
