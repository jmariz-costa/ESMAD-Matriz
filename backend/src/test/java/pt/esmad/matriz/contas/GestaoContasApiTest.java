package pt.esmad.matriz.contas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;
import static pt.esmad.matriz.ContasDeTeste.PALAVRA_PASSE;

/**
 * Gestão de contas pelo Administrador (US14). Os nomes levam um sufixo único, para a pesquisa
 * encontrar só as contas criadas em cada teste (a base de dados é partilhada entre testes).
 */
@QuarkusTest
@Tag("integracao")
class GestaoContasApiTest {

    @Inject
    ContasDeTeste contas;

    private String sufixo;
    private Conta admin;
    private Conta ana;
    private Conta bruno;
    private Conta carla;

    @BeforeEach
    void criarContas() {
        sufixo = UUID.randomUUID().toString().substring(0, 8);
        admin = contas.administrador();
        ana = contas.membro("Ana " + sufixo);
        bruno = contas.membro("Bruno " + sufixo);
        carla = contas.membro("Carla " + sufixo);
    }

    // Auxiliares

    private String criarProjeto(Conta gestor, String nome) {
        return given().cookie(COOKIE, gestor.token()).contentType(ContentType.JSON).body(Map.of("nome", nome))
                .when().post("/api/projetos")
                .then().statusCode(201).extract().path("id");
    }

    private void adicionarMembro(Conta gestor, String projeto, Conta membro) {
        given().cookie(COOKIE, gestor.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", membro.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros")
                .then().statusCode(201);
    }

