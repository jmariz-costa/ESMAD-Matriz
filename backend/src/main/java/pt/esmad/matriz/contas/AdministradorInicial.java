package pt.esmad.matriz.contas;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Cria a conta de Administrador no arranque, a partir da configuração (D07).
 * Se já existir uma conta com esse email, nada é alterado.
 */
@ApplicationScoped
public class AdministradorInicial {

    private static final int TAMANHO_MINIMO_PALAVRA_PASSE = 8; // RN04

    private final AgroalDataSource dataSource;
    private final String nome;
    private final String email;
    private final String palavraPasse;

    public AdministradorInicial(AgroalDataSource dataSource,
                                @ConfigProperty(name = "matriz.admin.nome") String nome,
                                // Optional: sem valor, o Quarkus não arrancaria e o aviso abaixo nunca apareceria
                                @ConfigProperty(name = "matriz.admin.email") Optional<String> email,
                                @ConfigProperty(name = "matriz.admin.password") Optional<String> palavraPasse) {
        this.dataSource = dataSource;
        this.nome = nome;
        this.email = email.orElse("");
        this.palavraPasse = palavraPasse.orElse("");
    }

    @Transactional
    void aoArrancar(@Observes StartupEvent evento) throws SQLException {
        if (email.isBlank() || palavraPasse.isBlank()) {
            Log.warn("Administrador não configurado (MATRIZ_ADMIN_EMAIL / MATRIZ_ADMIN_PASSWORD).");
            return;
        }
        if (palavraPasse.length() < TAMANHO_MINIMO_PALAVRA_PASSE) {
            Log.error("A palavra-passe do Administrador tem de ter pelo menos 8 caracteres; conta não criada.");
            return;
        }
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        try (Connection ligacao = dataSource.getConnection()) {
            if (existe(ligacao, emailNormalizado)) {
                return;
            }
            try (PreparedStatement insert = ligacao.prepareStatement("""
                    INSERT INTO utilizador (id, nome, email, password_hash, tipo, estado)
                    VALUES (?, ?, ?, ?, 'ADMINISTRADOR', 'ATIVO')
                    """)) {
                insert.setObject(1, UUID.randomUUID());
                insert.setString(2, nome.trim());
                insert.setString(3, emailNormalizado);
                insert.setString(4, BcryptUtil.bcryptHash(palavraPasse));
                insert.executeUpdate();
            }
            Log.infof("Conta de Administrador criada: %s", emailNormalizado);
        }
    }

    private static boolean existe(Connection ligacao, String email) throws SQLException {
        try (PreparedStatement select = ligacao.prepareStatement(
                "SELECT 1 FROM utilizador WHERE lower(email) = ?")) {
            select.setString(1, email);
            try (ResultSet resultado = select.executeQuery()) {
                return resultado.next();
            }
        }
    }
}
