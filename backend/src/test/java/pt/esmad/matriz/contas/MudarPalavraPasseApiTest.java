package pt.esmad.matriz.contas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;
import static pt.esmad.matriz.ContasDeTeste.PALAVRA_PASSE;

/** Mudar a própria palavra-passe (US20, PR21). */
@QuarkusTest
@Tag("integracao")
class MudarPalavraPasseApiTest {

    private static final String NOVA = "palavra-passe-nova";

    @Inject
    ContasDeTeste contas;

    private Conta ana;

    @BeforeEach
    void criarConta() {
        ana = contas.membro("Ana Exemplo");
    }

    private Response mudar(String token, Object corpo) {
        return given().cookie(COOKIE, token).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/auth/palavra-passe");
    }

    private Response entrar(String palavraPasse) {
        return given().contentType(ContentType.JSON).body(Map.of("email", ana.email(), "palavraPasse", palavraPasse))
                .when().post("/api/auth/login");
    }

    @Test
    void passaAEntrarComANovaETerminaAsOutrasSessoesMantendoAAtual() {
        String outraSessao = entrar(PALAVRA_PASSE).then().statusCode(200).extract().cookie(COOKIE);

        mudar(ana.token(), Map.of("palavraPasseAtual", PALAVRA_PASSE, "palavraPasseNova", NOVA)).then().statusCode(204);

        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().statusCode(200);
        given().cookie(COOKIE, outraSessao).when().get("/api/auth/me").then().statusCode(401);
        entrar(PALAVRA_PASSE).then().statusCode(401);
        entrar(NOVA).then().statusCode(200);
    }

    @Test
    void recusaAAtualErradaUmaNovaIgualOuCurta() {
        mudar(ana.token(), Map.of("palavraPasseAtual", "errada-123", "palavraPasseNova", NOVA)).then().statusCode(400)
                .body("detalhes.campo", hasItem("palavraPasseAtual"))
                .body("detalhes.mensagem", hasItem("A palavra-passe atual está errada."));
        mudar(ana.token(), Map.of("palavraPasseAtual", PALAVRA_PASSE, "palavraPasseNova", PALAVRA_PASSE))
                .then().statusCode(400)
                .body("detalhes.mensagem", hasItem("A palavra-passe nova tem de ser diferente da atual."));
        mudar(ana.token(), Map.of("palavraPasseAtual", PALAVRA_PASSE, "palavraPasseNova", "1234567"))
                .then().statusCode(400).body("detalhes.campo", hasItem("palavraPasseNova"));
        mudar(ana.token(), Map.of("palavraPasseNova", NOVA)).then().statusCode(400)
                .body("detalhes.campo", hasItem("palavraPasseAtual"));
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .when().post("/api/auth/palavra-passe").then().statusCode(400);

        entrar(PALAVRA_PASSE).then().statusCode(200);
    }

    @Test
    void oAdministradorNaoMudaAPalavraPasseNaMatrizESemSessaoDa401() {
        Conta admin = contas.administrador();
        mudar(admin.token(), Map.of("palavraPasseAtual", PALAVRA_PASSE, "palavraPasseNova", NOVA))
                .then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        given().contentType(ContentType.JSON).body(Map.of("palavraPasseAtual", PALAVRA_PASSE, "palavraPasseNova", NOVA))
                .when().post("/api/auth/palavra-passe").then().statusCode(401);
    }
}
