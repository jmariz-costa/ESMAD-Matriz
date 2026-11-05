import { describe, expect, it } from 'vitest'
import { percentagemConclusao, textoTarefas, totalTarefas } from './percentagem'

const contagens = (A_FAZER: number, EM_CURSO: number, EM_REVISAO: number, CONCLUIDA: number) =>
  ({ A_FAZER, EM_CURSO, EM_REVISAO, CONCLUIDA })

describe('percentagem de conclusão (US10)', () => {
  it('arredonda para baixo', () => {
    expect(percentagemConclusao(contagens(1, 0, 0, 3))).toBe(75)
    expect(percentagemConclusao(contagens(1, 0, 0, 199))).toBe(99)
    expect(percentagemConclusao(contagens(2, 2, 1, 4))).toBe(44)
  })

  it('é 0% sem tarefas e 100% só com todas concluídas', () => {
    expect(percentagemConclusao(contagens(0, 0, 0, 0))).toBe(0)
    expect(percentagemConclusao(contagens(0, 0, 0, 5))).toBe(100)
  })

  it('conta as tarefas de todos os estados', () => {
    expect(totalTarefas(contagens(2, 2, 1, 7))).toBe(12)
    expect(textoTarefas(1)).toBe('1 tarefa')
    expect(textoTarefas(0)).toBe('0 tarefas')
  })
})
