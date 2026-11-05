import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { projeto, simularApi } from '../test/apiFalsa'
import { FormularioProjeto } from './FormularioProjeto'

function abrir(props: Partial<Parameters<typeof FormularioProjeto>[0]> = {}) {
  const aoGuardar = vi.fn()
  const aoFechar = vi.fn()
  render(<FormularioProjeto aoGuardar={aoGuardar} aoFechar={aoFechar} {...props} />)
  return { aoGuardar, aoFechar }
}

describe('Formulário de projeto (nota 9, RN23)', () => {
  it('valida o nome antes de enviar', async () => {
    const fetchFalso = simularApi({})
    abrir()

    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))
    expect(screen.getByText('O nome é obrigatório.')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Nome *'), '  PR  ')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))
    expect(screen.getByText('O nome deve ter entre 3 e 100 caracteres.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome *')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchFalso).not.toHaveBeenCalled()
  })

  it('valida o tamanho da descrição', async () => {
    simularApi({})
    abrir()

    await userEvent.type(screen.getByLabelText('Nome *'), 'Projeto')
    await userEvent.click(screen.getByLabelText('Descrição'))
    await userEvent.paste('d'.repeat(1001))
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    expect(screen.getByText('A descrição não pode ter mais de 1000 caracteres.')).toBeInTheDocument()
  })

  it('mostra os erros do servidor junto ao campo e mantém o texto', async () => {
    simularApi({
      'POST /api/projetos': {
        estado: 400,
        corpo: { codigo: 'VALIDACAO', mensagem: 'O pedido tem dados inválidos.', detalhes: [{ campo: 'nome', mensagem: 'Nome recusado pelo servidor.' }] },
      },
    })
    const { aoGuardar } = abrir()

    await userEvent.type(screen.getByLabelText('Nome *'), 'Projeto')
    await userEvent.type(screen.getByLabelText('Descrição'), 'Texto escrito')
    await userEvent.click(screen.getByRole('button', { name: 'Criar' }))

    expect(await screen.findByText('Nome recusado pelo servidor.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome *')).toHaveValue('Projeto')
    expect(screen.getByLabelText('Descrição')).toHaveValue('Texto escrito')
    expect(aoGuardar).not.toHaveBeenCalled()
  })

  it('editar preenche os campos e envia PATCH', async () => {
    const existente = projeto({ id: 'p1', nome: 'Portal', descricao: 'Antiga' })
    const fetchFalso = simularApi({ 'PATCH /api/projetos/p1': { estado: 200, corpo: { ...existente, nome: 'Portal 2.0' } } })
    const { aoGuardar } = abrir({ projeto: existente })

    expect(screen.getByRole('dialog', { name: 'Editar projeto' })).toBeInTheDocument()
    expect(screen.getByLabelText('Descrição')).toHaveValue('Antiga')
    await userEvent.clear(screen.getByLabelText('Nome *'))
    await userEvent.type(screen.getByLabelText('Nome *'), 'Portal 2.0')
    await userEvent.clear(screen.getByLabelText('Descrição'))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(aoGuardar).toHaveBeenCalledWith(expect.objectContaining({ nome: 'Portal 2.0' }))
    const [, init] = fetchFalso.mock.calls[0]
    expect(JSON.parse(init!.body as string)).toEqual({ nome: 'Portal 2.0', descricao: null })
  })

  it('mostra a mensagem quando o projeto está arquivado', async () => {
    simularApi({
      'PATCH /api/projetos/p1': {
        estado: 409,
        corpo: { codigo: 'PROJETO_ARQUIVADO', mensagem: 'O projeto está arquivado e não pode ser alterado.' },
      },
    })
    abrir({ projeto: projeto({ id: 'p1', nome: 'Portal' }) })

    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('O projeto está arquivado e não pode ser alterado.')
  })

  it('Cancelar e Esc fecham', async () => {
    simularApi({})
    const { aoFechar } = abrir()

    expect(screen.getByLabelText('Nome *')).toHaveFocus()
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    await userEvent.keyboard('{Escape}')
    expect(aoFechar).toHaveBeenCalledTimes(2)
  })
})
