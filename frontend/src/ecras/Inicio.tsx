import { useUtilizador } from '../sessao/sessao'
import { GestaoContas } from './GestaoContas'
import { Painel } from './Painel'

/**
 * Página inicial: o painel para os Membros (US18). O Administrador só acede à gestão de
 * contas (US02 crit. 2, US14 crit. 9).
 */
export function Inicio() {
  const utilizador = useUtilizador()
  return utilizador.tipo === 'ADMINISTRADOR' ? <GestaoContas /> : <Painel />
}
