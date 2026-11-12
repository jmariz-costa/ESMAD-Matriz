package pt.esmad.matriz.tarefas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** Editar, mudar o estado e eliminar tarefas (US05 crit. 6 e 8, US06, US09; D08, RI09, RI12). */
@QuarkusTest
class EditarEliminarTarefaApiTest {

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
        tarefa = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("titulo", "Rever contrato", "descricao", "Cláusulas 3 e 4.",
                        "prazo", "2026-11-20", "prioridade", "MEDIA"))
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(201).extract().path("id");
    }

    private UUID tarefaId() {
        return UUID.fromString(tarefa);
    }

    private Response editar(Conta quem, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().patch("/api/tarefas/" + tarefa);
    }

    private Response eliminar(Conta quem) {
        return given().cookie(COOKIE, quem.token()).when().delete("/api/tarefas/" + tarefa);
    }

    private void adicionarMembro(Conta quem) {
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", quem.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(201);
    }

    private long entradasDeAlteracao() {
        return contas.contar("SELECT count(*) FROM historico_tarefa WHERE tarefa_id = ? AND tipo = 'ALTERACAO'",
                tarefaId());
    }

    private long entrada(String campo, String anterior, String novo, UUID autor) {
        return contas.contar("""
                SELECT count(*) FROM historico_tarefa
                WHERE tarefa_id = ? AND tipo = 'ALTERACAO' AND campo = ?
                  AND valor_anterior IS NOT DISTINCT FROM ? AND valor_novo IS NOT DISTINCT FROM ?
                  AND autor_id = ?
                """, tarefaId(), campo, anterior, novo, autor);
    }

    private static void assertValidacao(Response resposta, String campo) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem(campo));
    }

    // Editar (US05 crit. 6)

    @Test
    void editarAlteraSoOsCamposEnviadosAumentaAVersaoERegistaCadaCampo() {
        editar(ana, Map.of("versao", 0, "titulo", "  Rever contrato final ", "prioridade", "ALTA"))
                .then().statusCode(200)
                .body("titulo", equalTo("Rever contrato final"))
                .body("prioridade", equalTo("ALTA"))
                .body("descricao", equalTo("Cláusulas 3 e 4."))
                .body("prazo", equalTo("2026-11-20"))
                .body("versao", equalTo(1));

        assertEquals(2, entradasDeAlteracao());
        assertEquals(1, entrada("titulo", "Rever contrato", "Rever contrato final", ana.id()));
        assertEquals(1, entrada("prioridade", "MEDIA", "ALTA", ana.id()));
        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().body("titulo", equalTo("Rever contrato final")).body("versao", equalTo(1));
    }

    @Test
    void enviarNullLimpaOsCamposOpcionais() {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("versao", 0);
        corpo.put("descricao", null);
        corpo.put("prazo", null);
        corpo.put("prioridade", null);

        editar(ana, corpo).then().statusCode(200)
                .body("descricao", nullValue())
                .body("prazo", nullValue())
                .body("prioridade", nullValue());
        assertEquals(1, entrada("prazo", "2026-11-20", null, ana.id()));
    }

    @Test
    void qualquerMembroEditaEFicaComoAutorNoHistorico() {
        adicionarMembro(bruno);

        editar(bruno, Map.of("versao", 0, "prazo", "2020-01-01")).then().statusCode(200)
                .body("prazo", equalTo("2020-01-01"));
        assertEquals(1, entrada("prazo", "2026-11-20", "2020-01-01", bruno.id()));
    }

    @Test
    void semAlteracoesReaisAVersaoMantemSeENaoHaHistorico() {
        editar(ana, Map.of("versao", 0, "titulo", " Rever contrato ", "prioridade", "MEDIA"))
                .then().statusCode(200).body("versao", equalTo(0));
        assertEquals(0, entradasDeAlteracao());
    }

    // Mudar o estado (US06)

    @Test
    void qualquerEstadoPassaAQualquerOutroEOHistoricoRegistaOAnteriorEONovo() {
        editar(ana, Map.of("versao", 0, "estado", "CONCLUIDA")).then().statusCode(200)
                .body("estado", equalTo("CONCLUIDA")).body("versao", equalTo(1));
        editar(ana, Map.of("versao", 1, "estado", "A_FAZER")).then().statusCode(200)
                .body("estado", equalTo("A_FAZER")).body("versao", equalTo(2));
        editar(ana, Map.of("versao", 2, "estado", "EM_REVISAO")).then().statusCode(200);

        assertEquals(1, entrada("estado", "A_FAZER", "CONCLUIDA", ana.id()));
        assertEquals(1, entrada("estado", "CONCLUIDA", "A_FAZER", ana.id()));
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto + "/tarefas?estado=EM_REVISAO")
                .then().body("itens.titulo", contains("Rever contrato"));
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto)
                .then().body("contagens.EM_REVISAO", equalTo(1)).body("contagens.A_FAZER", equalTo(0));
    }

    // Versão (D08, US05 crit. 8)

    @Test
    void versaoAntigaERecusadaENadaMuda() {
        editar(ana, Map.of("versao", 0, "titulo", "Primeira alteração")).then().statusCode(200);

        editar(ana, Map.of("versao", 0, "titulo", "Alteração com versão antiga"))
                .then().statusCode(409)
                .body("codigo", equalTo("VERSAO_DESATUALIZADA"))
                .body("mensagem", equalTo("A tarefa foi alterada por outra pessoa. Recarregue-a antes de guardar."));
        editar(ana, Map.of("versao", 7, "estado", "CONCLUIDA")).then().statusCode(409);

        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().body("titulo", equalTo("Primeira alteração"))
                .body("estado", equalTo("A_FAZER"))
                .body("versao", equalTo(1));
        assertEquals(1, entradasDeAlteracao());
    }

    // Validação

    @Test
    void recusaPedidosInvalidosSemAlterarATarefa() {
        Map<String, Object> tituloNull = new HashMap<>();
        tituloNull.put("versao", 0);
        tituloNull.put("titulo", null);
        assertValidacao(editar(ana, tituloNull), "titulo");
        assertValidacao(editar(ana, Map.of("versao", 0, "titulo", "    ")), "titulo");
        assertValidacao(editar(ana, Map.of("versao", 0, "titulo", "x".repeat(151))), "titulo");
        assertValidacao(editar(ana, Map.of("versao", 0, "descricao", "x".repeat(2001))), "descricao");
        assertValidacao(editar(ana, Map.of("titulo", "Sem versão")), "versao");
        assertValidacao(editar(ana, Map.of("versao", -1, "titulo", "Versão negativa")), "versao");
        assertValidacao(editar(ana, Map.of("versao", "0", "titulo", "Versão em texto")), "versao");
        assertValidacao(editar(ana, Map.of("versao", 0)), "corpo");
        assertValidacao(editar(ana, Map.of("versao", 0, "estado", "FEITA")), "estado");
        assertValidacao(editar(ana, Map.of("versao", 0, "prazo", "2026-13-01")), "prazo");
        assertValidacao(editar(ana, Map.of("versao", 0, "projetoId", UUID.randomUUID().toString())), "projetoId");

        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().body("titulo", equalTo("Rever contrato")).body("versao", equalTo(0));
        assertEquals(0, entradasDeAlteracao());
    }

    // Permissões

    @Test
    void editarRecusaNaoMembrosAdministradorETarefasInexistentes() {
        editar(bruno, Map.of("versao", 0, "estado", "EM_CURSO")).then().statusCode(403);
        editar(contas.administrador(), Map.of("versao", 0, "estado", "EM_CURSO")).then().statusCode(403);
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("versao", 0, "estado", "EM_CURSO"))
                .when().patch("/api/tarefas/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    void projetoArquivadoRecusaEditarMudarEstadoEEliminar() {
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));

        editar(ana, Map.of("versao", 0, "estado", "EM_CURSO"))
                .then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        eliminar(ana).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
    }

    // Eliminar (US09)

    @Test
    void gestorEliminaATarefaComTudoOQueELaContemEFicaORegisto() {
        adicionarMembro(bruno);
        contas.sql("INSERT INTO atribuicao_tarefa (tarefa_id, utilizador_id) VALUES (?, ?)", tarefaId(), bruno.id());
        contas.sql("INSERT INTO comentario (id, tarefa_id, autor_id, texto, criado_em) VALUES (?, ?, ?, 'Ok', now())",
                UUID.randomUUID(), tarefaId(), bruno.id());

        eliminar(ana).then().statusCode(204);

        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa).then().statusCode(404);
        assertEquals(0, contas.contar("SELECT count(*) FROM historico_tarefa WHERE tarefa_id = ?", tarefaId()));
        assertEquals(0, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?", tarefaId()));
        assertEquals(0, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?", tarefaId()));
        assertEquals(1, contas.contar("""
                SELECT count(*) FROM tarefa_eliminada
                WHERE projeto_id = ? AND titulo = 'Rever contrato' AND eliminada_por = ? AND eliminada_em IS NOT NULL
                """, UUID.fromString(projeto), ana.id()));
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto)
                .then().body("contagens.A_FAZER", equalTo(0));
    }

    @Test
    void soOGestorElimina() {
        adicionarMembro(bruno);

        eliminar(bruno).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        eliminar(contas.administrador()).then().statusCode(403);
        given().cookie(COOKIE, ana.token()).when().delete("/api/tarefas/" + UUID.randomUUID())
                .then().statusCode(404);

        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa).then().statusCode(200);
        assertEquals(0, contas.contar("SELECT count(*) FROM tarefa_eliminada WHERE projeto_id = ?",
                UUID.fromString(projeto)));
    }
}
