import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { RotasMatriz } from '../RotasMatriz'
import { FornecedorSessao } from '../sessao/FornecedorSessao'

/** Renderiza a aplicação completa a partir de um endereço, sem browser real. */
export function renderizarAplicacao(endereco = '/') {
  return render(
    <MemoryRouter initialEntries={[endereco]}>
      <FornecedorSessao>
        <RotasMatriz />
      </FornecedorSessao>
    </MemoryRouter>,
  )
}