    private String tarefa(Conta quem, String projeto, String titulo, Conta responsavel) {
        String id = given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON).body(Map.of("titulo", titulo))
                .when().post("/api/projetos/" + projeto + "/tarefas")
                .then().statusCode(201).extract().path("id");
        given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", responsavel.id().toString()))
                .when().post("/api/tarefas/" + id + "/responsaveis")
                .then().statusCode(201);
        return id;
    }

    private Response desativar(Conta quem, Object conta, Object corpo) {
        var pedido = given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON);
        if (corpo != null) {
            pedido = pedido.body(corpo);
        }
        return pedido.when().post("/api/contas/" + conta + "/desativacao");
    }

    private Response reativar(Conta quem, Object conta) {
        return given().cookie(COOKIE, quem.token()).when().post("/api/contas/" + conta + "/reativacao");
    }

    private static Map<String, Object> novosGestores(String projeto, Conta gestor) {
        return Map.of("novosGestores", List.of(Map.of("projetoId", projeto, "gestorId", gestor.id().toString())));
    }

    private void arquivar(String projeto) {
        contas.sql("UPDATE projeto SET arquivado = true WHERE id = ?", UUID.fromString(projeto));
    }

    private void assertGestor(String projeto, Conta gestor) {
        assertEquals(1, contas.contar("SELECT count(*) FROM projeto WHERE id = ? AND gestor_id = ?",
                UUID.fromString(projeto), gestor.id()));
    }

    private boolean eMembro(String projeto, Conta conta) {
        return contas.contar("SELECT count(*) FROM membro_projeto WHERE projeto_id = ? AND utilizador_id = ?",
                UUID.fromString(projeto), conta.id()) == 1;
    }

    private Response entrar(Conta conta) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", conta.email(), "palavraPasse", PALAVRA_PASSE))
                .when().post("/api/auth/login");
    }

    // Listar

    @Test
    void listaSoContasDeMembroOrdenadasPeloNomeComOEstado() {
        contas.sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE id = ?", bruno.id());

        given().cookie(COOKIE, admin.token()).queryParam("q", sufixo)
                .when().get("/api/contas")
                .then().statusCode(200)
                .body("total", equalTo(3))
                .body("itens.nome", contains("Ana " + sufixo, "Bruno " + sufixo, "Carla " + sufixo))
                .body("itens.estado", contains("ATIVO", "DESATIVADO", "ATIVO"))
                .body("itens[0].email", equalTo(ana.email()))
                .body("itens[0].id", equalTo(ana.id().toString()));
        // As contas de Administrador não aparecem (RN21)
        given().cookie(COOKIE, admin.token()).queryParam("q", admin.email())
                .when().get("/api/contas")
                .then().statusCode(200).body("itens", empty());
    }

    @Test
    void filtraPorEstadoPesquisaPorEmailEPagina() {
        contas.sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE id = ?", bruno.id());
        var comSessao = given().cookie(COOKIE, admin.token()).queryParam("q", sufixo);

        comSessao.queryParam("estado", "DESATIVADO").when().get("/api/contas")
                .then().statusCode(200).body("itens.nome", contains("Bruno " + sufixo));
        given().cookie(COOKIE, admin.token()).queryParam("q", sufixo).queryParam("estado", "ATIVO")
                .when().get("/api/contas")
                .then().body("itens.nome", contains("Ana " + sufixo, "Carla " + sufixo));
        given().cookie(COOKIE, admin.token()).queryParam("q", carla.email().toUpperCase())
                .when().get("/api/contas")
                .then().body("itens.nome", contains("Carla " + sufixo));
        given().cookie(COOKIE, admin.token()).queryParam("q", sufixo).queryParam("pagina", 2).queryParam("tamanho", 2)
                .when().get("/api/contas")
                .then().body("total", equalTo(3)).body("itens.nome", contains("Carla " + sufixo));
    }

    @Test
    void recusaFiltrosInvalidos() {
        given().cookie(COOKIE, admin.token()).queryParam("estado", "POR_CONFIRMAR")
                .when().get("/api/contas")
                .then().statusCode(400).body("detalhes.campo", hasItem("estado"));
        given().cookie(COOKIE, admin.token()).queryParam("tamanho", 0)
                .when().get("/api/contas")
                .then().statusCode(400).body("detalhes.campo", hasItem("tamanho"));
    }

    // Acesso (crit. 7 e 8)

    @Test
    void osMembrosNaoTemAcessoASGestaoDeContas() {
        String c = "/api/contas/" + bruno.id();
        given().cookie(COOKIE, ana.token()).when().get("/api/contas").then().statusCode(403)
                .body("codigo", equalTo("SEM_PERMISSAO"));
        given().cookie(COOKIE, ana.token()).when().get(c + "/projetos-geridos").then().statusCode(403);
        desativar(ana, bruno.id(), null).then().statusCode(403);
        reativar(ana, bruno.id()).then().statusCode(403);
        given().when().get("/api/contas").then().statusCode(401);
        given().when().post(c + "/reativacao").then().statusCode(401);

        assertEquals(1, contas.contar("SELECT count(*) FROM utilizador WHERE id = ? AND estado = 'ATIVO'", bruno.id()));
    }

    @Test
    void naoSeDesativaNemReativaUmaContaDeAdministradorIncluindoAPropria() {
        Conta outro = contas.administrador();
        desativar(admin, admin.id(), null).then().statusCode(403);
        desativar(admin, outro.id(), null).then().statusCode(403);
        reativar(admin, outro.id()).then().statusCode(403);
        given().cookie(COOKIE, admin.token()).when().get("/api/contas/" + outro.id() + "/projetos-geridos")
                .then().statusCode(403);
        given().cookie(COOKIE, admin.token()).when().get("/api/auth/me").then().statusCode(200);
    }

    @Test
    void contaInexistenteOuIdentificadorInvalidoDa404() {
        desativar(admin, UUID.randomUUID(), null).then().statusCode(404).body("codigo", equalTo("NAO_ENCONTRADO"));
        reativar(admin, "nao-e-um-id").then().statusCode(404);
        given().cookie(COOKIE, admin.token()).when().get("/api/contas/" + UUID.randomUUID() + "/projetos-geridos")
                .then().statusCode(404);
    }

    // Projetos geridos (crit. 3, S04)

    @Test
    void projetosGeridosIncluemArquivadosEMostramSoONome() {
        String website = criarProjeto(ana, "Website " + sufixo);
        String arquivo = criarProjeto(ana, "Arquivo " + sufixo);
        criarProjeto(bruno, "Do Bruno " + sufixo);
        arquivar(arquivo);

        given().cookie(COOKIE, admin.token()).when().get("/api/contas/" + ana.id() + "/projetos-geridos")
                .then().statusCode(200)
                .body("nome", contains("Arquivo " + sufixo, "Website " + sufixo))
                .body("id", contains(arquivo, website))
                .body("arquivado", contains(true, false))
                .body("[0].keySet()", containsInAnyOrder("id", "nome", "arquivado"));
        given().cookie(COOKIE, admin.token()).when().get("/api/contas/" + carla.id() + "/projetos-geridos")
                .then().statusCode(200).body("$", empty());
    }

    // Desativar quem não é Gestor (crit. 1, 2, 5; PR15, PR16)

    @Test
    void desativarTerminaAsSessoesEImpedeDeEntrar() {
        desativar(admin, bruno.id(), null).then().statusCode(200)
                .body("id", equalTo(bruno.id().toString()))
                .body("estado", equalTo("DESATIVADO"));

        given().cookie(COOKIE, bruno.token()).when().get("/api/auth/me").then().statusCode(401);
        assertEquals(0, contas.contar("SELECT count(*) FROM sessao WHERE utilizador_id = ?", bruno.id()));
        entrar(bruno).then().statusCode(401).body("codigo", equalTo("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void desativarRetiraDasTarefasNaoConcluidasDeTodosOsProjetosComOAdministradorComoAutor() {
        String website = criarProjeto(ana, "Website " + sufixo);
        String arquivo = criarProjeto(ana, "Arquivo " + sufixo);
        adicionarMembro(ana, website, bruno);
        adicionarMembro(ana, arquivo, bruno);
        String porFazer = tarefa(ana, website, "Por fazer", bruno);
        String concluida = tarefa(ana, website, "Concluída", bruno);
        String doArquivo = tarefa(ana, arquivo, "Do arquivo", bruno);
        contas.sql("UPDATE tarefa SET estado = 'CONCLUIDA' WHERE id = ?", UUID.fromString(concluida));
        arquivar(arquivo);

        desativar(admin, bruno.id(), null).then().statusCode(200);

        String responsavel = "SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ? AND utilizador_id = ?";
        assertEquals(0, contas.contar(responsavel, UUID.fromString(porFazer), bruno.id()));
        assertEquals(0, contas.contar(responsavel, UUID.fromString(doArquivo), bruno.id()));
        assertEquals(1, contas.contar(responsavel, UUID.fromString(concluida), bruno.id()));
        assertEquals(2, contas.contar("""
                SELECT count(*) FROM historico_tarefa
                WHERE tarefa_id IN (?, ?) AND autor_id = ? AND campo = 'responsavel'
                  AND valor_anterior = ? AND valor_novo IS NULL
                """, UUID.fromString(porFazer), UUID.fromString(doArquivo), admin.id(), bruno.nome()));
        // Continua na lista de membros, como desativado (PR15)
        given().cookie(COOKIE, ana.token()).when().get("/api/projetos/" + website + "/membros")
                .then().statusCode(200)
                .body("find { it.id == '" + bruno.id() + "' }.ativo", equalTo(false));
    }

    @Test
    void umaContaDesativadaNaoEAdicionadaAProjetosNemEscolhidaComoResponsavel() {
        String website = criarProjeto(ana, "Website " + sufixo);
        adicionarMembro(ana, website, bruno);
        String tarefa = tarefa(ana, website, "Rever", ana);
        desativar(admin, bruno.id(), null).then().statusCode(200);
        desativar(admin, carla.id(), null).then().statusCode(200);

        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", carla.id().toString()))
                .when().post("/api/projetos/" + website + "/membros")
                .then().statusCode(400);
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", bruno.id().toString()))
                .when().post("/api/tarefas/" + tarefa + "/responsaveis")
                .then().statusCode(400);
        given().cookie(COOKIE, ana.token()).queryParam("q", sufixo).when().get("/api/utilizadores")
                .then().statusCode(200).body("itens.nome", contains("Ana " + sufixo));
    }

    // Desativar um Gestor (crit. 3, 4; RN17, RI07)

    @Test
    void desativarUmGestorPassaCadaProjetoAoNovoGestorIncluindoArquivados() {
        String website = criarProjeto(ana, "Website " + sufixo);
        String arquivo = criarProjeto(ana, "Arquivo " + sufixo);
        adicionarMembro(ana, website, bruno);
        arquivar(arquivo);

        desativar(admin, ana.id(), Map.of("novosGestores", List.of(
                Map.of("projetoId", website, "gestorId", bruno.id().toString()),
                Map.of("projetoId", arquivo, "gestorId", carla.id().toString()))))
                .then().statusCode(200).body("estado", equalTo("DESATIVADO"));

        assertGestor(website, bruno);
        assertGestor(arquivo, carla);
        // A Carla não era membro do projeto arquivado e passou a sê-lo; a Ana continua membro
        assertTrue(eMembro(arquivo, carla));
        assertTrue(eMembro(website, ana));
        given().cookie(COOKIE, bruno.token()).when().get("/api/projetos/" + website)
                .then().statusCode(200).body("souGestor", equalTo(true));
    }

    @Test
    void semNovoGestorParaCadaProjetoNadaEAlterado() {
        String website = criarProjeto(ana, "Website " + sufixo);
        String portal = criarProjeto(ana, "Portal " + sufixo);
        String tarefa = tarefa(ana, website, "Rever", ana);

        desativar(admin, ana.id(), null).then().statusCode(400)
                .body("codigo", equalTo("VALIDACAO"))
                .body("detalhes.campo", hasItem("novosGestores"))
                .body("detalhes.mensagem", hasItem("Escolha o novo Gestor do projeto «Portal " + sufixo + "»."));
        desativar(admin, ana.id(), novosGestores(website, bruno)).then().statusCode(400)
                .body("detalhes.mensagem", hasItem("Escolha o novo Gestor do projeto «Portal " + sufixo + "»."))
                .body("detalhes.mensagem", not(hasItem("Escolha o novo Gestor do projeto «Website " + sufixo + "».")));

        assertGestor(website, ana);
        assertGestor(portal, ana);
        assertEquals(1, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?", UUID.fromString(tarefa)));
        given().cookie(COOKIE, ana.token()).when().get("/api/auth/me").then().statusCode(200);
    }

    @Test
    void recusaNovosGestoresInelegiveis() {
        String website = criarProjeto(ana, "Website " + sufixo);
        String doBruno = criarProjeto(bruno, "Do Bruno " + sufixo);
        contas.sql("UPDATE utilizador SET estado = 'DESATIVADO' WHERE id = ?", carla.id());

        // Desativado, Administrador, a própria pessoa, projeto que não é dela, projeto repetido
        for (Object corpo : List.of(
                novosGestores(website, carla),
                novosGestores(website, admin),
                novosGestores(website, ana),
                Map.of("novosGestores", List.of(
                        Map.of("projetoId", website, "gestorId", bruno.id().toString()),
                        Map.of("projetoId", doBruno, "gestorId", bruno.id().toString()))),
                Map.of("novosGestores", List.of(
                        Map.of("projetoId", website, "gestorId", bruno.id().toString()),
                        Map.of("projetoId", website, "gestorId", bruno.id().toString()))))) {
            desativar(admin, ana.id(), corpo).then().statusCode(400).body("detalhes.campo", hasItem("novosGestores"));
        }
        // Identificadores em falta ou inválidos, e campos desconhecidos
        desativar(admin, ana.id(), Map.of("novosGestores", List.of(Map.of("projetoId", website))))
                .then().statusCode(400).body("detalhes.campo", hasItem("novosGestores"));
        desativar(admin, ana.id(), Map.of("novosGestores", List.of(Map.of("projetoId", "x", "gestorId", "y"))))
                .then().statusCode(400);
        desativar(admin, ana.id(), Map.of("motivo", "Saiu")).then().statusCode(400);

        assertGestor(website, ana);
        assertEquals(1, contas.contar("SELECT count(*) FROM utilizador WHERE id = ? AND estado = 'ATIVO'", ana.id()));
    }

    // Reativar (crit. 6) e repetir

    @Test
    void reativarDevolveOAcessoComoMembroSemTarefasNemPapelDeGestor() {
        String website = criarProjeto(ana, "Website " + sufixo);
        tarefa(ana, website, "Rever", ana);
        desativar(admin, ana.id(), novosGestores(website, bruno)).then().statusCode(200);

        reativar(admin, ana.id()).then().statusCode(200).body("estado", equalTo("ATIVO"));

        String token = entrar(ana).then().statusCode(200).extract().cookie(COOKIE);
        given().cookie(COOKIE, token).when().get("/api/projetos/" + website)
                .then().statusCode(200).body("souGestor", equalTo(false));
        given().cookie(COOKIE, token).when().get("/api/minhas-tarefas")
                .then().statusCode(200).body("itens", empty());
    }

    @Test
    void desativarUmaContaDesativadaOuReativarUmaAtivaNaoAlteraNada() {
        desativar(admin, bruno.id(), null).then().statusCode(200);
        desativar(admin, bruno.id(), Map.of("novosGestores", List.of())).then().statusCode(200)
                .body("estado", equalTo("DESATIVADO"));
        reativar(admin, carla.id()).then().statusCode(200).body("estado", equalTo("ATIVO"));
        given().cookie(COOKIE, carla.token()).when().get("/api/auth/me").then().statusCode(200);
    }
}
