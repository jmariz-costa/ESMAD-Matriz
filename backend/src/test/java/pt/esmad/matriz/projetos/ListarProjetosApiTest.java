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

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** GET /projetos: os meus projetos, pesquisa e paginação (RN20, US17, US18). */
@QuarkusTest
@Tag("integracao")
class ListarProjetosApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;

    @BeforeEach
    void criarConta() {
        ana = contas.membro("Ana Exemplo");
    }

    private static String criarProjeto(Conta conta, String nome) {
        return given().cookie(COOKIE, conta.token()).contentType(ContentType.JSON).body(Map.of("nome", nome))
                .when().post("/api/projetos")
                .then().statusCode(201)
                .extract().path("id");
    }

    /** Parâmetros aos pares: nome, valor, nome, valor… (codificados pelo RestAssured). */
    private static Response listar(Conta conta, String... parametros) {
        var pedido = given().cookie(COOKIE, conta.token());
        for (int i = 0; i < parametros.length; i += 2) {
            pedido = pedido.queryParam(parametros[i], parametros[i + 1]);
        }
        return pedido.when().get("/api/projetos");
    }

    private static void assertValidacao(Response resposta, String campo) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem(campo));
    }

    @Test
    void soMostraOsProjetosDeQueSouMembro() {
        Conta bruno = contas.membro("Bruno Exemplo");
        criarProjeto(ana, "Projeto da Ana");
        criarProjeto(bruno, "Projeto do Bruno");
        String partilhado = criarProjeto(bruno, "Projeto partilhado");
        contas.sql("INSERT INTO membro_projeto (projeto_id, utilizador_id) VALUES (?, ?)",
                UUID.fromString(partilhado), ana.id());

        listar(ana).then().statusCode(200)
                .body("total", equalTo(2))
                .body("itens.nome", contains("Projeto da Ana", "Projeto partilhado"))
                .body("itens.souGestor", contains(true, false))
                .body("itens.gestor.nome", contains("Ana Exemplo", "Bruno Exemplo"));
    }

    @Test
    void naoMostraProjetosArquivados() {
        criarProjeto(ana, "Ativo");
        String arquivado = criarProjeto(ana, "Arquivado");
        contas.sql("UPDATE projeto SET arquivado = TRUE WHERE id = ?", UUID.fromString(arquivado));

        listar(ana).then().statusCode(200)
                .body("total", equalTo(1))
                .body("itens.nome", contains("Ativo"));
    }

    @Test
    void ordenadosPeloNomeSemDistinguirMaiusculas() {
        criarProjeto(ana, "beta");
        criarProjeto(ana, "Alfa");
        criarProjeto(ana, "Gama");

        listar(ana).then().body("itens.nome", contains("Alfa", "beta", "Gama"));
    }

    @Test
    void pesquisaPeloNomeSemDistinguirMaiusculas() {
        criarProjeto(ana, "Portal do Cliente");
        criarProjeto(ana, "Website institucional");
        criarProjeto(ana, "Novo PORTAL interno");

        listar(ana, "q", "portal").then().statusCode(200)
                .body("total", equalTo(2))
                .body("itens.nome", contains("Novo PORTAL interno", "Portal do Cliente"));
        listar(ana, "q", "  ").then().body("total", equalTo(3)); // só espaços: sem filtro
        listar(ana, "q", "inexistente").then().body("total", equalTo(0)).body("itens", empty());
    }

    @Test
    void caracteresEspeciaisDaPesquisaSaoTexto() {
        criarProjeto(ana, "Taxa 100% cumprida");
        criarProjeto(ana, "Outro projeto");

        listar(ana, "q", "%").then().body("itens.nome", contains("Taxa 100% cumprida"));
        listar(ana, "q", "_").then().body("total", equalTo(0));
    }

    @Test
    void paginacao() {
        for (String nome : new String[] {"P1", "P2", "P3", "P4", "P5"}) {
            criarProjeto(ana, nome + " projeto");
        }

        listar(ana).then()
                .body("pagina", equalTo(1))
                .body("tamanho", equalTo(20))
                .body("total", equalTo(5));
        listar(ana, "pagina", "2", "tamanho", "2").then().statusCode(200)
                .body("pagina", equalTo(2))
                .body("tamanho", equalTo(2))
                .body("total", equalTo(5))
                .body("itens.nome", contains("P3 projeto", "P4 projeto"));
        listar(ana, "pagina", "4", "tamanho", "2").then().statusCode(200)
                .body("total", equalTo(5))
                .body("itens", empty());
    }

    @Test
    void parametrosDePaginacaoInvalidos() {
        assertValidacao(listar(ana, "pagina", "0"), "pagina");
        assertValidacao(listar(ana, "pagina", "abc"), "pagina");
        assertValidacao(listar(ana, "tamanho", "0"), "tamanho");
        assertValidacao(listar(ana, "tamanho", "101"), "tamanho");
        listar(ana, "tamanho", "100").then().statusCode(200);
    }

    @Test
    void contagensDeCadaProjeto() {
        String comTarefas = criarProjeto(ana, "A com tarefas");
        criarProjeto(ana, "B sem tarefas");
        for (String estado : new String[] {"EM_REVISAO", "CONCLUIDA", "CONCLUIDA"}) {
            contas.sql("INSERT INTO tarefa (id, projeto_id, titulo, estado) VALUES (?, ?, 'Tarefa', ?)",
                    UUID.randomUUID(), UUID.fromString(comTarefas), estado);
        }

        listar(ana).then()
                .body("itens[0].contagens.EM_REVISAO", equalTo(1))
                .body("itens[0].contagens.CONCLUIDA", equalTo(2))
                .body("itens[1].contagens.A_FAZER", equalTo(0))
                .body("itens[1].contagens.CONCLUIDA", equalTo(0));
    }

    @Test
    void administradorRecebe403ESemSessao401() {
        listar(contas.administrador()).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        given().when().get("/api/projetos").then().statusCode(401);
    }
}
