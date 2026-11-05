import { useUtilizador } from '../sessao/sessao'
import { Painel } from './Painel'

/**
 * Página inicial: o painel para os Membros (US18). O Administrador só acede à gestão de
 * contas (US02 crit. 2), que chega numa fase seguinte (US14).
 */
export function Inicio() {
  const utilizador = useUtilizador()

  if (utilizador.tipo === 'ADMINISTRADOR') {
    return (
      <section>
        <h1>Gestão de contas</h1>
        <p>Disponível numa fase seguinte.</p>
      </section>
    )
  }
  return <Painel />
}
