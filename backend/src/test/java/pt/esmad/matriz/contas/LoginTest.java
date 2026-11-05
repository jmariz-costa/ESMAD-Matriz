package pt.esmad.matriz.contas;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** POST /auth/login (US02 crit. 1 e 3, PR03, D06, D09). Contas e palavras-passe fictícias. */
@QuarkusTest
class LoginTest {

    private static final String PALAVRA_PASSE = "palavra-passe-ficticia";
    private static final String MENSAGEM = "Email ou palavra-passe incorretos.";

    @Inject
    UtilizadorRepositorio utilizadores;

    @Inject
    SessaoRepositorio sessoes;

    private String email;

    /** Cada teste usa um email novo, para não depender do estado deixado por outros. */
    @BeforeEach
    void criarMembro() {
        email = "membro-" + UUID.randomUUID() + "@empresa.pt";
        criarConta(email, EstadoConta.ATIVO);
    }

    private void criarConta(String emailConta, EstadoConta estado) {
        QuarkusTransaction.requiringNew().run(() -> utilizadores.persist(new Utilizador(
                UUID.randomUUID(), "Membro Exemplo", emailConta, BcryptUtil.bcryptHash(PALAVRA_PASSE),
                TipoUtilizador.MEMBRO, estado)));
    }

    private static Response login(String email, String palavraPasse) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "palavraPasse", palavraPasse))
                .when().post("/api/auth/login");
    }

    private static void assertCredenciaisInvalidas(Response resposta) {
        resposta.then()
                .statusCode(401)
                .body("codigo", equalTo("CREDENCIAIS_INVALIDAS"))
                .body("mensagem", equalTo(MENSAGEM));
    }

    @Test
    void credenciaisCorretasIniciamSessao() {
        Response resposta = login(email, PALAVRA_PASSE);

        resposta.then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("nome", equalTo("Membro Exemplo"))
                .body("email", equalTo(email))
                .body("tipo", equalTo("MEMBRO"))
                .header("Set-Cookie", containsString("matriz_sessao="))
                .header("Set-Cookie", containsString("HttpOnly"))
                .header("Set-Cookie", containsString("SameSite=Strict"))
                .header("Set-Cookie", containsString("Path=/api"));

        String token = resposta.getCookie("matriz_sessao");
        UUID id = UUID.fromString(resposta.path("id"));
        QuarkusTransaction.requiringNew().run(() -> {
            assertTrue(sessoes.procurarPorTokenHash(TokenSessao.hash(token)).isPresent(),
                    "a sessão é guardada pelo hash do token");
            assertEquals(0, sessoes.count("tokenHash", token), "o token nunca é guardado em claro");
            assertEquals(1, sessoes.count("utilizadorId", id));
        });
    }

    @Test
    void emailSemDistinguirMaiusculas() {
        login(email.toUpperCase(), PALAVRA_PASSE).then().statusCode(200);
    }

    @Test
    void palavraPasseErradaERecusada() {
        assertCredenciaisInvalidas(login(email, "errada"));
    }

    @Test
    void emailInexistenteTemAMesmaResposta() {
        assertCredenciaisInvalidas(login("ninguem-" + UUID.randomUUID() + "@empresa.pt", PALAVRA_PASSE));
    }

    @Test
    void contaDesativadaERecusadaComAMesmaResposta() {
        String desativado = "desativado-" + UUID.randomUUID() + "@empresa.pt";
        criarConta(desativado, EstadoConta.DESATIVADO);
        assertCredenciaisInvalidas(login(desativado, PALAVRA_PASSE));
    }

    @Test
    void cincoFalhasBloqueiamMesmoComAPalavraPasseCerta() {
        for (int i = 0; i < 5; i++) {
            assertCredenciaisInvalidas(login(email, "errada"));
        }
        assertCredenciaisInvalidas(login(email, PALAVRA_PASSE));
    }

    @Test
    void loginCorretoPoeContadorAZero() {
        for (int i = 0; i < 4; i++) {
            login(email, "errada");
        }
        login(email, PALAVRA_PASSE).then().statusCode(200);
        for (int i = 0; i < 4; i++) {
            login(email, "errada");
        }
        login(email, PALAVRA_PASSE).then().statusCode(200);
    }

    @Test
    void cadaLoginCriaUmaSessaoComTokenDiferente() {
        String primeiro = login(email, PALAVRA_PASSE).getCookie("matriz_sessao");
        String segundo = login(email, PALAVRA_PASSE).getCookie("matriz_sessao");
        assertNotEquals(primeiro, segundo);
    }

    @Test
    void camposEmFaltaDevolvemValidacao() {
        given().contentType(ContentType.JSON).body(Map.of("email", "nao-e-email"))
                .when().post("/api/auth/login")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("email"))
                .body("detalhes.campo", hasItem("palavraPasse"));
    }

    @Test
    void corpoEmFaltaDevolveValidacao() {
        given().contentType(ContentType.JSON)
                .when().post("/api/auth/login")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void campoDesconhecidoDevolveValidacao() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "palavraPasse", PALAVRA_PASSE, "tipo", "ADMINISTRADOR"))
                .when().post("/api/auth/login")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void jsonMalFormadoDevolveValidacao() {
        given().contentType(ContentType.JSON).body("{\"email\": ")
                .when().post("/api/auth/login")
                .then()
                .statusCode(400)
                .body("codigo", equalTo("VALIDACAO"));
    }
}
