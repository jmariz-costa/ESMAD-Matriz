import { useEffect, useState } from 'react'

export type Tema = 'claro' | 'escuro'

/** Chave no localStorage; também é lida no index.html, antes de a aplicação arrancar. */
export const CHAVE_TEMA = 'matriz-tema'

const CONSULTA_ESCURO = '(prefers-color-scheme: dark)'

function temaDoSistema(): Tema {
  return window.matchMedia?.(CONSULTA_ESCURO).matches ? 'escuro' : 'claro'
}

/** O tema escolhido pelo utilizador (atributo data-tema do <html>), ou null se segue o sistema. */
function temaEscolhido(): Tema | null {
  const atributo = document.documentElement.dataset.tema
  return atributo === 'claro' || atributo === 'escuro' ? atributo : null
}

/**
 * Tema em uso e função para alternar entre claro e escuro. Sem escolha guardada, segue o
 * sistema operativo; ao alternar, a escolha fica guardada neste browser (localStorage).
 */
export function useTema(): [Tema, () => void] {
  const [tema, setTema] = useState<Tema>(() => temaEscolhido() ?? temaDoSistema())

  // Enquanto o utilizador não escolher, acompanha as mudanças do tema do sistema
  useEffect(() => {
    const consulta = window.matchMedia?.(CONSULTA_ESCURO)
    if (!consulta) return
    const mudou = () => {
      if (temaEscolhido() === null) setTema(temaDoSistema())
    }
    consulta.addEventListener('change', mudou)
    return () => consulta.removeEventListener('change', mudou)
  }, [])

  function alternar() {
    const novo: Tema = tema === 'escuro' ? 'claro' : 'escuro'
    document.documentElement.dataset.tema = novo
    try {
      localStorage.setItem(CHAVE_TEMA, novo)
    } catch {
      // Sem armazenamento disponível (ex.: bloqueado): a escolha vale só até recarregar
    }
    setTema(novo)
  }

  return [tema, alternar]
}
