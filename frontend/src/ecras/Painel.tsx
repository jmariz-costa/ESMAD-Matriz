import { useState } from 'react'
import { Link } from 'react-router'
import { listarProjetos, type Projeto } from '../api/projetos'
import { useDados } from '../comum/useDados'
import { ContagensProjeto } from '../projetos/ContagensProjeto'
import { FormularioProjeto } from '../projetos/FormularioProjeto'
import { percentagemConclusao, textoTarefas, totalTarefas } from '../projetos/percentagem'
import { useUtilizador } from '../sessao/sessao'

function CartaoProjeto({ projeto }: { projeto: Projeto }) {
  return (
    <li className="cartao-projeto caixa">
      <h3>
        <Link to={`/projetos/${projeto.id}`}>{projeto.nome}</Link>
        {projeto.souGestor && <span className="etiqueta-gestor">Gestor</span>}
      </h3>
      <small>
        {percentagemConclusao(projeto.contagens)}% concluído · {textoTarefas(totalTarefas(projeto.contagens))}
      </small>
      <ContagensProjeto contagens={projeto.contagens} />
    </li>
  )
}

/** Ecrã 2 · Painel inicial (US18, US10): os meus projetos não arquivados e as suas contagens. */
export function Painel() {
  const utilizador = useUtilizador()
  const [dados, recarregar] = useDados(() => listarProjetos(), 'painel')
  const [aCriar, setACriar] = useState(false)

  const botaoNovo = (
    <button type="button" className="btn principal" onClick={() => setACriar(true)}>+ Novo projeto</button>
  )

  return (
    <section>
      <div className="barra-titulo">
        <h1>Olá, {utilizador.nome}</h1>
        {dados.estado === 'pronto' && dados.dados.total > 0 && botaoNovo}
      </div>

      {dados.estado === 'a-carregar' && <p className="vazio">A carregar os projetos…</p>}

      {dados.estado === 'erro' && (
        <div className="vazio" role="alert">
          <p>Não foi possível carregar os projetos.</p>
          <button type="button" className="btn" onClick={recarregar}>Tentar novamente</button>
        </div>
      )}

      {dados.estado === 'pronto' && dados.dados.total === 0 && (
        <div className="vazio">
          <p>Ainda não faz parte de nenhum projeto.</p>
          {botaoNovo}
        </div>
      )}

      {dados.estado === 'pronto' && dados.dados.total > 0 && (
        <>
          <ul className="cartoes">
            {dados.dados.itens.map((projeto) => <CartaoProjeto key={projeto.id} projeto={projeto} />)}
          </ul>
          {dados.dados.total > dados.dados.itens.length && (
            <p className="ver-todos">
              <Link to="/projetos">Ver todos os projetos ({dados.dados.total})</Link>
            </p>
          )}
        </>
      )}

      {aCriar && (
        <FormularioProjeto
          aoFechar={() => setACriar(false)}
          aoGuardar={() => {
            setACriar(false)
            recarregar()
          }}
        />
      )}
    </section>
  )
}
