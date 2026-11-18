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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * Criar contas de Membro pelo Administrador (US21). Nos testes, o domínio configurado é
 * empresa.pt (application.properties, %test).
 */
@QuarkusTest
@Tag("integracao")
class CriarContaApiTest {

    private static final String TEMPORARIA = "temporaria-123";

    @Inject
    ContasDeTeste contas;

    private Conta admin;
    private String email;

    @BeforeEach
    void preparar() {
        admin = contas.administrador();
        email = "eva-" + UUID.randomUUID() + "@empresa.pt";
    }

    private static Map<String, Object> pedido(String nome, String email, String palavraPasse) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("email", email);
        corpo.put("palavraPasseTemporaria", palavraPasse);
        return corpo;
    }

    private Response criar(Conta quem, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/contas");
    }

    private Response entrar(String email, String palavraPasse) {
        return given().contentType(ContentType.JSON).body(Map.of("email", email, "palavraPasse", palavraPasse))
                .when().post("/api/auth/login");
    }

    private long contasComEmail(String email) {
        return contas.contar("SELECT count(*) FROM utilizador WHERE lower(email) = lower(?)", email);
    }

    // Crit. 1

    @Test
    void oAdministradorCriaUmMembroAtivoQueApareceNaLista() {
        String id = criar(admin, pedido("  Eva Rocha  ", "  " + email.toUpperCase() + " ", TEMPORARIA))
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("nome", equalTo("Eva Rocha"))
                .body("email", equalTo(email))
                .body("estado", equalTo("ATIVO"))
                .extract().path("id");

        given().cookie(COOKIE, admin.token()).queryParam("q", email).when().get("/api/contas")
                .then().statusCode(200).body("itens.id", hasItem(id));
        assertEquals(1, contas.contar("""
                SELECT count(*) FROM utilizador
                WHERE id = ? AND tipo = 'MEMBRO' AND deve_mudar_palavra_passe AND password_hash <> ?
                """, UUID.fromString(id), TEMPORARIA));
    }

    // Crit. 4 (PR20)

    @Test
    void noPrimeiroLoginAPessoaTemDeDefinirUmaPalavraPasseNova() {
        criar(admin, pedido("Eva Rocha", email, TEMPORARIA)).then().statusCode(201);

        String token = entrar(email, TEMPORARIA).then().statusCode(200)
                .body("deveMudarPalavraPasse", equalTo(true)).extract().cookie(COOKIE);
        given().cookie(COOKIE, token).when().get("/api/projetos").then().statusCode(403)
                .body("codigo", equalTo("PALAVRA_PASSE_TEMPORARIA"));

        given().cookie(COOKIE, token).contentType(ContentType.JSON)
                .body(Map.of("palavraPasseAtual", TEMPORARIA, "palavraPasseNova", "a-minha-nova-1"))
                .when().post("/api/auth/palavra-passe").then().statusCode(204);

        given().cookie(COOKIE, token).when().get("/api/projetos").then().statusCode(200);
        entrar(email, "a-minha-nova-1").then().statusCode(200).body("deveMudarPalavraPasse", equalTo(false));
    }

    // Validação (crit. 1 e 2)

    @Test
    void recusaDadosInvalidosSemCriarConta() {
        criar(admin, pedido(" A ", "sem-arroba", "1234567")).then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", containsInAnyOrder("nome", "email", "palavraPasseTemporaria"));
        criar(admin, Map.of()).then().statusCode(400)
                .body("detalhes.campo", containsInAnyOrder("nome", "email", "palavraPasseTemporaria"));
        criar(admin, Map.of("nome", 123, "email", email, "palavraPasseTemporaria", TEMPORARIA)).then().statusCode(400);
        // Campos desconhecidos são recusados (additionalProperties: false): o tipo não se escolhe
        Map<String, Object> comTipo = pedido("Eva Rocha", email, TEMPORARIA);
        comTipo.put("tipo", "ADMINISTRADOR");
        criar(admin, comTipo).then().statusCode(400);
        given().cookie(COOKIE, admin.token()).contentType(ContentType.JSON)
                .when().post("/api/contas").then().statusCode(400);

        assertEquals(0, contasComEmail(email));
    }

    @Test
    void recusaEmailsDeOutroDominio() {
        String deFora = "eva-" + UUID.randomUUID() + "@outra.pt";

        criar(admin, pedido("Eva Rocha", deFora, TEMPORARIA)).then().statusCode(400)
                .body("detalhes.campo", equalTo(List.of("email")))
                .body("detalhes.mensagem", hasItem("O email tem de ser do domínio empresa.pt."));

        assertEquals(0, contasComEmail(deFora));
    }

    // Crit. 3

    @Test
    void recusaUmEmailQueJaTemContaSemDistinguirMaiusculas() {
        Conta ana = contas.membro("Ana Exemplo");
        UUID desativada = contas.semSessao("Desativada", "MEMBRO", "DESATIVADO");
        String emailDesativada = "conta-" + desativada + "@empresa.pt";

        for (String existente : List.of(ana.email().toUpperCase(), emailDesativada, admin.email())) {
            criar(admin, pedido("Outra Pessoa", existente, TEMPORARIA)).then().statusCode(409)
                    .body("codigo", equalTo("JA_EXISTE"))
                    .body("mensagem", equalTo("Este email já tem conta."));
            assertEquals(1, contasComEmail(existente), existente);
        }
        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().statusCode(200)
                .body("nome", equalTo("Ana Exemplo"));
    }

    // Crit. 5

    @Test
    void soOAdministradorCriaContas() {
        Conta ana = contas.membro("Ana Exemplo");

        criar(ana, pedido("Eva Rocha", email, TEMPORARIA)).then().statusCode(403)
                .body("codigo", equalTo("SEM_PERMISSAO"));
        given().contentType(ContentType.JSON).body(pedido("Eva Rocha", email, TEMPORARIA))
                .when().post("/api/contas").then().statusCode(401);

        assertEquals(0, contasComEmail(email));
    }
}
