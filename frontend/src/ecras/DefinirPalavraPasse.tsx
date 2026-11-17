import { FormularioPalavraPasse } from '../contas/FormularioPalavraPasse'
import { useSessao, useUtilizador } from '../sessao/sessao'

/**
 * Depois de o Administrador repor o acesso (US19 crit. 3, PR20): antes de usar a Matriz, a
 * pessoa troca a palavra-passe temporária por uma só dela. O Backend recusa os outros pedidos.
 */
export function DefinirPalavraPasse() {
  const utilizador = useUtilizador()
  const { sair, palavraPasseMudada } = useSessao()

  return (
    <main className="login-fundo">
      <section className="login caixa">
        <span className="logo">MATRIZ</span>
        <h1>Defina uma palavra-passe nova</h1>
        <p className="suave">
          Olá, {utilizador.nome}. O Administrador repôs o seu acesso. Para continuar, troque a
          palavra-passe temporária por uma que só o próprio conheça.
        </p>
        <FormularioPalavraPasse rotuloAtual="Palavra-passe temporária" textoBotao="Guardar e continuar"
          aoMudar={palavraPasseMudada} />
        <button type="button" className="btn ligacao" onClick={() => void sair()}>Sair</button>
      </section>
    </main>
  )
}
