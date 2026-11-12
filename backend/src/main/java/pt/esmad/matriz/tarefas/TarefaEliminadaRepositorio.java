package pt.esmad.matriz.tarefas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
class TarefaEliminadaRepositorio implements PanacheRepositoryBase<TarefaEliminada, UUID> {
}
