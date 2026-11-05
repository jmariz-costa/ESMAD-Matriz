import { Navigate, Outlet, useLocation } from 'react-router'
import { useSessao } from './sessao'

/** Sem sessão, encaminha para o início de sessão e guarda o ecrã de origem. */
export function RotaProtegida() {
  const { estado } = useSessao()
  const local = useLocation()

  if (estado.tipo === 'a-verificar') {
    return <p className="a-carregar">A carregar…</p>
  }
  if (estado.tipo === 'anonimo') {
    return <Navigate to="/entrar" replace state={{ origem: local.pathname + local.search }} />
  }
  return <Outlet />
}
