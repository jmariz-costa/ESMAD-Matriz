package pt.esmad.matriz;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * Caso 08 da Ficha PL 03, Exercício 8: um utilizador autenticado não consegue aceder nem alterar
 * recursos de um projeto de que não é membro, mesmo pondo no pedido os identificadores certos
 * (S03, RN20). A Ana é Gestora do projeto A; o projeto B é do Bruno, com a Carla como membro.
 */
@QuarkusTest
class AutorizacaoApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private Conta carla;
    private String projetoA;
    private String tarefaA;
    private String projetoB;
    private String tarefaB;

    @BeforeEach
    void criarDoisProjetos() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        carla = contas.membro("Carla Exemplo");
        projetoA = criar(ana, "/api/projetos", Map.of("nome", "Projeto A"));
        tarefaA = criar(ana, "/api/projetos/" + projetoA + "/tarefas", Map.of("titulo", "Tarefa A"));
        projetoB = criar(bruno, "/api/projetos", Map.of("nome", "Projeto B"));
        given().cookie(COOKIE, bruno.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", carla.id().toString()))
                .when().post("/api/projetos/" + projetoB + "/membros").then().statusCode(201);
        tarefaB = criar(bruno, "/api/projetos/" + projetoB + "/tarefas", Map.of("titulo", "Tarefa B"));
        given().cookie(COOKIE, bruno.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", carla.id().toString()))
                .when().post("/api/tarefas/" + tarefaB + "/responsaveis").then().statusCode(201);
        criar(bruno, "/api/tarefas/" + tarefaB + "/comentarios", Map.of("texto", "Do projeto B"));
    }

    private String criar(Conta quem, String caminho, Map<String, ?> corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().post(caminho)
                .then().statusCode(201).extract().path("id");
    }

    private Response pedido(String metodo, String caminho, Object corpo) {
        var especificacao = given().cookie(COOKIE, ana.token());
        if (corpo != null) {
            especificacao = especificacao.contentType(ContentType.JSON).body(corpo);
        }
        return especificacao.when().request(metodo, caminho);
    }

    @Test
    void aAnaNaoAcedeNemAlteraNadaDoProjetoBComOsIdentificadoresDele() {
        String b = "/api/projetos/" + projetoB;
        String t = "/api/tarefas/" + tarefaB;
        Object[][] tentativas = {
                {"GET", b, null},
                {"PATCH", b, Map.of("nome", "Tomado pela Ana")},
                {"DELETE", b, null},
                {"GET", b + "/membros", null},
                {"POST", b + "/membros", Map.of("utilizadorId", ana.id().toString())},
                {"DELETE", b + "/membros/" + carla.id(), null},
                {"GET", b + "/tarefas", null},
                {"POST", b + "/tarefas", Map.of("titulo", "Tarefa da Ana")},
                {"GET", t, null},
                {"PATCH", t, Map.of("versao", 1, "estado", "CONCLUIDA")},
                {"DELETE", t, null},
                {"POST", t + "/responsaveis", Map.of("utilizadorId", ana.id().toString())},
                {"DELETE", t + "/responsaveis/" + carla.id(), null},
                {"GET", t + "/comentarios", null},
                {"POST", t + "/comentarios", Map.of("texto", "Comentário da Ana")},
        };
        for (Object[] tentativa : tentativas) {
            pedido((String) tentativa[0], (String) tentativa[1], tentativa[2])
                    .then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        }

        // O projeto B ficou exatamente como estava
        given().cookie(COOKIE, bruno.token()).when().get(b).then().statusCode(200).body("nome", equalTo("Projeto B"));
        given().cookie(COOKIE, bruno.token()).when().get(t).then().statusCode(200)
                .body("estado", equalTo("A_FAZER"))
                .body("versao", equalTo(1))
                .body("responsaveis.nome", hasItem("Carla Exemplo"));
        UUID idB = UUID.fromString(projetoB);
        UUID idTarefaB = UUID.fromString(tarefaB);
        assertEquals(1, contas.contar("SELECT count(*) FROM tarefa WHERE projeto_id = ?", idB));
        assertEquals(2, contas.contar("SELECT count(*) FROM membro_projeto WHERE projeto_id = ?", idB));
        assertEquals(1, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?", idTarefaB));
        assertEquals(0, contas.contar("SELECT count(*) FROM tarefa_eliminada WHERE projeto_id = ?", idB));
    }

    @Test
    void oProjetoBNaoApareceNasListagensDaAna() {
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos")
                .then().statusCode(200).body("itens.id", not(hasItem(projetoB)));
    }

    @Test
    void misturarIdentificadoresDeProjetosDiferentesERecusado() {
        // A Carla é membro do B, não do A: não pode ficar responsável por uma tarefa do A (RI02)
        pedido("POST", "/api/tarefas/" + tarefaA + "/responsaveis", Map.of("utilizadorId", carla.id().toString()))
                .then().statusCode(400).body("detalhes.campo", hasItem("utilizadorId"));
        // Uma tarefa não muda de projeto pelo corpo do pedido
        pedido("PATCH", "/api/tarefas/" + tarefaA, Map.of("versao", 0, "projetoId", projetoB))
                .then().statusCode(400).body("detalhes.campo", hasItem("projetoId"));
        // Remover do A quem só é membro do B
        pedido("DELETE", "/api/projetos/" + projetoA + "/membros/" + carla.id(), null).then().statusCode(404);
        // O autor de um comentário não vem do pedido
        pedido("POST", "/api/tarefas/" + tarefaA + "/comentarios",
                Map.of("texto", "Olá", "autor", Map.of("id", bruno.id().toString())))
                .then().statusCode(400);

        assertEquals(0, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?",
                UUID.fromString(tarefaA)));
        assertEquals(0, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?",
                UUID.fromString(tarefaA)));
        given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefaA)
                .then().body("projetoId", equalTo(projetoA));
    }

    @Test
    void oAdministradorNaoAcedeAosProjetosNemAsTarefas() {
        Conta admin = contas.administrador();
        for (String caminho : new String[] {"/api/projetos", "/api/projetos/" + projetoB, "/api/tarefas/" + tarefaB,
                "/api/tarefas/" + tarefaB + "/comentarios"}) {
            given().cookie(COOKIE, admin.token()).when().get(caminho).then().statusCode(403);
        }
    }

    @Test
    void semSessaoTudoDevolve401() {
        for (String caminho : new String[] {"/api/projetos", "/api/projetos/" + projetoB, "/api/tarefas/" + tarefaB,
                "/api/tarefas/" + tarefaB + "/comentarios", "/api/utilizadores"}) {
            given().when().get(caminho).then().statusCode(401).body("codigo", equalTo("NAO_AUTENTICADO"));
        }
    }
}
