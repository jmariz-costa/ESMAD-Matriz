package pt.esmad.matriz.projetos;

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

/** Membros de um projeto (US04, RN07, RN08, RI01, RI04, RI16). */
@QuarkusTest
class MembrosApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private String projeto;

    @BeforeEach
    void criarProjeto() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
    }

    private UUID id() {
        return UUID.fromString(projeto);
    }

    private Response adicionar(Conta quem, Object utilizadorId) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", String.valueOf(utilizadorId)))
                .when().post("/api/projetos/" + projeto + "/membros");
    }

    private Response remover(Conta quem, Object utilizadorId) {
        return given().cookie(COOKIE, quem.token())
                .when().delete("/api/projetos/" + projeto + "/membros/" + utilizadorId);
    }

    private Response listar(Conta quem) {
        return given().cookie(COOKIE, quem.token()).when().get("/api/projetos/" + projeto + "/membros");
    }

    private static void assertValidacao(Response resposta) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("utilizadorId"));
    }

    private UUID tarefa(String estado) {
        UUID tarefa = UUID.randomUUID();
        contas.sql("INSERT INTO tarefa (id, projeto_id, titulo, estado) VALUES (?, ?, 'Tarefa', ?)", tarefa, id(), estado);
        return tarefa;
    }

    private void atribuir(UUID tarefa, UUID utilizador) {
        contas.sql("INSERT INTO atribuicao_tarefa (tarefa_id, utilizador_id) VALUES (?, ?)", tarefa, utilizador);
    }

    private long atribuicoes(UUID tarefa, UUID utilizador) {
        return contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ? AND utilizador_id = ?",
                tarefa, utilizador);
    }

    // Adicionar (US04 crit. 1 e 3)

    @Test
    void gestorAdicionaUmMembroQuePassaAVerOProjeto() {
        given().cookie(COOKIE, bruno.token()).when().get("/api/projetos/" + projeto).then().statusCode(403);

        adicionar(ana, bruno.id()).then().statusCode(201)
                .body("id", equalTo(bruno.id().toString()))
                .body("nome", equalTo("Bruno Exemplo"))
                .body("email", equalTo(bruno.email()))
                .body("ativo", equalTo(true))
                .body("gestor", equalTo(false));

        given().cookie(COOKIE, bruno.token()).when().get("/api/projetos/" + projeto)
                .then().statusCode(200).body("souGestor", equalTo(false));
        given().cookie(COOKIE, bruno.token()).when().get("/api/projetos")
                .then().body("itens.id", hasItem(projeto));
    }

    @Test
    void adicionarDuasVezesDevolve409() {
        adicionar(ana, bruno.id()).then().statusCode(201);
        adicionar(ana, bruno.id()).then().statusCode(409).body("codigo", equalTo("JA_EXISTE"));
        adicionar(ana, ana.id()).then().statusCode(409).body("codigo", equalTo("JA_EXISTE"));
        assertEquals(1, contas.contar("SELECT count(*) FROM membro_projeto WHERE projeto_id = ? AND utilizador_id = ?",
                id(), bruno.id()));
    }

    @Test
    void soMembrosAtivosPodemSerAdicionados() {
        assertValidacao(adicionar(ana, contas.administrador().id()));
        assertValidacao(adicionar(ana, contas.semSessao("Desativada", "MEMBRO", "DESATIVADO")));
        assertValidacao(adicionar(ana, contas.semSessao("Por confirmar", "MEMBRO", "POR_CONFIRMAR")));
        assertValidacao(adicionar(ana, UUID.randomUUID()));
        assertValidacao(adicionar(ana, "nao-e-uuid"));
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body("{}")
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(400).body("detalhes.campo", hasItem("utilizadorId"));
    }

    @Test
    void soOGestorAdiciona() {
        Conta carla = contas.membro("Carla Exemplo");
        adicionar(ana, bruno.id()).then().statusCode(201);

        adicionar(bruno, carla.id()).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        adicionar(carla, carla.id()).then().statusCode(403);
        adicionar(contas.administrador(), carla.id()).then().statusCode(403);
    }

    // Listar

    @Test
    void listaOsMembrosOrdenadosComGestorEContaAtiva() {
        UUID desativada = contas.semSessao("Carla Exemplo", "MEMBRO", "ATIVO");
        adicionar(ana, bruno.id()).then().statusCode(201);
        adicionar(ana, desativada).then().statusCode(201);
        contas.sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE id = ?", desativada);

        listar(bruno).then().statusCode(200)
                .body("nome", contains("Ana Exemplo", "Bruno Exemplo", "Carla Exemplo"))
                .body("gestor", contains(true, false, false))
                .body("ativo", contains(true, true, false));
    }

    @Test
    void quemNaoEMembroNaoVeOsMembros() {
        listar(bruno).then().statusCode(403);
        listar(contas.administrador()).then().statusCode(403);
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + UUID.randomUUID() + "/membros")
                .then().statusCode(404);
    }

    // Remover (US04 crit. 4 e 5)

    @Test
    void removerTiraAsAtribuicoesDasTarefasNaoConcluidasEOAcesso() {
        Conta carla = contas.membro("Carla Exemplo");
        adicionar(ana, bruno.id()).then().statusCode(201);
        adicionar(ana, carla.id()).then().statusCode(201);
        UUID aFazer = tarefa("A_FAZER");
        UUID emRevisao = tarefa("EM_REVISAO");
        UUID concluida = tarefa("CONCLUIDA");
        atribuir(aFazer, bruno.id());
        atribuir(emRevisao, bruno.id());
        atribuir(concluida, bruno.id());
        atribuir(aFazer, carla.id());

        remover(ana, bruno.id()).then().statusCode(204);

        assertEquals(0, atribuicoes(aFazer, bruno.id()));
        assertEquals(0, atribuicoes(emRevisao, bruno.id()));
        assertEquals(1, atribuicoes(concluida, bruno.id()), "nas tarefas concluídas continua a constar");
        assertEquals(1, atribuicoes(aFazer, carla.id()), "as atribuições dos outros mantêm-se");
        given().cookie(COOKIE, bruno.token()).when().get("/api/projetos/" + projeto).then().statusCode(403);
        listar(ana).then().body("nome", contains("Ana Exemplo", "Carla Exemplo"));

        // As tarefas afetadas aumentam a versão (D08) e registam a remoção, com o Gestor como autor (RI12)
        assertEquals(1, contas.contar("SELECT versao FROM tarefa WHERE id = ?", aFazer));
        assertEquals(0, contas.contar("SELECT versao FROM tarefa WHERE id = ?", concluida));
        assertEquals(2, contas.contar("""
                SELECT count(*) FROM historico_tarefa
                WHERE tarefa_id IN (?, ?) AND campo = 'responsavel'
                  AND valor_anterior = 'Bruno Exemplo' AND valor_novo IS NULL AND autor_id = ?
                """, aFazer, emRevisao, ana.id()));
        assertEquals(0, contas.contar("SELECT count(*) FROM historico_tarefa WHERE tarefa_id = ?", concluida));
    }

    @Test
    void oGestorNaoPodeSerRemovido() {
        remover(ana, ana.id()).then().statusCode(409).body("codigo", equalTo("GESTOR_NAO_REMOVIVEL"));
        listar(ana).then().body("nome", contains("Ana Exemplo"));
    }

    @Test
    void removerQuemNaoEMembroDevolve404() {
        remover(ana, bruno.id()).then().statusCode(404);
        remover(ana, "nao-e-uuid").then().statusCode(404);
    }

    @Test
    void soOGestorRemove() {
        Conta carla = contas.membro("Carla Exemplo");
        adicionar(ana, bruno.id()).then().statusCode(201);
        adicionar(ana, carla.id()).then().statusCode(201);

        remover(bruno, carla.id()).then().statusCode(403);
        remover(contas.administrador(), carla.id()).then().statusCode(403);
        listar(ana).then().body("nome", contains("Ana Exemplo", "Bruno Exemplo", "Carla Exemplo"));
    }

    // Projeto arquivado (RI10)

    @Test
    void numProjetoArquivadoNaoSeAdicionaNemRemove() {
        Conta carla = contas.membro("Carla Exemplo");
        adicionar(ana, bruno.id()).then().statusCode(201);
        contas.sql("UPDATE projeto SET arquivado = TRUE WHERE id = ?", id());

        adicionar(ana, carla.id()).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        adicionar(ana, UUID.randomUUID()).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        remover(ana, bruno.id()).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
        listar(bruno).then().statusCode(200);
    }
}
