import type { FiltroPrioridade, Prioridade } from '../api/tarefas'

/** Prioridades com o texto mostrado na interface (RN10, PR18), da mais alta para a mais baixa. */
export const PRIORIDADES: ReadonlyArray<{ codigo: Prioridade; nome: string }> = [
  { codigo: 'ALTA', nome: 'Alta' },
  { codigo: 'MEDIA', nome: 'Média' },
  { codigo: 'BAIXA', nome: 'Baixa' },
]

export const FILTROS_PRIORIDADE: ReadonlyArray<{ codigo: FiltroPrioridade; nome: string }> = [
  ...PRIORIDADES,
  { codigo: 'SEM_PRIORIDADE', nome: 'Sem prioridade' },
]

export function nomePrioridade(prioridade: Prioridade): string {
  return PRIORIDADES.find((p) => p.codigo === prioridade)?.nome ?? prioridade
}

/** AAAA-MM-DD → DD/MM/AAAA, sem passar por Date (evita mudanças de dia por fuso horário). */
export function formatarPrazo(prazo: string): string {
  const [ano, mes, dia] = prazo.split('-')
  return `${dia}/${mes}/${ano}`
}

const DATA_HORA = new Intl.DateTimeFormat('pt-PT', {
  day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

/** Data e hora ISO 8601 → "12/11/2026, 18:45", na hora local do browser. */
export function formatarDataHora(iso: string): string {
  return DATA_HORA.format(new Date(iso))
}
