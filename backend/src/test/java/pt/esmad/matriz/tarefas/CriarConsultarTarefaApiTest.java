package pt.esmad.matriz.tarefas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * Criar uma tarefa e consultá-la depois pela API, confirmando o que ficou na base de dados
 * (US05 crit. 1–3 e 6, RN10, RN23). Ficha PL 04, Exercício 3; cenários T02, T04 e T09 do
 * docs/test-plan.md.
 *
 * <p>Isolamento: a base é o PostgreSQL descartável dos Dev Services, e cada teste cria contas
 * e um projeto novos, por isso não depende de dados nem de execuções anteriores.
 */
@QuarkusTest
class CriarConsultarTarefaApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private String projeto;

    @BeforeEach
    void criarProjeto() {
        ana = contas.membro("Ana Exemplo");
        projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
    }

    private Response criar(Object corpo) {
        return given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/projetos/" + projeto + "/tarefas");
    }

    private Response consultar(Object tarefa) {
        return given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa);
    }

    private long tarefasDoProjeto() {
        return contas.contar("SELECT count(*) FROM tarefa WHERE projeto_id = ?", UUID.fromString(projeto));
    }

    @Test
    void criarEConsultarDevolveOsMesmosDadosQueFicaramNaBaseDeDados() {
        // Arrange
        Map<String, Object> corpo = Map.of(
                "titulo", "  Rever contrato  ",
                "descricao", "Cláusulas 3 e 4.",
                "prazo", "2026-12-31",
                "prioridade", "ALTA");

        // Act
        Response criada = criar(corpo);
        String id = criada.then().statusCode(201).extract().path("id");
        Response consultada = consultar(id);

        // Assert: a consulta devolve o que a criação devolveu (RN10; os espaços nas pontas não contam, RN23)
        consultada.then().statusCode(200)
                .body("id", equalTo(id))
                .body("projetoId", equalTo(projeto))
                .body("titulo", equalTo("Rever contrato"))
                .body("descricao", equalTo("Cláusulas 3 e 4."))
                .body("prazo", equalTo("2026-12-31"))
                .body("prioridade", equalTo("ALTA"))
                .body("estado", equalTo("A_FAZER"))
                .body("responsaveis", empty())
                .body("versao", equalTo(0));
        for (String campo : List.of("titulo", "descricao", "prazo", "prioridade", "estado", "versao")) {
            assertEquals((Object) criada.path(campo), consultada.path(campo), campo);
        }

        // Assert: e é isso que está guardado na tabela tarefa
        assertEquals(1, contas.contar("""
                SELECT count(*) FROM tarefa
                WHERE id = ? AND projeto_id = ? AND titulo = ? AND descricao = ? AND prazo = ?
                  AND prioridade = 'ALTA' AND estado = 'A_FAZER' AND versao = 0
                """, UUID.fromString(id), UUID.fromString(projeto), "Rever contrato", "Cláusulas 3 e 4.",
                LocalDate.of(2026, 12, 31)));
    }

    @Test
    void tarefaSoComTituloGuardaOsOpcionaisComoNulos() {
        String id = criar(Map.of("titulo", "Preparar reunião")).then().statusCode(201).extract().path("id");

        consultar(id).then().statusCode(200)
                .body("descricao", nullValue())
                .body("prazo", nullValue())
                .body("prioridade", nullValue());
        assertEquals(1, contas.contar("""
                SELECT count(*) FROM tarefa
                WHERE id = ? AND descricao IS NULL AND prazo IS NULL AND prioridade IS NULL
                """, UUID.fromString(id)));
    }

    @Test
    void tituloInvalidoERecusadoENaoGravaNada() {
        Map<String, Object> semTitulo = new HashMap<>();
        semTitulo.put("descricao", "Sem título");
        List<Map<String, Object>> invalidos = List.of(
                semTitulo,
                Map.of("titulo", ""),
                Map.of("titulo", "    "),
                Map.of("titulo", " ab "),
                Map.of("titulo", "x".repeat(151)));

        for (Map<String, Object> corpo : invalidos) {
            criar(corpo).then().statusCode(400)
                    .body("codigo", equalTo("VALIDACAO"))
                    .body("detalhes.campo", hasItem("titulo"))
                    .header("Location", nullValue());
        }

        assertEquals(0, tarefasDoProjeto());
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(200).body("total", equalTo(0));
    }

    @Test
    void tarefaInexistenteDevolve404() {
        String existente = criar(Map.of("titulo", "Rever contrato")).then().statusCode(201).extract().path("id");

        consultar(UUID.randomUUID()).then().statusCode(404).body("codigo", equalTo("NAO_ENCONTRADO"));
        consultar("nao-e-um-id").then().statusCode(404).body("codigo", equalTo("NAO_ENCONTRADO"));
        // A tarefa que existe continua a ser encontrada: o 404 não vem de uma falha geral
        consultar(existente).then().statusCode(200);
    }
}
