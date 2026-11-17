import { Dialogo } from '../componentes/Dialogo'
import { useNotificar } from '../comum/notificacao'
import { FormularioPalavraPasse } from './FormularioPalavraPasse'

/** Mudar a própria palavra-passe, a partir do nome no cabeçalho (US20). */
export function MudarPalavraPasse({ aoFechar }: { aoFechar: () => void }) {
  const notificar = useNotificar()

  return (
    <Dialogo titulo="Mudar a palavra-passe" aoFechar={aoFechar}>
      <p className="suave">As outras sessões abertas com a sua conta vão terminar; esta mantém-se.</p>
      <FormularioPalavraPasse rotuloAtual="Palavra-passe atual" textoBotao="Mudar a palavra-passe"
        aoCancelar={aoFechar}
        aoMudar={() => {
          notificar('Palavra-passe mudada.')
          aoFechar()
        }} />
    </Dialogo>
  )
}
