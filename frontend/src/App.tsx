import { BrowserRouter } from 'react-router'
import { RotasMatriz } from './RotasMatriz'
import { FornecedorSessao } from './sessao/FornecedorSessao'

export default function App() {
  return (
    <BrowserRouter>
      <FornecedorSessao>
        <RotasMatriz />
      </FornecedorSessao>
    </BrowserRouter>
  )
}
