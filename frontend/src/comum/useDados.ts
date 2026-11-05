import { useCallback, useEffect, useRef, useState } from 'react'
import { comoErroApi, type ErroApi } from '../api/cliente'

export type Dados<T> =
  | { estado: 'a-carregar' }
  | { estado: 'erro'; erro: ErroApi }
  | { estado: 'pronto'; dados: T }

const A_CARREGAR = { estado: 'a-carregar' } as const

/**
 * Carrega dados da API quando o ecrã abre e sempre que a {@code chave} muda
 * (por exemplo, a pesquisa e a página). Ao recarregar, os dados anteriores continuam
 * visíveis até chegarem os novos. Devolve também a função para recarregar
 * (depois de criar, ou em "Tentar novamente").
 */
export function useDados<T>(carregar: () => Promise<T>, chave: string): [Dados<T>, () => void] {
  const [versao, setVersao] = useState(0)
  const [resultado, setResultado] = useState<{ pedido: string; dados: Dados<T> } | null>(null)
  const pedido = `${chave}#${versao}`

  // A função mais recente, sem que uma nova função em cada renderização repita o pedido
  const carregarAtual = useRef(carregar)
  useEffect(() => {
    carregarAtual.current = carregar
  })

  useEffect(() => {
    let ativo = true
    carregarAtual.current()
      .then((dados) => ativo && setResultado({ pedido, dados: { estado: 'pronto', dados } }))
      .catch((erro: unknown) => ativo && setResultado({ pedido, dados: { estado: 'erro', erro: comoErroApi(erro) } }))
    return () => {
      ativo = false
    }
  }, [pedido])

  const recarregar = useCallback(() => setVersao((v) => v + 1), [])

  let dados: Dados<T> = A_CARREGAR
  if (resultado?.pedido === pedido) {
    dados = resultado.dados
  } else if (resultado?.dados.estado === 'pronto') {
    dados = resultado.dados
  }
  return [dados, recarregar]
}
