package pt.esmad.matriz.contas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.comum.texto.Texto;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UtilizadorRepositorio implements PanacheRepositoryBase<Utilizador, UUID> {

    private static final String MEMBROS_ATIVOS = """
            u.tipo = pt.esmad.matriz.contas.TipoUtilizador.MEMBRO
            and u.estado = pt.esmad.matriz.contas.EstadoConta.ATIVO
            and (:padrao is null or lower(u.nome) like :padrao escape '\\' or lower(u.email) like :padrao escape '\\')
            """;

    /** Pesquisa sem distinguir maiúsculas de minúsculas (RI13). */
    public Optional<Utilizador> procurarPorEmail(String email) {
        return find("lower(email) = ?1", email.trim().toLowerCase(Locale.ROOT)).firstResultOptional();
    }

    /** Membros com conta ativa (RI16), ordenados pelo nome, com pesquisa no nome ou no email. */
    public List<Utilizador> membrosAtivos(String pesquisa, PedidoPagina pagina) {
        return getEntityManager()
                .createQuery("select u from Utilizador u where " + MEMBROS_ATIVOS
                        + " order by lower(u.nome), u.id", Utilizador.class)
                .setParameter("padrao", Texto.padraoPesquisa(pesquisa))
                .setFirstResult(pagina.inicio())
                .setMaxResults(pagina.tamanho())
                .getResultList();
    }

    public long contarMembrosAtivos(String pesquisa) {
        return getEntityManager()
                .createQuery("select count(u) from Utilizador u where " + MEMBROS_ATIVOS, Long.class)
                .setParameter("padrao", Texto.padraoPesquisa(pesquisa))
                .getSingleResult();
    }
}
