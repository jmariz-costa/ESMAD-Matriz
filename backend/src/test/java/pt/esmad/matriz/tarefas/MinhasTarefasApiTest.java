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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * "As minhas tarefas" (US11): a Ana é Gestora do Portal e do Website; o Bruno é membro do Portal.
 * Cada teste usa contas novas, por isso só vê as tarefas que ele próprio cria.
 */
@QuarkusTest
@Tag("integracao")
class MinhasTarefasApiTest {

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private String portal;
    private String website;

    @BeforeEach
    void criarProjetos() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        portal = criarProjeto("Portal do Cliente");
        website = criarProjeto("Website institucional");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", bruno.id().toString()))
                .when().post("/api/projetos/" + portal + "/membros")
                .then().statusCode(201);
    }

    private String criarProjeto(String nome) {
        return given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", nome))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
    }

    /** Cria uma tarefa (pela Ana) e atribui-a às pessoas indicadas. */
    private String tarefa(String projeto, String titulo, String prazo, Conta... responsaveis) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("prazo", prazo);
        String id = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(corpo)
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(201).extract().path("id");
        for (Conta responsavel : responsaveis) {
            given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                    .body(Map.of("utilizadorId", responsavel.id().toString()))
                    .when().post("/api/tarefas/" + id + "/responsaveis")
                    .then().statusCode(201);
        }
        return id;
    }

    private void mudarEstado(String tarefa, String estado) {
        int versao = given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().statusCode(200).extract().path("versao");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("versao", versao, "estado", estado))
                .when().patch("/api/tarefas/" + tarefa)
                .then().statusCode(200);
    }

    /** Conclui a tarefa pela API e fixa a data da conclusão no histórico, para a ordem ser previsível. */
    private void concluirEm(String tarefa, String dataHora) {
        mudarEstado(tarefa, "CONCLUIDA");
        contas.sql("""
                UPDATE historico_tarefa SET data_hora = ?
                WHERE tarefa_id = ? AND campo = 'estado' AND valor_novo = 'CONCLUIDA'
                """, Timestamp.from(Instant.parse(dataHora)), UUID.fromString(tarefa));
    }

    private Response minhas(Conta quem, String query) {
        return given().cookie(COOKIE, quem.token()).when().get("/api/minhas-tarefas" + query);
    }

    // Por concluir (crit. 1 a 4)

    @Test
    void mostraAsNaoConcluidasAtribuidasAMimDeTodosOsProjetosComProjetoEEstado() {
        String minha = tarefa(portal, "Definir requisitos", "2026-12-01", ana);
        tarefa(website, "Mapa do site", "2026-12-10", ana);
        tarefa(portal, "Escolher fotografias", null, ana, bruno);
        tarefa(portal, "Do Bruno", null, bruno);
        tarefa(website, "Sem responsável", null);
        mudarEstado(minha, "EM_CURSO");

        minhas(ana, "").then().statusCode(200)
                .body("total", equalTo(3))
                .body("itens.titulo", contains("Definir requisitos", "Mapa do site", "Escolher fotografias"))
                .body("itens[0].estado", equalTo("EM_CURSO"))
                .body("itens[0].projetoId", equalTo(portal))
                .body("itens[0].projeto.id", equalTo(portal))
                .body("itens[0].projeto.nome", equalTo("Portal do Cliente"))
                .body("itens[0].concluidaEm", nullValue())
                .body("itens[1].estado", equalTo("A_FAZER"))
                .body("itens[1].projeto.nome", equalTo("Website institucional"))
                .body("itens[2].responsaveis.nome", contains("Ana Exemplo", "Bruno Exemplo"));
    }

    @Test
    void naoMostraConcluidasNemTarefasDeProjetosArquivados() {
        String concluida = tarefa(portal, "Já feita", null, ana);
        tarefa(website, "Do arquivado", null, ana);
        tarefa(portal, "Por fazer", null, ana);
        mudarEstado(concluida, "CONCLUIDA");
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(website));

        minhas(ana, "").then().statusCode(200)
                .body("total", equalTo(1))
                .body("itens.titulo", contains("Por fazer"));
        minhas(ana, "?concluidas=false").then().body("itens.titulo", contains("Por fazer"));
    }

    @Test
    void ordenaPorPrazoComAsSemPrazoNoFimEDepoisPeloTitulo() {
        tarefa(portal, "Sem prazo B", null, ana);
        tarefa(portal, "Sem prazo a", null, ana);
        tarefa(website, "Mais tarde", "2026-12-20", ana);
        tarefa(portal, "Mais cedo", "2026-11-20", ana);
        tarefa(website, "Atrasada", "2020-01-31", ana);

        minhas(ana, "").then().statusCode(200)
                .body("itens.titulo", contains("Atrasada", "Mais cedo", "Mais tarde", "Sem prazo a", "Sem prazo B"));
    }

    @Test
    void semTarefasOuSemProjetosAListaVemVazia() {
        tarefa(portal, "Do Bruno", null, bruno);
        minhas(ana, "").then().statusCode(200).body("itens", empty()).body("total", equalTo(0));

        Conta carla = contas.membro("Carla Exemplo");
        minhas(carla, "").then().statusCode(200).body("itens", empty()).body("total", equalTo(0));
        minhas(carla, "?concluidas=true").then().statusCode(200).body("itens", empty()).body("total", equalTo(0));
    }

    @Test
    void paginaAsNaoConcluidas() {
        tarefa(portal, "Tarefa 1", "2026-12-01", ana);
        tarefa(portal, "Tarefa 2", "2026-12-02", ana);
        tarefa(portal, "Tarefa 3", "2026-12-03", ana);

        minhas(ana, "?pagina=2&tamanho=2").then().statusCode(200)
                .body("total", equalTo(3))
                .body("pagina", equalTo(2))
                .body("tamanho", equalTo(2))
                .body("itens.titulo", contains("Tarefa 3"));
    }

    // Concluídas (crit. 5)

    @Test
    void concluidasVemDaMaisRecenteParaAMaisAntigaComADataDeConclusao() {
        String antiga = tarefa(portal, "Reunião de arranque", null, ana);
        String recente = tarefa(website, "Definir paleta", null, ana);
        String reaberta = tarefa(portal, "Reaberta", null, ana);
        tarefa(portal, "Por fazer", null, ana);
        concluirEm(antiga, "2026-11-02T10:00:00Z");
        concluirEm(recente, "2026-11-10T15:30:00Z");
        concluirEm(reaberta, "2026-11-12T09:00:00Z");
        mudarEstado(reaberta, "EM_CURSO");

        minhas(ana, "?concluidas=true").then().statusCode(200)
                .body("total", equalTo(2))
                .body("itens.titulo", contains("Definir paleta", "Reunião de arranque"))
                .body("itens[0].estado", equalTo("CONCLUIDA"))
                .body("itens[0].projeto.nome", equalTo("Website institucional"))
                .body("itens[0].concluidaEm", equalTo("2026-11-10T15:30:00Z"))
                .body("itens[1].concluidaEm", equalTo("2026-11-02T10:00:00Z"));
    }

    @Test
    void usaAUltimaConclusaoDeUmaTarefaConcluidaMaisDoQueUmaVez() {
        String tarefa = tarefa(portal, "Concluída duas vezes", null, ana);
        concluirEm(tarefa, "2026-11-01T10:00:00Z");
        mudarEstado(tarefa, "EM_REVISAO");
        mudarEstado(tarefa, "CONCLUIDA");

        Instant ultima = Instant.parse(minhas(ana, "?concluidas=true").then().statusCode(200)
                .extract().path("itens[0].concluidaEm"));
        assertTrue(ultima.isAfter(Instant.parse("2026-11-01T10:00:00Z")));
    }

    @Test
    void concluidasSemEntradaNoHistoricoVemNoFimSemData() {
        String comData = tarefa(portal, "Com data", null, ana);
        concluirEm(comData, "2026-11-05T10:00:00Z");
        // Como nos dados de demonstração: tarefa concluída sem a passagem a CONCLUIDA no histórico
        String b = tarefa(portal, "Sem data B", null, ana);
        String a = tarefa(website, "Sem data a", null, ana);
        contas.sql("UPDATE tarefa SET estado = 'CONCLUIDA' WHERE id IN (?, ?)", UUID.fromString(a), UUID.fromString(b));

        minhas(ana, "?concluidas=true").then().statusCode(200)
                .body("itens.titulo", contains("Com data", "Sem data a", "Sem data B"))
                .body("itens[1].concluidaEm", nullValue());
    }

    @Test
    void concluidasNaoMostraProjetosArquivadosNemTarefasDeOutros() {
        String doArquivado = tarefa(website, "Do arquivado", null, ana);
        String doBruno = tarefa(portal, "Do Bruno", null, bruno);
        String minha = tarefa(portal, "Minha", null, ana);
        concluirEm(doArquivado, "2026-11-05T10:00:00Z");
        concluirEm(doBruno, "2026-11-05T10:00:00Z");
        concluirEm(minha, "2026-11-04T10:00:00Z");
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(website));

        minhas(ana, "?concluidas=true").then().statusCode(200).body("itens.titulo", contains("Minha"));
        minhas(bruno, "?concluidas=true").then().statusCode(200).body("itens.titulo", contains("Do Bruno"));
    }

    @Test
    void paginaAsConcluidasIncluindoUmaPaginaAlemDoFim() {
        concluirEm(tarefa(portal, "Primeira", null, ana), "2026-11-01T10:00:00Z");
        concluirEm(tarefa(portal, "Segunda", null, ana), "2026-11-02T10:00:00Z");
        concluirEm(tarefa(portal, "Terceira", null, ana), "2026-11-03T10:00:00Z");

        minhas(ana, "?concluidas=true&pagina=2&tamanho=2").then().statusCode(200)
                .body("total", equalTo(3))
                .body("itens.titulo", contains("Primeira"));
        minhas(ana, "?concluidas=true&pagina=5&tamanho=2").then().statusCode(200)
                .body("total", equalTo(3))
                .body("itens", hasSize(0));
    }

    // Acesso e validação

    @Test
    void recusaParametrosInvalidos() {
        minhas(ana, "?concluidas=sim").then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("concluidas"));
        minhas(ana, "?pagina=0").then().statusCode(400).body("detalhes.campo", hasItem("pagina"));
        minhas(ana, "?tamanho=101").then().statusCode(400).body("detalhes.campo", hasItem("tamanho"));
    }

    @Test
    void oAdministradorRecebe403ESemSessao401() {
        Conta admin = contas.administrador();
        minhas(admin, "").then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        minhas(admin, "?concluidas=true").then().statusCode(403);
        given().when().get("/api/minhas-tarefas").then().statusCode(401).body("codigo", equalTo("NAO_AUTENTICADO"));
    }
}
