// Regras comuns de texto (RN23), iguais às do Backend (comum/texto/Texto.java):
// os espaços e os caracteres invisíveis nas pontas não contam, e cada carácter conta como o
// utilizador o vê. Revisão da Ficha PL 04 (Q1/S2): antes usava trim(), que não retira U+200B.

/** Caracteres como o utilizador os vê (um emoji conta como um), igual ao Backend. */
export function comprimento(texto: string): number {
  return [...texto].length
}

// Espaços (incluindo os Unicode, como U+00A0, e os separadores U+001C–U+001F) e caracteres
// invisíveis de formatação (categoria Cf, como U+200B e U+FEFF): os mesmos que o Backend retira.
// eslint-disable-next-line no-control-regex -- U+001C–U+001F são intencionais: o Java também os trata como espaço
const INVISIVEIS_NAS_PONTAS = /^[\s\u001C-\u001F\p{Zs}\p{Cf}]+|[\s\u001C-\u001F\p{Zs}\p{Cf}]+$/gu

/** O texto sem espaços nem caracteres invisíveis nas pontas; o que está no meio mantém-se. */
export function limpar(texto: string): string {
  return texto.replace(INVISIVEIS_NAS_PONTAS, '')
}

export interface RegraTexto {
  /** Mensagem quando o texto está vazio ou só tem espaços; sem ela, o campo é opcional. */
  obrigatorio?: string
  minimo?: number
  maximo: number
  /** Mensagem quando o texto, sem os espaços nem os invisíveis nas pontas, está fora dos limites. */
  foraDosLimites: string
}

/** A mensagem de erro do campo, ou null se o texto cumprir a regra. O Backend volta a verificar. */
export function validarTexto(texto: string, regra: RegraTexto): string | null {
  const n = comprimento(limpar(texto))
  if (n === 0) {
    return regra.obrigatorio ?? null
  }
  if ((regra.minimo !== undefined && n < regra.minimo) || n > regra.maximo) {
    return regra.foraDosLimites
  }
  return null
}
