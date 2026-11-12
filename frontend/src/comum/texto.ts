// Regras comuns de texto (RN23), iguais às do Backend (comum/texto/Texto.java):
// os espaços nas pontas não contam e cada carácter conta como o utilizador o vê.

/** Caracteres como o utilizador os vê (um emoji conta como um), igual ao Backend. */
export function comprimento(texto: string): number {
  return [...texto].length
}

export interface RegraTexto {
  /** Mensagem quando o texto está vazio ou só tem espaços; sem ela, o campo é opcional. */
  obrigatorio?: string
  minimo?: number
  maximo: number
  /** Mensagem quando o texto, sem os espaços nas pontas, está fora dos limites. */
  foraDosLimites: string
}

/** A mensagem de erro do campo, ou null se o texto cumprir a regra. O Backend volta a verificar. */
export function validarTexto(texto: string, regra: RegraTexto): string | null {
  const n = comprimento(texto.trim())
  if (n === 0) {
    return regra.obrigatorio ?? null
  }
  if ((regra.minimo !== undefined && n < regra.minimo) || n > regra.maximo) {
    return regra.foraDosLimites
  }
  return null
}
