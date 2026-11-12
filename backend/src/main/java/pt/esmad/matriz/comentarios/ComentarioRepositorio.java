package pt.esmad.matriz.comentarios;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ComentarioRepositorio implements PanacheRepositoryBase<Comentario, UUID> {

    /** Do mais antigo para o mais recente (openapi.yaml, listarComentarios). */
    List<Comentario> daTarefa(UUID tarefaId, PedidoPagina pagina) {
        return getEntityManager()
                .createQuery("select c from Comentario c where c.tarefaId = :tarefa order by c.criadoEm, c.id",
                        Comentario.class)
                .setParameter("tarefa", tarefaId)
                .setFirstResult(pagina.inicio())
                .setMaxResults(pagina.tamanho())
                .getResultList();
    }

    long contarDaTarefa(UUID tarefaId) {
        return count("tarefaId", tarefaId);
    }
}
