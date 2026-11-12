package pt.esmad.matriz.tarefas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** Responsáveis de uma tarefa (Ficha PL 03, Exercício 6; US05 crit. 4 e 7; RN11, RI02, CE8). */
@QuarkusTest
class ResponsaveisApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private Conta carla;
    private String projeto;
    private String tarefa;

    @BeforeEach
    void criarTarefa() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        carla = contas.membro("Carla Exemplo");
        projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
        adicionarMembro(bruno);
        adicionarMembro(carla);
        tarefa = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(201).extract().path("id");
    }

    private void adicionarMembro(Conta quem) {
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", quem.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(201);
    }

    private Response atribuir(Conta quem, Object utilizadorId) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", String.valueOf(utilizadorId)))
                .when().post("/api/tarefas/" + tarefa + "/responsaveis");
    }

    private Response remover(Conta quem, Object utilizadorId) {
        return given().cookie(COOKIE, quem.token())
                .when().delete("/api/tarefas/" + tarefa + "/responsaveis/" + utilizadorId);
    }

    private Response consultar() {
        return given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa);
    }

    private long historico(String anterior, String novo, UUID autor) {
        return contas.contar("""
                SELECT count(*) FROM historico_tarefa
                WHERE tarefa_id = ? AND campo = 'responsavel'
                  AND valor_anterior IS NOT DISTINCT FROM ? AND valor_novo IS NOT DISTINCT FROM ?
                  AND autor_id = ?
                """, UUID.fromString(tarefa), anterior, novo, autor);
    }

    private static void assertValidacao(Response resposta) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("utilizadorId"));
    }

    // Atribuir (US05 crit. 4)

    @Test
    void qualquerMembroAtribuiVariosResponsaveisQueFicamOrdenadosPeloNome() {
        atribuir(bruno, carla.id()).then().statusCode(201)
                .body("responsaveis.nome", contains("Carla Exemplo"))
                .body("versao", equalTo(1));
        atribuir(ana, ana.id()).then().statusCode(201)
                .body("responsaveis.nome", contains("Ana Exemplo", "Carla Exemplo"))
                .body("versao", equalTo(2));

        assertEquals(1, historico(null, "Carla Exemplo", bruno.id()));
        assertEquals(1, historico(null, "Ana Exemplo", ana.id()));
    }

    @Test
    void aMesmaPessoaNaoFicaDuasVezes() {
        atribuir(ana, bruno.id()).then().statusCode(201);

        atribuir(ana, bruno.id()).then().statusCode(409)
                .body("codigo", equalTo("JA_EXISTE"))
                .body("mensagem", equalTo("Esta pessoa já é responsável pela tarefa."));
        assertEquals(1, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?",
                UUID.fromString(tarefa)));
    }

    @Test
    void soMembrosDoProjetoComContaAtiva() {
        Conta daniel = contas.membro("Daniel Exemplo");
        UUID desativada = contas.semSessao("Eva Exemplo", "MEMBRO", "DESATIVADO");
        contas.sql("INSERT INTO membro_projeto (projeto_id, utilizador_id) VALUES (?, ?)",
                UUID.fromString(projeto), desativada);

        assertValidacao(atribuir(ana, daniel.id()));
        assertValidacao(atribuir(ana, desativada));
        assertValidacao(atribuir(ana, UUID.randomUUID()));
        assertValidacao(atribuir(ana, "nao-e-um-id"));
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of())
                .when().post("/api/tarefas/" + tarefa + "/responsaveis")
                .then().statusCode(400);
        consultar().then().body("responsaveis", org.hamcrest.Matchers.empty()).body("versao", equalTo(0));
    }

    // Remover (US05 crit. 7)

    @Test
    void removerTiraSoEssaAtribuicao() {
        atribuir(ana, bruno.id()).then().statusCode(201);
        atribuir(ana, carla.id()).then().statusCode(201);

        remover(carla, bruno.id()).then().statusCode(204);

        consultar().then().statusCode(200)
                .body("responsaveis.nome", contains("Carla Exemplo"))
                .body("versao", equalTo(3));
        assertEquals(1, historico("Bruno Exemplo", null, carla.id()));
    }

    @Test
    void removerQuemNaoEResponsavelDa404() {
        remover(ana, bruno.id()).then().statusCode(404);
        remover(ana, "nao-e-um-id").then().statusCode(404);
    }

    // Permissões e persistência

    @Test
    void recusaNaoMembrosAdministradorTarefasInexistentesEProjetosArquivados() {
        Conta daniel = contas.membro("Daniel Exemplo");
        atribuir(daniel, daniel.id()).then().statusCode(403);
        atribuir(contas.administrador(), ana.id()).then().statusCode(403);
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", ana.id().toString()))
                .when().post("/api/tarefas/" + UUID.randomUUID() + "/responsaveis")
                .then().statusCode(404);

        atribuir(ana, bruno.id()).then().statusCode(201);
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));
        atribuir(ana, carla.id()).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        remover(ana, bruno.id()).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
    }

    @Test
    void asAtribuicoesAparecemAoReabrirATarefaENaListagem() {
        atribuir(ana, bruno.id()).then().statusCode(201);
        atribuir(ana, carla.id()).then().statusCode(201);

        consultar().then().body("responsaveis.nome", contains("Bruno Exemplo", "Carla Exemplo"));
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto + "/tarefas")
                .then().body("itens[0].responsaveis.nome", contains("Bruno Exemplo", "Carla Exemplo"));
    }

    @Test
    void depoisDeAtribuirAVersaoAntigaDaTarefaERecusada() {
        atribuir(ana, bruno.id()).then().statusCode(201);

        given().cookie(COOKIE, carla.token()).contentType(ContentType.JSON)
                .body(Map.of("versao", 0, "titulo", "Rever tudo"))
                .when().patch("/api/tarefas/" + tarefa)
                .then().statusCode(409).body("codigo", equalTo("VERSAO_DESATUALIZADA"));
    }
}
