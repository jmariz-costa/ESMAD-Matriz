package pt.esmad.matriz.contas;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SessaoRepositorio implements PanacheRepositoryBase<Sessao, UUID> {

    public Optional<Sessao> procurarPorTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    /** Termina todas as sessões da pessoa (PR16). */
    public long apagarDoUtilizador(UUID utilizadorId) {
        return delete("utilizadorId", utilizadorId);
    }
}
