package pt.esmad.matriz.contas;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ciclo da sessão: GET /auth/me, POST /auth/logout, expiração e renovação (US02 crit. 4, D06). */
@QuarkusTest
class SessaoApiTest {

    private static final String PALAVRA_PASSE = "palavra-passe-ficticia";
    private static final String COOKIE = "matriz_sessao";

    @Inject
    UtilizadorRepositorio utilizadores;

    @Inject
    SessaoRepositorio sessoes;

    private String email;

    @BeforeEach
    void criarMembro() {
        email = "membro-" + UUID.randomUUID() + "@empresa.pt";
        criarConta(email, TipoUtilizador.MEMBRO);
    }

    private void criarConta(String emailConta, TipoUtilizador tipo) {
        QuarkusTransaction.requiringNew().run(() -> utilizadores.persist(new Utilizador(
                UUID.randomUUID(), "Pessoa Exemplo", emailConta, BcryptUtil.bcryptHash(PALAVRA_PASSE),
                tipo, EstadoConta.ATIVO)));
    }

    private static String entrar(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "palavraPasse", PALAVRA_PASSE))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().cookie(COOKIE);
    }

    private static io.restassured.response.Response me(String token) {
        return given().cookie(COOKIE, token).when().get("/api/auth/me");
    }

    private static void assertNaoAutenticado(io.restassured.response.Response resposta) {
        resposta.then()
                .statusCode(401)
                .body("codigo", equalTo("NAO_AUTENTICADO"))
                .body("mensagem", equalTo("A sessão não é válida. Inicie sessão novamente."));
    }

    /** Altera diretamente a base de dados, para simular a passagem do tempo ou outras ações. */
    private void sql(String instrucao, Object... parametros) {
        QuarkusTransaction.requiringNew().run(() -> {
            var consulta = utilizadores.getEntityManager().createNativeQuery(instrucao);
            for (int i = 0; i < parametros.length; i++) {
                consulta.setParameter(i + 1, parametros[i]);
            }
            consulta.executeUpdate();
        });
    }

    private Sessao sessaoDe(String token) {
        return QuarkusTransaction.requiringNew().call(() ->
                sessoes.procurarPorTokenHash(TokenSessao.hash(token)).orElse(null));
    }

    @Test
    void semCookieDevolve401() {
        assertNaoAutenticado(given().when().get("/api/auth/me"));
    }

    @Test
    void tokenInventadoDevolve401() {
        assertNaoAutenticado(me("token-inventado"));
    }

    @Test
    void comSessaoDevolveOUtilizador() {
        me(entrar(email)).then()
                .statusCode(200)
                .body("email", equalTo(email))
                .body("nome", equalTo("Pessoa Exemplo"))
                .body("tipo", equalTo("MEMBRO"));
    }

    @Test
    void administradorTemTipoAdministrador() {
        String admin = "admin-" + UUID.randomUUID() + "@empresa.pt";
        criarConta(admin, TipoUtilizador.ADMINISTRADOR);
        me(entrar(admin)).then().statusCode(200).body("tipo", equalTo("ADMINISTRADOR"));
    }

    @Test
    void sairApagaASessaoEOCookie() {
        String token = entrar(email);

        given().cookie(COOKIE, token).when().post("/api/auth/logout")
                .then()
                .statusCode(204)
                .header("Set-Cookie", containsString("matriz_sessao="))
                .header("Set-Cookie", containsString("Max-Age=0"));

        assertNaoAutenticado(me(token));
        assertEquals(null, sessaoDe(token));
    }

    @Test
    void sairSemSessaoDevolve204() {
        given().when().post("/api/auth/logout").then().statusCode(204);
        given().cookie(COOKIE, "token-inventado").when().post("/api/auth/logout").then().statusCode(204);
    }

    @Test
    void sairSoTerminaASessaoAtual() {
        String primeira = entrar(email);
        String segunda = entrar(email);
        given().cookie(COOKIE, primeira).when().post("/api/auth/logout").then().statusCode(204);
        me(segunda).then().statusCode(200);
    }

    @Test
    void sessaoExpiraAoFimDeTrintaMinutosSemAtividade() {
        String token = entrar(email);
        sql("UPDATE sessao SET ultima_atividade = ?1 WHERE token_hash = ?2",
                Instant.now().minus(Duration.ofMinutes(31)), TokenSessao.hash(token));

        assertNaoAutenticado(me(token));
        assertEquals(null, sessaoDe(token), "a sessão expirada é apagada");
    }

    @Test
    void cadaPedidoRenovaOPrazo() {
        String token = entrar(email);
        Instant haVinteMinutos = Instant.now().minus(Duration.ofMinutes(20));
        sql("UPDATE sessao SET ultima_atividade = ?1 WHERE token_hash = ?2",
                haVinteMinutos, TokenSessao.hash(token));

        me(token).then().statusCode(200);

        assertTrue(sessaoDe(token).ultimaAtividade().isAfter(haVinteMinutos.plus(Duration.ofMinutes(19))));
    }

    @Test
    void contaDesativadaPerdeOAcesso() {
        String token = entrar(email);
        sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE lower(email) = ?1", email);

        assertNaoAutenticado(me(token));
    }
}
