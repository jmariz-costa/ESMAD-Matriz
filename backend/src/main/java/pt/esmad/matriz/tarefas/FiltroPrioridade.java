package pt.esmad.matriz.tarefas;

/** Filtro de prioridade da listagem (US17 crit. 3; openapi.yaml, FiltroPrioridade). */
enum FiltroPrioridade {
    BAIXA,
    MEDIA,
    ALTA,
    /** Tarefas sem prioridade. */
    SEM_PRIORIDADE;

    /** A prioridade a procurar; {@code null} para {@link #SEM_PRIORIDADE}. */
    Prioridade prioridade() {
        return this == SEM_PRIORIDADE ? null : Prioridade.valueOf(name());
    }
}
