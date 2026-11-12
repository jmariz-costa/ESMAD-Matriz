import { describe, expect, it } from 'vitest'
import { comprimento, validarTexto } from './texto'

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
})
