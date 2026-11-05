import type { Estado } from '../api/projetos'

/** Os 4 estados pela ordem do quadro, com o texto mostrado na interface (RN13, PR18). */
export const ESTADOS: ReadonlyArray<{ codigo: Estado; nome: string }> = [
  { codigo: 'A_FAZER', nome: 'A fazer' },
  { codigo: 'EM_CURSO', nome: 'Em curso' },
  { codigo: 'EM_REVISAO', nome: 'Em revisão' },
  { codigo: 'CONCLUIDA', nome: 'Concluída' },
]
