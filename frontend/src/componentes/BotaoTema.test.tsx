import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CHAVE_TEMA } from '../comum/tema'
import { BotaoTema } from './BotaoTema'

function sistemaEscuro(escuro: boolean) {
  vi.stubGlobal('matchMedia', () => ({
    matches: escuro,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }))
}

function botao() {
  return screen.getByRole('button', { name: 'Tema escuro' })
}

describe('BotaoTema', () => {
  beforeEach(() => {
    localStorage.clear()
    delete document.documentElement.dataset.tema
  })

  it('sem escolha guardada segue o sistema em modo claro', () => {
    sistemaEscuro(false)
    render(<BotaoTema />)
    expect(botao()).toHaveAttribute('aria-pressed', 'false')
    expect(botao()).toHaveAttribute('title', 'Mudar para o tema escuro')
  })

  it('sem escolha guardada segue o sistema em modo escuro', () => {
    sistemaEscuro(true)
    render(<BotaoTema />)
    expect(botao()).toHaveAttribute('aria-pressed', 'true')
    expect(botao()).toHaveAttribute('title', 'Mudar para o tema claro')
  })

  it('a escolha já aplicada ao documento prevalece sobre o sistema', () => {
    sistemaEscuro(false)
    document.documentElement.dataset.tema = 'escuro'
    render(<BotaoTema />)
    expect(botao()).toHaveAttribute('aria-pressed', 'true')
  })

  it('alternar aplica o tema ao documento e guarda a escolha', async () => {
    sistemaEscuro(false)
    render(<BotaoTema />)

    await userEvent.click(botao())
    expect(document.documentElement.dataset.tema).toBe('escuro')
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('escuro')
    expect(botao()).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(botao())
    expect(document.documentElement.dataset.tema).toBe('claro')
    expect(localStorage.getItem(CHAVE_TEMA)).toBe('claro')
    expect(botao()).toHaveAttribute('aria-pressed', 'false')
  })
})
