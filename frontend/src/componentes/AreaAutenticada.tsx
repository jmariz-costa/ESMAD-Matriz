import { Outlet } from 'react-router'
import { Cabecalho } from './Cabecalho'
import { FornecedorNotificacao } from './FornecedorNotificacao'

/** Estrutura comum dos ecrãs com sessão iniciada. */
export function AreaAutenticada() {
  return (
    <FornecedorNotificacao>
      <Cabecalho />
      <main className="conteudo">
        <Outlet />
      </main>
    </FornecedorNotificacao>
  )
}
