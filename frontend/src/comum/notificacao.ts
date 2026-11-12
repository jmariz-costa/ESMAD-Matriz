import { createContext, useContext } from 'react'

/** Mostra uma mensagem de sucesso breve e uniforme (Ficha PL 03, Exercício 7: estado "sucesso"). */
export type Notificar = (mensagem: string) => void

export const ContextoNotificacao = createContext<Notificar>(() => {})

/** Para confirmar uma operação concluída (ex.: "Tarefa criada."). */
export function useNotificar(): Notificar {
  return useContext(ContextoNotificacao)
}
