import { Navigate, Outlet } from 'react-router'
import { useUtilizador } from './sessao'

/** Ecrãs de projetos: o Administrador não participa em projetos (RN19) e volta ao início. */
export function SoMembros() {
  return useUtilizador().tipo === 'ADMINISTRADOR' ? <Navigate to="/" replace /> : <Outlet />
}
