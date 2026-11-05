import { Outlet } from 'react-router'
import { Cabecalho } from './Cabecalho'

/** Estrutura comum dos ecrãs com sessão iniciada. */
export function AreaAutenticada() {
  return (
    <>
      <Cabecalho />
      <main className="conteudo">
        <Outlet />
      </main>
    </>
  )
}
