import { Navigate, Route, Routes } from 'react-router'
import { AreaAutenticada } from './componentes/AreaAutenticada'
import { DetalheProjeto } from './ecras/DetalheProjeto'
import { Inicio } from './ecras/Inicio'
import { InicioSessao } from './ecras/InicioSessao'
import { Projetos } from './ecras/Projetos'
import { RotaProtegida } from './sessao/RotaProtegida'
import { SoMembros } from './sessao/SoMembros'

/** Rotas da aplicação. Todas exigem sessão, exceto o início de sessão. */
export function RotasMatriz() {
  return (
    <Routes>
      <Route path="/entrar" element={<InicioSessao />} />
      <Route element={<RotaProtegida />}>
        <Route element={<AreaAutenticada />}>
          <Route index element={<Inicio />} />
          <Route element={<SoMembros />}>
            <Route path="projetos" element={<Projetos />} />
            <Route path="projetos/:projetoId" element={<DetalheProjeto />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
