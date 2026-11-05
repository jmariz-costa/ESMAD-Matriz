import { describe, expect, it, vi } from 'vitest'
import { ANA, SEM_SESSAO, simularApi } from '../test/apiFalsa'
import { definirAoTerminarSessao, ErroApi, pedido } from './cliente'

describe('cliente HTTP', () => {
  it('usa sempre o caminho /api e envia JSON', async () => {
    const fetchFalso = simularApi({ 'POST /api/auth/login': { estado: 200, corpo: ANA } })

    const resposta = await pedido('/auth/login', { metodo: 'POST', corpo: { email: 'a@b.pt' } })

    expect(resposta).toEqual(ANA)
    const [, init] = fetchFalso.mock.calls[0]
    expect(init?.body).toBe('{"email":"a@b.pt"}')
    expect(init?.headers).toEqual({ 'Content-Type': 'application/json' })
    expect(init?.credentials).toBe('same-origin')
  })

  it('converte o formato único de erro em ErroApi', async () => {
    simularApi({
      'GET /api/x': { estado: 400, corpo: { codigo: 'VALIDACAO', mensagem: 'Dados inválidos.', detalhes: [{ campo: 'nome', mensagem: 'Obrigatório.' }] } },
    })

    const erro = await pedido('/x').catch((e: unknown) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro).toMatchObject({ estado: 400, codigo: 'VALIDACAO', message: 'Dados inválidos.' })
    expect((erro as ErroApi).detalhes).toEqual([{ campo: 'nome', mensagem: 'Obrigatório.' }])
  })

  it('num 401 avisa que a sessão terminou', async () => {
    simularApi({ 'GET /api/x': SEM_SESSAO })
    const ouvinte = vi.fn()
    const cancelar = definirAoTerminarSessao(ouvinte)

    await expect(pedido('/x')).rejects.toBeInstanceOf(ErroApi)
    expect(ouvinte).toHaveBeenCalledTimes(1)
    cancelar()
  })

  it('não avisa quando o 401 é esperado', async () => {
    simularApi({ 'GET /api/x': SEM_SESSAO })
    const ouvinte = vi.fn()
    const cancelar = definirAoTerminarSessao(ouvinte)

    await expect(pedido('/x', { ignorar401: true })).rejects.toBeInstanceOf(ErroApi)
    expect(ouvinte).not.toHaveBeenCalled()
    cancelar()
  })

  it('sem ligação ao servidor devolve uma mensagem compreensível', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(pedido('/x')).rejects.toMatchObject({
      codigo: 'SEM_LIGACAO',
      message: 'Não foi possível contactar o servidor. Tente novamente.',
    })
  })

  it('204 devolve undefined', async () => {
    simularApi({ 'POST /api/auth/logout': { estado: 204 } })
    await expect(pedido('/auth/logout', { metodo: 'POST' })).resolves.toBeUndefined()
  })
})
