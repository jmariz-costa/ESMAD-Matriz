package pt.esmad.matriz;

import io.agroal.api.AgroalDataSource;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Cria contas fictícias e inicia sessão nos testes de integração.
 * Cada conta tem um email novo, para os testes não dependerem uns dos outros.
 */
@ApplicationScoped
public class ContasDeTeste {

    public static final String PALAVRA_PASSE = "palavra-passe-ficticia";
    public static final String COOKIE = "matriz_sessao";
    private static final String HASH = BcryptUtil.bcryptHash(PALAVRA_PASSE);

    private final AgroalDataSource dataSource;

    public ContasDeTeste(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record Conta(UUID id, String nome, String email, String token) {
    }

    public Conta membro(String nome) {
        return criar(nome, "MEMBRO");
    }

    public Conta administrador() {
        return criar("Administrador", "ADMINISTRADOR");
    }

    /** Conta sem sessão iniciada (por exemplo, desativada ou por confirmar, que não podem entrar). */
    public UUID semSessao(String nome, String tipo, String estado) {
        UUID id = UUID.randomUUID();
        inserir(id, nome, "conta-" + id + "@empresa.pt", tipo, estado);
        return id;
    }

    private void inserir(UUID id, String nome, String email, String tipo, String estado) {
        try (Connection ligacao = dataSource.getConnection();
             PreparedStatement insert = ligacao.prepareStatement("""
                     INSERT INTO utilizador (id, nome, email, password_hash, tipo, estado)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            insert.setObject(1, id);
            insert.setString(2, nome);
            insert.setString(3, email);
            insert.setString(4, HASH);
            insert.setString(5, tipo);
            insert.setString(6, estado);
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Conta criar(String nome, String tipo) {
        UUID id = UUID.randomUUID();
        String email = "conta-" + id + "@empresa.pt";
        inserir(id, nome, email, tipo, "ATIVO");
        String token = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "palavraPasse", PALAVRA_PASSE))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().cookie(COOKIE);
        return new Conta(id, nome, email, token);
    }

    /** Resultado numérico de uma consulta SQL (por exemplo, um count). */
    public long contar(String consulta, Object... parametros) {
        try (Connection ligacao = dataSource.getConnection();
             PreparedStatement comando = ligacao.prepareStatement(consulta)) {
            for (int i = 0; i < parametros.length; i++) {
                comando.setObject(i + 1, parametros[i]);
            }
            try (var resultado = comando.executeQuery()) {
                resultado.next();
                return resultado.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Executa SQL diretamente, para preparar dados que ainda não têm API (por exemplo, tarefas). */
    public void sql(String instrucao, Object... parametros) {
        try (Connection ligacao = dataSource.getConnection();
             PreparedStatement comando = ligacao.prepareStatement(instrucao)) {
            for (int i = 0; i < parametros.length; i++) {
                comando.setObject(i + 1, parametros[i]);
            }
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
