import { describe, expect, it } from 'vitest'
import { comprimento, limpar, validarTexto } from './texto'

describe('Regras de texto (RN23)', () => {
  it('conta os caracteres como o utilizador os vê', () => {
    expect(comprimento('abc')).toBe(3)
    expect(comprimento('😀😀😀')).toBe(3)
  })

  const titulo = { obrigatorio: 'Obrigatório.', minimo: 3, maximo: 5, foraDosLimites: 'Fora dos limites.' }

  it('campo obrigatório: vazio ou só com espaços é recusado; os espaços nas pontas não contam', () => {
    expect(validarTexto('', titulo)).toBe('Obrigatório.')
    expect(validarTexto('    ', titulo)).toBe('Obrigatório.')
    expect(validarTexto(' ab ', titulo)).toBe('Fora dos limites.')
    expect(validarTexto('  abc  ', titulo)).toBeNull()
    expect(validarTexto('abcde', titulo)).toBeNull()
    expect(validarTexto('abcdef', titulo)).toBe('Fora dos limites.')
    expect(validarTexto('😀😀😀', titulo)).toBeNull()
  })

  it('campo opcional: vazio é aceite e só o máximo conta', () => {
    const descricao = { maximo: 3, foraDosLimites: 'Demasiado longo.' }
    expect(validarTexto('', descricao)).toBeNull()
    expect(validarTexto('   ', descricao)).toBeNull()
    expect(validarTexto('a', descricao)).toBeNull()
    expect(validarTexto('abcd', descricao)).toBe('Demasiado longo.')
  })

  // Regressão da revisão da Ficha PL 04 (Q1/S2): a mesma regra do Backend (Texto.java)
  it.each(['0020', '0009', '00A0', '2003', '2007', '202F', '3000', '2028', '200B', '200D', '2060', 'FEFF', '001C'])(
    'U+%s só, ou nas pontas, não conta', (hex) => {
      const c = String.fromCodePoint(parseInt(hex, 16))
      expect(validarTexto(c.repeat(3), titulo)).toBe('Obrigatório.')
      expect(limpar(c + 'abc' + c)).toBe('abc')
    })

  it('o que está no meio mantém-se, incluindo emojis compostos', () => {
    expect(limpar(' a\u00A0b ')).toBe('a\u00A0b')
    expect(limpar('\u200B👩‍💻 Rever\uFEFF')).toBe('👩‍💻 Rever')
    expect(validarTexto('\u00A0ab\u200B', titulo)).toBe('Fora dos limites.')
  })
})
