package pt.esmad.matriz.projetos;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** PATCH e DELETE /projetos/{id} (US16, RN22, RI10, RI17). */
@QuarkusTest
class EditarEliminarProjetoApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private String projeto;

    @BeforeEach
    void criarProjeto() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("nome", "Portal do Cliente", "descricao", "Área reservada."))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
        contas.sql("INSERT INTO membro_projeto (projeto_id, utilizador_id) VALUES (?, ?)", id(), bruno.id());
    }

    private UUID id() {
        return UUID.fromString(projeto);
    }

    private Response editar(Conta conta, String json) {
        return given().cookie(COOKIE, conta.token()).contentType(ContentType.JSON).body(json)
                .when().patch("/api/projetos/" + projeto);
    }

    private Response eliminar(Conta conta) {
        return given().cookie(COOKIE, conta.token()).when().delete("/api/projetos/" + projeto);
    }

    private void arquivar() {
        contas.sql("UPDATE projeto SET arquivado = TRUE WHERE id = ?", id());
    }

    private static void assertValidacao(Response resposta, String campo) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem(campo));
    }

    // Editar

    @Test
    void gestorAlteraSoOsCamposEnviados() {
        editar(ana, "{\"nome\": \"  Portal 2.0  \"}").then().statusCode(200)
                .body("nome", equalTo("Portal 2.0"))
                .body("descricao", equalTo("Área reservada."));

        editar(ana, "{\"descricao\": \"Nova descrição\"}").then().statusCode(200)
                .body("nome", equalTo("Portal 2.0"))
                .body("descricao", equalTo("Nova descrição"));

        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto)
                .then().body("nome", equalTo("Portal 2.0")).body("descricao", equalTo("Nova descrição"));
    }

    @Test
    void descricaoNullLimpaADescricao() {
        editar(ana, "{\"descricao\": null}").then().statusCode(200)
                .body("descricao", nullValue())
                .body("nome", equalTo("Portal do Cliente"));
    }

    @Test
    void validaOPedidoDeEdicao() {
        assertValidacao(editar(ana, "{}"), "corpo");
        assertValidacao(editar(ana, "[]"), "corpo");
        assertValidacao(editar(ana, "{\"nome\": null}"), "nome");
        assertValidacao(editar(ana, "{\"nome\": \"ab\"}"), "nome");
        assertValidacao(editar(ana, "{\"nome\": 123}"), "nome");
        assertValidacao(editar(ana, "{\"descricao\": \"" + "d".repeat(1001) + "\"}"), "descricao");
        assertValidacao(editar(ana, "{\"nome\": \"Válido\", \"gestorId\": \"" + bruno.id() + "\"}"), "gestorId");
        editar(ana, "{\"nome\": ").then().statusCode(400).body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void soOGestorEdita() {
        editar(bruno, "{\"nome\": \"Tentativa do Bruno\"}").then().statusCode(403)
                .body("codigo", equalTo("SEM_PERMISSAO"));
        Conta carla = contas.membro("Carla Exemplo");
        editar(carla, "{\"nome\": \"Tentativa da Carla\"}").then().statusCode(403);
        editar(contas.administrador(), "{\"nome\": \"Tentativa do Admin\"}").then().statusCode(403);
    }

    @Test
    void projetoArquivadoNaoPodeSerEditado() {
        arquivar();
        editar(ana, "{\"nome\": \"Novo nome\"}").then().statusCode(409)
                .body("codigo", equalTo("PROJETO_ARQUIVADO"));
    }

    @Test
    void editarProjetoInexistenteDevolve404() {
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body("{\"nome\": \"Novo\"}")
                .when().patch("/api/projetos/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    // Eliminar

    @Test
    void gestorEliminaEmCascataSemDeixarOrfaos() {
        UUID tarefa = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        contas.sql("INSERT INTO tarefa (id, projeto_id, titulo) VALUES (?, ?, 'Tarefa')", tarefa, id());
        contas.sql("INSERT INTO atribuicao_tarefa (tarefa_id, utilizador_id) VALUES (?, ?)", tarefa, bruno.id());
        contas.sql("INSERT INTO comentario (id, tarefa_id, autor_id, texto, criado_em) VALUES (?, ?, ?, 'Olá', ?)",
                UUID.randomUUID(), tarefa, bruno.id(), agora);
        contas.sql("INSERT INTO historico_tarefa (id, tarefa_id, autor_id, data_hora, tipo) VALUES (?, ?, ?, ?, 'CRIACAO')",
                UUID.randomUUID(), tarefa, ana.id(), agora);
        contas.sql("INSERT INTO tarefa_eliminada (id, projeto_id, titulo, eliminada_por, eliminada_em) VALUES (?, ?, 'Antiga', ?, ?)",
                UUID.randomUUID(), id(), ana.id(), agora);

        eliminar(ana).then().statusCode(204);

        assertEquals(0, contas.contar("SELECT count(*) FROM projeto WHERE id = ?", id()));
        assertEquals(0, contas.contar("SELECT count(*) FROM membro_projeto WHERE projeto_id = ?", id()));
        assertEquals(0, contas.contar("SELECT count(*) FROM tarefa WHERE projeto_id = ?", id()));
        assertEquals(0, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?", tarefa));
        assertEquals(0, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?", tarefa));
        assertEquals(0, contas.contar("SELECT count(*) FROM historico_tarefa WHERE tarefa_id = ?", tarefa));
        assertEquals(0, contas.contar("SELECT count(*) FROM tarefa_eliminada WHERE projeto_id = ?", id()));
        assertEquals(1, contas.contar("SELECT count(*) FROM utilizador WHERE id = ?", bruno.id()),
                "as contas dos membros mantêm-se");
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto).then().statusCode(404);
    }

    @Test
    void soOGestorElimina() {
        eliminar(bruno).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        eliminar(contas.administrador()).then().statusCode(403);
        assertEquals(1, contas.contar("SELECT count(*) FROM projeto WHERE id = ?", id()));
    }

    @Test
    void projetoArquivadoPodeSerEliminado() {
        arquivar();
        eliminar(ana).then().statusCode(204);
        assertEquals(0, contas.contar("SELECT count(*) FROM projeto WHERE id = ?", id()));
    }

    @Test
    void eliminarProjetoInexistenteDevolve404() {
        given().cookie(COOKIE, ana.token()).when().delete("/api/projetos/" + UUID.randomUUID())
                .then().statusCode(404);
        eliminar(ana).then().statusCode(204);
        eliminar(ana).then().statusCode(404);
    }
}
