package pt.esmad.matriz.tarefas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** Criar, consultar e listar tarefas (US05, US07, US17; Ficha PL 03, Exercício 5). */
@QuarkusTest
@Tag("integracao")
class TarefasApiTest {

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

    private Response criar(Conta quem, Object corpo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/projetos/" + projeto + "/tarefas");
    }

    private String criarTarefa(String titulo, String prioridade, String prazo) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("prioridade", prioridade);
        corpo.put("prazo", prazo);
        return criar(ana, corpo).then().statusCode(201).extract().path("id");
    }

    private Response listar(Conta quem, String query) {
        return given().cookie(COOKIE, quem.token())
                .when().get("/api/projetos/" + projeto + "/tarefas" + query);
    }

    private Response consultar(Conta quem, Object tarefa) {
        return given().cookie(COOKIE, quem.token()).when().get("/api/tarefas/" + tarefa);
    }

    private void adicionarMembro(Conta quem) {
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", quem.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(201);
    }

    private static void assertValidacao(Response resposta, String campo) {
        resposta.then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem(campo));
    }

    // Criar (US05 crit. 1, 2, 3 e 5)

    @Test
    void tarefaSoComTituloComecaEmAFazerSemResponsaveisENaVersao0() {
        Response resposta = criar(ana, Map.of("titulo", "  Preparar reunião  "));

        resposta.then().statusCode(201)
                .header("Location", endsWith("/api/tarefas/" + resposta.path("id")))
                .body("projetoId", equalTo(projeto))
                .body("titulo", equalTo("Preparar reunião"))
                .body("descricao", nullValue())
                .body("prazo", nullValue())
                .body("prioridade", nullValue())
                .body("estado", equalTo("A_FAZER"))
                .body("responsaveis", empty())
                .body("versao", equalTo(0));
    }

    @Test
    void criacaoFicaNoHistoricoComQuemTemASessaoComoAutor() {
        String tarefa = criarTarefa("Preparar reunião", null, null);

        assertEquals(1, contas.contar("""
                SELECT count(*) FROM historico_tarefa
                WHERE tarefa_id = ? AND autor_id = ? AND tipo = 'CRIACAO' AND campo IS NULL
                """, UUID.fromString(tarefa), ana.id()));
    }

    @Test
    void aceitaTodosOsCamposOpcionaisEUmPrazoPassado() {
        criar(ana, Map.of("titulo", "Rever contrato", "descricao", "Cláusulas 3 e 4.",
                "prazo", "2020-01-31", "prioridade", "ALTA"))
                .then().statusCode(201)
                .body("descricao", equalTo("Cláusulas 3 e 4."))
                .body("prazo", equalTo("2020-01-31"))
                .body("prioridade", equalTo("ALTA"));
    }

    @Test
    void recusaTituloEmFaltaSoComEspacosOuForaDosLimites() {
        assertValidacao(criar(ana, Map.of("descricao", "Sem título")), "titulo");
        assertValidacao(criar(ana, Map.of("titulo", "    ")), "titulo");
        assertValidacao(criar(ana, Map.of("titulo", " ab ")), "titulo");
        assertValidacao(criar(ana, Map.of("titulo", "x".repeat(151))), "titulo");
        criar(ana, Map.of("titulo", "  " + "x".repeat(150) + "  ")).then().statusCode(201);
    }

    @Test
    void recusaDescricaoComMaisDe2000Caracteres() {
        assertValidacao(criar(ana, Map.of("titulo", "Rever", "descricao", "x".repeat(2001))), "descricao");
        criar(ana, Map.of("titulo", "Rever", "descricao", "x".repeat(2000))).then().statusCode(201);
    }

    @Test
    void recusaPrazoPrioridadeECamposInvalidos() {
        assertValidacao(criar(ana, Map.of("titulo", "Rever", "prazo", "2026-02-30")), "prazo");
        assertValidacao(criar(ana, Map.of("titulo", "Rever", "prazo", "31/01/2026")), "prazo");
        assertValidacao(criar(ana, Map.of("titulo", "Rever", "prioridade", "URGENTE")), "prioridade");
        assertValidacao(criar(ana, Map.of("titulo", 123)), "titulo");
        // O estado não é enviado na criação (PR05)
        assertValidacao(criar(ana, Map.of("titulo", "Rever", "estado", "CONCLUIDA")), "estado");
        assertEquals(0, contas.contar("SELECT count(*) FROM tarefa WHERE projeto_id = ?", UUID.fromString(projeto)));
    }

    @Test
    void soMembrosCriamTarefasEmProjetosNaoArquivados() {
        criar(bruno, Map.of("titulo", "Rever")).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        criar(contas.administrador(), Map.of("titulo", "Rever")).then().statusCode(403);
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + UUID.randomUUID() + "/tarefas")
                .then().statusCode(404);
        given().contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(401);

        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));
        criar(ana, Map.of("titulo", "Rever")).then().statusCode(409).body("codigo", equalTo("PROJETO_ARQUIVADO"));
    }

    // Consultar

    @Test
    void membroConsultaATarefaComOsResponsaveisOrdenadosPeloNome() {
        adicionarMembro(bruno);
        String tarefa = criarTarefa("Rever contrato", "BAIXA", "2026-12-01");
        contas.sql("INSERT INTO atribuicao_tarefa (tarefa_id, utilizador_id) VALUES (?, ?), (?, ?)",
                UUID.fromString(tarefa), bruno.id(), UUID.fromString(tarefa), ana.id());

        consultar(bruno, tarefa).then().statusCode(200)
                .body("titulo", equalTo("Rever contrato"))
                .body("prazo", equalTo("2026-12-01"))
                .body("prioridade", equalTo("BAIXA"))
                .body("responsaveis.nome", contains("Ana Exemplo", "Bruno Exemplo"));
    }

    @Test
    void consultarRecusaNaoMembrosAdministradorETarefasInexistentes() {
        String tarefa = criarTarefa("Rever contrato", null, null);

        consultar(bruno, tarefa).then().statusCode(403);
        consultar(contas.administrador(), tarefa).then().statusCode(403);
        consultar(contas.administrador(), UUID.randomUUID()).then().statusCode(403);
        consultar(ana, UUID.randomUUID()).then().statusCode(404);
        consultar(ana, "nao-e-um-id").then().statusCode(404);
    }

    @Test
    void tarefaDeUmProjetoArquivadoContinuaAPoderSerConsultada() {
        String tarefa = criarTarefa("Rever contrato", null, null);
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));

        consultar(ana, tarefa).then().statusCode(200);
    }

    // Listar (US07 crit. 2, US17 crit. 3 e 4)

    @Test
    void listaOrdenadaPorPrazoComAsSemPrazoNoFimEDepoisPeloTitulo() {
        criarTarefa("Sem prazo B", null, null);
        criarTarefa("Sem prazo A", null, null);
        criarTarefa("Prazo tardio", null, "2026-12-20");
        criarTarefa("Prazo próximo", null, "2026-11-01");
        criarTarefa("Também próximo", null, "2026-11-01");

        listar(ana, "").then().statusCode(200)
                .body("total", equalTo(5))
                .body("itens.titulo", contains("Prazo próximo", "Também próximo", "Prazo tardio",
                        "Sem prazo A", "Sem prazo B"));
    }

    @Test
    void filtrosDeEstadoEPrioridadeCombinam() {
        String alta = criarTarefa("Alta em curso", "ALTA", null);
        criarTarefa("Alta a fazer", "ALTA", null);
        criarTarefa("Baixa a fazer", "BAIXA", null);
        criarTarefa("Sem prioridade", null, null);
        contas.sql("UPDATE tarefa SET estado = 'EM_CURSO' WHERE id = ?", UUID.fromString(alta));

        listar(ana, "?estado=A_FAZER&prioridade=ALTA").then().statusCode(200)
                .body("total", equalTo(1)).body("itens.titulo", contains("Alta a fazer"));
        listar(ana, "?prioridade=ALTA").then()
                .body("total", equalTo(2));
        listar(ana, "?estado=EM_CURSO").then()
                .body("itens.titulo", contains("Alta em curso"));
        listar(ana, "?prioridade=SEM_PRIORIDADE").then()
                .body("itens.titulo", contains("Sem prioridade"));
        listar(ana, "?estado=EM_REVISAO").then()
                .body("total", equalTo(0)).body("itens", empty());
        // Filtros vazios = sem filtro ("Limpar filtros")
        listar(ana, "?estado=&prioridade=").then().body("total", equalTo(4));
    }

    @Test
    void paginacaoComTotalDeTodaAListagem() {
        for (int i = 1; i <= 5; i++) {
            criarTarefa("Tarefa " + i, null, LocalDate.of(2026, 11, i).toString());
        }

        listar(ana, "?tamanho=2&pagina=3").then().statusCode(200)
                .body("total", equalTo(5))
                .body("pagina", equalTo(3))
                .body("tamanho", equalTo(2))
                .body("itens.titulo", contains("Tarefa 5"));
    }

    @Test
    void listarRecusaFiltrosInvalidosENaoMembros() {
        assertValidacao(listar(ana, "?estado=FEITA"), "estado");
        assertValidacao(listar(ana, "?prioridade=URGENTE"), "prioridade");
        assertValidacao(listar(ana, "?tamanho=0"), "tamanho");
        listar(bruno, "").then().statusCode(403);
        listar(contas.administrador(), "").then().statusCode(403);
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + UUID.randomUUID() + "/tarefas")
                .then().statusCode(404);
    }

    @Test
    void listaSoAsTarefasDesteProjeto() {
        criarTarefa("Deste projeto", null, null);
        String outro = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("nome", "Outro")).when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Do outro"))
                .when().post("/api/projetos/" + outro + "/tarefas").then().statusCode(201);

        listar(ana, "").then().body("itens.titulo", contains("Deste projeto"));
    }

    @Test
    void contagensDoProjetoIncluemATarefaCriada() {
        criarTarefa("Rever", null, null);

        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + projeto)
                .then().statusCode(200).body("contagens.A_FAZER", equalTo(1));
    }
}
