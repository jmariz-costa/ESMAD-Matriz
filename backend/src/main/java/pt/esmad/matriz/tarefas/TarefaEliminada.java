package pt.esmad.matriz.tarefas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registo de uma tarefa eliminada: o título, quem a eliminou e quando (RI09, US09 crit. 4).
 * A consulta destes registos pelo Gestor é da Fase 2.
 */
@Entity
@Table(name = "tarefa_eliminada")
class TarefaEliminada {

    @Id
    private UUID id;

    @Column(name = "projeto_id")
    private UUID projetoId;

    private String titulo;

    @Column(name = "eliminada_por")
    private UUID eliminadaPor;

    @Column(name = "eliminada_em")
    private Instant eliminadaEm;

    protected TarefaEliminada() {
        // JPA
    }

    static TarefaEliminada de(Tarefa tarefa, UUID eliminadaPor, Instant eliminadaEm) {
        TarefaEliminada registo = new TarefaEliminada();
        registo.id = UUID.randomUUID();
        registo.projetoId = tarefa.projetoId();
        registo.titulo = tarefa.titulo();
        registo.eliminadaPor = eliminadaPor;
        registo.eliminadaEm = eliminadaEm;
        return registo;
    }
}
