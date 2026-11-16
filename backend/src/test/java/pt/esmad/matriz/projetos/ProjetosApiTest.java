package pt.esmad.matriz.projetos;

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
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** POST /projetos e GET /projetos/{id} (US03, RN05, RN19, RN20, RN23, RI01). */
@QuarkusTest
@Tag("integracao")
class ProjetosApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;

    @BeforeEach
    void criarConta() {
        ana = contas.membro("Ana Exemplo");
    }

    private static Response criar(Conta conta, Map<String, ?> corpo) {
        return given().cookie(COOKIE, conta.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/projetos");
    }

    private static String criarProjeto(Conta conta, String nome) {
        return criar(conta, Map.of("nome", nome)).then().statusCode(201).extract().path("id");
    }

    private static Response consultar(Conta conta, String id) {
        return given().cookie(COOKIE, conta.token()).when().get("/api/projetos/" + id);
    }

    private static void assertValidacao(Response resposta, String campo) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem(campo));
    }

    @Test
    void criarDevolve201ComOGestorDaSessao() {
        Response resposta = criar(ana, Map.of("nome", "  Portal do Cliente  ", "descricao", "Área reservada."));

        String id = resposta.then()
                .statusCode(201)
                .header("Location", endsWith("/api/projetos/" + resposta.path("id")))
                .body("nome", equalTo("Portal do Cliente"))
                .body("descricao", equalTo("Área reservada."))
                .body("arquivado", equalTo(false))
                .body("gestor.id", equalTo(ana.id().toString()))
                .body("gestor.nome", equalTo("Ana Exemplo"))
                .body("souGestor", equalTo(true))
                .body("contagens.A_FAZER", equalTo(0))
                .body("contagens.CONCLUIDA", equalTo(0))
                .extract().path("id");

        consultar(ana, id).then().statusCode(200).body("nome", equalTo("Portal do Cliente"));
    }

    @Test
    void descricaoOpcional() {
        criar(ana, Map.of("nome", "Sem descrição")).then().statusCode(201).body("descricao", nullValue());
        criar(ana, Map.of("nome", "Só espaços", "descricao", "   ")).then().statusCode(201)
                .body("descricao", nullValue());
    }

    @Test
    void validaONomeEADescricao() {
        assertValidacao(criar(ana, Map.of("nome", "ab")), "nome");
        assertValidacao(criar(ana, Map.of("nome", "  ab  ")), "nome");
        assertValidacao(criar(ana, Map.of("nome", "a".repeat(101))), "nome");
        assertValidacao(criar(ana, Map.of("descricao", "Sem nome")), "nome");
        assertValidacao(criar(ana, Map.of("nome", "Projeto", "descricao", "d".repeat(1001))), "descricao");
        Map<String, Object> nomeNulo = new HashMap<>();
        nomeNulo.put("nome", null);
        assertValidacao(criar(ana, nomeNulo), "nome");
    }

    @Test
    void naoAceitaOGestorEnviadoPeloCliente() {
        Conta bruno = contas.membro("Bruno Exemplo");
        criar(ana, Map.of("nome", "Projeto", "gestorId", bruno.id().toString()))
                .then().statusCode(400).body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void semSessaoDevolve401() {
        given().contentType(ContentType.JSON).body(Map.of("nome", "Projeto"))
                .when().post("/api/projetos")
                .then().statusCode(401).body("codigo", equalTo("NAO_AUTENTICADO"));
        given().when().get("/api/projetos/" + UUID.randomUUID())
                .then().statusCode(401);
    }

    @Test
    void administradorRecebe403() {
        Conta admin = contas.administrador();
        criar(admin, Map.of("nome", "Projeto do Administrador"))
                .then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));

        String id = criarProjeto(ana, "Projeto da Ana");
        consultar(admin, id).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
    }

    @Test
    void outroUtilizadorNaoConsultaOProjeto() {
        Conta bruno = contas.membro("Bruno Exemplo");
        String id = criarProjeto(ana, "Projeto da Ana");

        consultar(bruno, id).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        consultar(ana, id).then().statusCode(200).body("souGestor", equalTo(true));
    }

    @Test
    void membroQueNaoEGestorVeSouGestorFalso() {
        Conta bruno = contas.membro("Bruno Exemplo");
        String id = criarProjeto(ana, "Projeto partilhado");
        contas.sql("INSERT INTO membro_projeto (projeto_id, utilizador_id) VALUES (?, ?)",
                UUID.fromString(id), bruno.id());

        consultar(bruno, id).then().statusCode(200)
                .body("souGestor", equalTo(false))
                .body("gestor.nome", equalTo("Ana Exemplo"));
    }

    @Test
    void projetoInexistenteOuIdInvalidoDevolve404() {
        consultar(ana, UUID.randomUUID().toString()).then().statusCode(404)
                .body("codigo", equalTo("NAO_ENCONTRADO"));
        consultar(ana, "nao-e-um-uuid").then().statusCode(404)
                .body("codigo", equalTo("NAO_ENCONTRADO"));
    }

    @Test
    void contagensPorEstado() {
        String id = criarProjeto(ana, "Projeto com tarefas");
        UUID projetoId = UUID.fromString(id);
        for (String estado : new String[] {"A_FAZER", "A_FAZER", "EM_CURSO", "CONCLUIDA"}) {
            contas.sql("INSERT INTO tarefa (id, projeto_id, titulo, estado) VALUES (?, ?, 'Tarefa', ?)",
                    UUID.randomUUID(), projetoId, estado);
        }

        consultar(ana, id).then().statusCode(200)
                .body("contagens.A_FAZER", equalTo(2))
                .body("contagens.EM_CURSO", equalTo(1))
                .body("contagens.EM_REVISAO", equalTo(0))
                .body("contagens.CONCLUIDA", equalTo(1));
    }
}
