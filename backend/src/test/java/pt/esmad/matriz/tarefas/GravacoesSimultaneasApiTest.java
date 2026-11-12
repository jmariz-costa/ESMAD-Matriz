package pt.esmad.matriz.tarefas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * Dois pedidos ao mesmo tempo sobre a mesma tarefa (D08, US05 crit. 8, RN11): um é aceite e o
 * outro recusado com 409, sem gravações perdidas nem atribuições repetidas. Revisão do I8 (R2).
 */
@QuarkusTest
class GravacoesSimultaneasApiTest {

    private static final int RONDAS = 3;

    @Inject
    ContasDeTeste contas;

    private Conta ana;
    private Conta bruno;
    private String tarefa;

    @BeforeEach
    void criarTarefa() {
        ana = contas.membro("Ana Exemplo");
        bruno = contas.membro("Bruno Exemplo");
        String projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos").then().statusCode(201).extract().path("id");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                .body(Map.of("utilizadorId", bruno.id().toString()))
                .when().post("/api/projetos/" + projeto + "/membros").then().statusCode(201);
        tarefa = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + projeto + "/tarefas").then().statusCode(201).extract().path("id");
    }

    /** Os dois pedidos partem ao mesmo tempo; devolve os estados HTTP ordenados. */
    private static List<Integer> aoMesmoTempo(Callable<Response> primeiro, Callable<Response> segundo) throws Exception {
        ExecutorService execucao = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch partida = new CountDownLatch(1);
            List<Future<Response>> respostas = new ArrayList<>();
            for (Callable<Response> pedido : List.of(primeiro, segundo)) {
                respostas.add(execucao.submit(() -> {
                    partida.await();
                    return pedido.call();
                }));
            }
            partida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Response> resposta : respostas) {
                estados.add(resposta.get().statusCode());
            }
            estados.sort(null);
            return estados;
        } finally {
            execucao.shutdown();
        }
    }

    private int versaoAtual() {
        return given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                .then().statusCode(200).extract().path("versao");
    }

    private Response editar(Conta quem, int versao, String titulo) {
        return given().cookie(COOKIE, quem.token()).contentType(ContentType.JSON)
                .body(Map.of("versao", versao, "titulo", titulo))
                .when().patch("/api/tarefas/" + tarefa);
    }

    @Test
    void duasGravacoesComAMesmaVersaoUmaEAceiteEAOutraRecusada() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            int versao = versaoAtual();
            String daAna = "Título da Ana " + ronda;
            String doBruno = "Título do Bruno " + ronda;

            assertEquals(List.of(200, 409), aoMesmoTempo(() -> editar(ana, versao, daAna), () -> editar(bruno, versao, doBruno)));

            // A versão subiu uma só vez e o título é o de quem ganhou, com uma só entrada no histórico
            assertEquals(versao + 1, versaoAtual());
            String titulo = given().cookie(COOKIE, ana.token()).when().get("/api/tarefas/" + tarefa)
                    .then().extract().path("titulo");
            assertEquals(1, List.of(daAna, doBruno).stream().filter(titulo::equals).count());
            assertEquals(1, contas.contar("""
                    SELECT count(*) FROM historico_tarefa
                    WHERE tarefa_id = ? AND campo = 'titulo' AND valor_novo IN (?, ?)
                    """, UUID.fromString(tarefa), daAna, doBruno));
        }
    }

    @Test
    void atribuirAMesmaPessoaDuasVezesAoMesmoTempoNaoDuplica() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            Callable<Response> atribuir = () -> given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON)
                    .body(Map.of("utilizadorId", bruno.id().toString()))
                    .when().post("/api/tarefas/" + tarefa + "/responsaveis");

            assertEquals(List.of(201, 409), aoMesmoTempo(atribuir, atribuir));
            assertEquals(1, contas.contar("SELECT count(*) FROM atribuicao_tarefa WHERE tarefa_id = ?",
                    UUID.fromString(tarefa)));

            given().cookie(COOKIE, ana.token()).when().delete("/api/tarefas/" + tarefa + "/responsaveis/" + bruno.id())
                    .then().statusCode(204);
        }
    }
}
