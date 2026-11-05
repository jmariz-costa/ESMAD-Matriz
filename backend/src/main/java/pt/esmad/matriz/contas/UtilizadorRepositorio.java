package pt.esmad.matriz.contas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UtilizadorRepositorio implements PanacheRepositoryBase<Utilizador, UUID> {

    /** Pesquisa sem distinguir maiúsculas de minúsculas (RI13). */
    public Optional<Utilizador> procurarPorEmail(String email) {
        return find("lower(email) = ?1", email.trim().toLowerCase(Locale.ROOT)).firstResultOptional();
    }
}
