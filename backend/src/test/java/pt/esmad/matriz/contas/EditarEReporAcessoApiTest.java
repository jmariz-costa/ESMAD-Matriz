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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;
import static pt.esmad.matriz.ContasDeTeste.PALAVRA_PASSE;

/** Editar o nome e repor o acesso de uma conta (US19), pelo Administrador. */
@QuarkusTest
@Tag("integracao")
class EditarEReporAcessoApiTest {

    private static final String TEMPORARIA = "temporaria-123";

    @Inject
    ContasDeTeste contas;

    private Conta admin;
    private Conta ana;

    @BeforeEach
    void criarContas() {
        admin = contas.administrador();
        ana = contas.membro("Ana Exemplo");
    }

    private Response editar(Conta quem, Object conta, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().patch("/api/contas/" + conta);
    }

    private Response repor(Conta quem, Object conta, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/contas/" + conta + "/reposicao-acesso");
    }

    private Response entrar(String email, String palavraPasse) {
        return given().contentType(ContentType.JSON).body(Map.of("email", email, "palavraPasse", palavraPasse))
                .when().post("/api/auth/login");
    }

    // Editar o nome (crit. 1)

    @Test
    void oAdministradorAlteraONomeQueApareceNaMatriz() {
        String projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos").then().statusCode(201).extract().path("id");

        editar(admin, ana.id(), Map.of("nome", "  Ana Silva  ")).then().statusCode(200)
                .body("id", equalTo(ana.id().toString()))
                .body("nome", equalTo("Ana Silva"))
                .body("email", equalTo(ana.email()))
                .body("estado", equalTo("ATIVO"));

        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto + "/membros")
                .then().statusCode(200).body("nome", hasItem("Ana Silva"));
        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().body("nome", equalTo("Ana Silva"));
    }

    @Test
    void recusaNomesForaDosLimitesEOEmailNaoSeAltera() {
        editar(admin, ana.id(), Map.of("nome", " A ")).then().statusCode(400).body("detalhes.campo", hasItem("nome"));
        editar(admin, ana.id(), Map.of("nome", "x".repeat(151))).then().statusCode(400);
        editar(admin, ana.id(), Map.of("nome", 123)).then().statusCode(400);
        editar(admin, ana.id(), Map.of()).then().statusCode(400).body("detalhes.campo", hasItem("nome"));
        // O email não faz parte do pedido: um campo desconhecido é recusado
        editar(admin, ana.id(), Map.of("nome", "Ana Silva", "email", "outra@empresa.pt")).then().statusCode(400);
        editar(admin, ana.id(), Map.of("nome", "Al")).then().statusCode(200);

        assertEquals(1, contas.contar("SELECT count(*) FROM utilizador WHERE id = ? AND email = ? AND nome = 'Al'",
                ana.id(), ana.email()));
    }

    // Repor o acesso (crit. 2 e 3)

    @Test
    void reporOAcessoTrocaAPalavraPasseETerminaAsSessoes() {
        repor(admin, ana.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(204);

        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().statusCode(401);
        entrar(ana.email(), PALAVRA_PASSE).then().statusCode(401).body("codigo", equalTo("CREDENCIAIS_INVALIDAS"));
        entrar(ana.email(), TEMPORARIA).then().statusCode(200).body("deveMudarPalavraPasse", equalTo(true));
    }

    @Test
    void reporOAcessoLimpaOBloqueioPorTentativasFalhadas() {
        contas.sql("UPDATE utilizador SET bloqueado_ate = ? WHERE id = ?",
                Timestamp.from(Instant.now().plus(15, ChronoUnit.MINUTES)), ana.id());

        repor(admin, ana.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(204);

        entrar(ana.email(), TEMPORARIA).then().statusCode(200);
    }

    @Test
    void comAPalavraPasseTemporariaSoSePodeMudalaConsultarASessaoESair() {
        repor(admin, ana.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(204);
        String token = entrar(ana.email(), TEMPORARIA).then().statusCode(200).extract().cookie(COOKIE);

        given().cookie(COOKIE, token).when().get("/api/projetos").then().statusCode(403)
                .body("codigo", equalTo("PALAVRA_PASSE_TEMPORARIA"));
        given().cookie(COOKIE, token).when().get("/api/minhas-tarefas").then().statusCode(403);
        given().cookie(COOKIE, token).contentType(ContentType.JSON).body(Map.of("nome", "Novo"))
                .when().post("/api/projetos").then().statusCode(403);
        given().cookie(COOKIE, token).when().get("/api/auth/me").then().statusCode(200)
                .body("deveMudarPalavraPasse", equalTo(true));

        given().cookie(COOKIE, token).contentType(ContentType.JSON)
                .body(Map.of("palavraPasseAtual", TEMPORARIA, "palavraPasseNova", "a-minha-nova-1"))
                .when().post("/api/auth/palavra-passe").then().statusCode(204);

        given().cookie(COOKIE, token).when().get("/api/projetos").then().statusCode(200);
        given().cookie(COOKIE, token).when().get("/api/auth/me").then().body("deveMudarPalavraPasse", equalTo(false));
        given().cookie(COOKIE, token).when().post("/api/auth/logout").then().statusCode(204);
        entrar(ana.email(), "a-minha-nova-1").then().statusCode(200).body("deveMudarPalavraPasse", equalTo(false));
    }

    @Test
    void recusaPalavraPasseTemporariaCurtaEContaDesativada() {
        repor(admin, ana.id(), Map.of("palavraPasseTemporaria", "1234567")).then().statusCode(400)
                .body("detalhes.campo", hasItem("palavraPasseTemporaria"));
        repor(admin, ana.id(), Map.of()).then().statusCode(400);

        contas.sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE id = ?", ana.id());
        repor(admin, ana.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(409)
                .body("codigo", equalTo("CONTA_DESATIVADA"));
        assertEquals(0, contas.contar(
                "SELECT count(*) FROM utilizador WHERE id = ? AND deve_mudar_palavra_passe", ana.id()));
    }

    // Acesso (crit. 4)

    @Test
    void soOAdministradorEditaERepoeESoEmContasDeMembro() {
        Conta bruno = contas.membro("Bruno Exemplo");
        Conta outroAdmin = contas.administrador();

        editar(bruno, ana.id(), Map.of("nome", "Tomado")).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        repor(bruno, ana.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(403);
        editar(admin, outroAdmin.id(), Map.of("nome", "Outro")).then().statusCode(403);
        repor(admin, admin.id(), Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(403);
        editar(admin, UUID.randomUUID(), Map.of("nome", "Ninguém")).then().statusCode(404);
        repor(admin, "nao-e-um-id", Map.of("palavraPasseTemporaria", TEMPORARIA)).then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of("nome", "X"))
                .when().patch("/api/contas/" + ana.id()).then().statusCode(401);

        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().statusCode(200)
                .body("nome", equalTo("Ana Exemplo"));
    }
}
