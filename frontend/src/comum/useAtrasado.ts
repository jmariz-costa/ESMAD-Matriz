import { useEffect, useState } from 'react'

/** O valor, mas só depois de ter ficado igual durante {@code ms} (por exemplo, pesquisa enquanto se escreve). */
export function useAtrasado<T>(valor: T, ms: number): T {
  const [atrasado, setAtrasado] = useState(valor)
  useEffect(() => {
    const espera = setTimeout(() => setAtrasado(valor), ms)
    return () => clearTimeout(espera)
  }, [valor, ms])
  return atrasado
}
