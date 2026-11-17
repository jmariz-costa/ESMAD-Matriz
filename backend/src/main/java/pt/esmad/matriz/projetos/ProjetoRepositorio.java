package pt.esmad.matriz.projetos;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.comum.texto.Texto;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjetoRepositorio implements PanacheRepositoryBase<Projeto, UUID> {

    private static final String DO_MEMBRO = """
            :membro member of p.membros
            and p.arquivado = false
            and (:padrao is null or lower(p.nome) like :padrao escape '\\')
            """;

    /**
     * Projetos não arquivados de que o utilizador é membro (RN20), ordenados pelo nome,
     * com pesquisa opcional sem distinguir maiúsculas (US17).
     */
    public List<Projeto> doMembro(UUID membro, String pesquisa, PedidoPagina pagina) {
        return getEntityManager()
                .createQuery("select p from Projeto p where " + DO_MEMBRO
                        + " order by lower(p.nome), p.id", Projeto.class)
                .setParameter("membro", membro)
                .setParameter("padrao", Texto.padraoPesquisa(pesquisa))
                .setFirstResult(pagina.inicio())
                .setMaxResults(pagina.tamanho())
                .getResultList();
    }

    /** Projetos de que a pessoa é Gestora, incluindo arquivados, ordenados pelo nome (US14 crit. 3). */
    public List<Projeto> geridosPor(UUID gestorId) {
        return getEntityManager()
                .createQuery("select p from Projeto p where p.gestorId = :gestor order by lower(p.nome), p.id",
                        Projeto.class)
                .setParameter("gestor", gestorId)
                .getResultList();
    }

    /** Todos os projetos não arquivados de que o utilizador é membro, sem ordem nem página. */
    public List<Projeto> todosDoMembro(UUID membro) {
        return getEntityManager()
                .createQuery("select p from Projeto p where :membro member of p.membros and p.arquivado = false",
                        Projeto.class)
                .setParameter("membro", membro)
                .getResultList();
    }

    public long contarDoMembro(UUID membro, String pesquisa) {
        return getEntityManager()
                .createQuery("select count(p) from Projeto p where " + DO_MEMBRO, Long.class)
                .setParameter("membro", membro)
                .setParameter("padrao", Texto.padraoPesquisa(pesquisa))
                .getSingleResult();
    }
}
