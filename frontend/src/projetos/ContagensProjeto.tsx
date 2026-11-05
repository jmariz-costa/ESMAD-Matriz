import type { Contagens } from '../api/projetos'
import { ESTADOS } from './estados'

/** Número de tarefas em cada um dos 4 estados (US18). */
export function ContagensProjeto({ contagens }: { contagens: Contagens }) {
  return (
    <dl className="estados">
      {ESTADOS.map(({ codigo, nome }) => (
        <div key={codigo}>
          <dt>{nome}</dt>
          <dd>{contagens[codigo]}</dd>
        </div>
      ))}
    </dl>
  )
}
