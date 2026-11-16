package pt.esmad.matriz.comentarios;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** Comentários de uma tarefa (Ficha PL 03, Exercício 6; US12 crit. 1 e 2). */
@QuarkusTest
@Tag("integracao")
class ComentariosApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private String projeto;
    private String tarefa;

    @BeforeEach
    void criarTarefa() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", bruno.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(201);
        tarefa = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(201).extract().path("id");
    }

    private Response comentar(Conta quem, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/tarefas/" + tarefa + "/comentarios");
    }

    private Response listar(Conta quem, String query) {
        return given().cookie(COOKIE, quem.token()).when().get("/api/tarefas/" + tarefa + "/comentarios" + query);
    }

    private static void assertValidacao(Response resposta) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("texto"));
    }

    @Test
    void comentarioFicaComOAutorDaSessaoEAData() {
        Instant antes = Instant.now().minus(1, ChronoUnit.SECONDS);

        Response resposta = comentar(bruno, Map.of("texto", "  Já enviei os slides.  "));

        resposta.then().statusCode(201)
                .body("tarefaId", equalTo(tarefa))
                .body("autor.id", equalTo(bruno.id().toString()))
                .body("autor.nome", equalTo("Bruno Exemplo"))
                .body("texto", equalTo("Já enviei os slides."))
                .body("criadoEm", notNullValue())
                .body("editadoEm", nullValue());
        Instant criadoEm = Instant.parse(resposta.path("criadoEm"));
        assertTrue(criadoEm.isAfter(antes) && criadoEm.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void oPedidoNaoPodeDefinirOAutorNemAData() {
        comentar(bruno, Map.of("texto", "Olá", "autor", Map.of("id", ana.id().toString())))
                .then().statusCode(400).body("codigo", equalTo("VALIDACAO"));
        comentar(bruno, Map.of("texto", "Olá", "criadoEm", "2020-01-01T00:00:00Z"))
                .then().statusCode(400);
        assertEquals(0, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?", UUID.fromString(tarefa)));
    }

    @Test
    void recusaComentarioVazioSoComEspacosOuComMaisDe2000Caracteres() {
        assertValidacao(comentar(ana, Map.of("texto", "")));
        assertValidacao(comentar(ana, Map.of("texto", "    ")));
        assertValidacao(comentar(ana, Map.of()));
        assertValidacao(comentar(ana, Map.of("texto", "x".repeat(2001))));
        comentar(ana, Map.of("texto", "x")).then().statusCode(201);
        comentar(ana, Map.of("texto", " " + "x".repeat(2000) + " ")).then().statusCode(201);
    }

    @Test
    void listaDoMaisAntigoParaOMaisRecenteComPaginacao() {
        comentar(ana, Map.of("texto", "Primeiro")).then().statusCode(201);
        comentar(bruno, Map.of("texto", "Segundo")).then().statusCode(201);
        comentar(ana, Map.of("texto", "Terceiro")).then().statusCode(201);

        listar(bruno, "").then().statusCode(200)
                .body("total", equalTo(3))
                .body("itens.texto", contains("Primeiro", "Segundo", "Terceiro"))
                .body("itens.autor.nome", contains("Ana Exemplo", "Bruno Exemplo", "Ana Exemplo"));
        listar(bruno, "?tamanho=2&pagina=2").then()
                .body("total", equalTo(3))
                .body("itens.texto", contains("Terceiro"));
        listar(bruno, "?pagina=0").then().statusCode(400);
    }

    @Test
    void osComentariosContinuamDisponiveisAoReabrirATarefa() {
        String id = comentar(ana, Map.of("texto", "Fica guardado")).then().statusCode(201).extract().path("id");

        listar(ana, "").then().body("itens.id", contains(id));
        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa).then().statusCode(200);
        listar(ana, "").then().body("itens.texto", contains("Fica guardado"));
    }

    @Test
    void comentarNaoAlteraAVersaoDaTarefa() {
        comentar(ana, Map.of("texto", "Olá")).then().statusCode(201);

        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().body("versao", equalTo(0));
    }

    @Test
    void recusaNaoMembrosAdministradorETarefasInexistentes() {
        Conta daniel = contas.membro("Daniel Exemplo");

        comentar(daniel, Map.of("texto", "Olá")).then().statusCode(403);
        listar(daniel, "").then().statusCode(403);
        comentar(contas.administrador(), Map.of("texto", "Olá")).then().statusCode(403);
        listar(contas.administrador(), "").then().statusCode(403);
        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + UUID.randomUUID() + "/comentarios")
                .then().statusCode(404);
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("texto", "Olá"))
                .when().post("/api/tarefas/nao-e-um-id/comentarios")
                .then().statusCode(404);
    }

    @Test
    void projetoArquivadoRecusaComentarMasDeixaLer() {
        comentar(ana, Map.of("texto", "Antes de arquivar")).then().statusCode(201);
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));

        comentar(ana, Map.of("texto", "Depois")).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        listar(ana, "").then().statusCode(200).body("itens.texto", contains("Antes de arquivar"));
    }
}
