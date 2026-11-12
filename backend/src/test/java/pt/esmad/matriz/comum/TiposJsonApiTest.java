package pt.esmad.matriz.comum;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/**
 * Regressão da revisão do I8 (R1): um número ou um booleano num campo de texto do contrato
 * era aceite e convertido em texto (ex.: o projeto "12345"); agora é recusado com 400.
 */
@QuarkusTest
class TiposJsonApiTest {

    @Inject
    ContasDeTeste contas;

    private void recusado(Conta quem, String caminho, String corpo) {
        var pedido = given().contentType(ContentType.JSON).body(corpo);
        if (quem != null) {
            pedido = pedido.cookie(COOKIE, quem.token());
        }
        pedido.when().post(caminho).then().statusCode(400).body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void numerosEBooleanosNaoSaoAceitesComoTexto() {
        Conta ana = contas.membro("Ana Exemplo");
        long projetosAntes = contas.contar("SELECT count(*) FROM projeto WHERE gestor_id = ?", ana.id());

        recusado(ana, "/api/projetos", "{\"nome\": 12345}");
        recusado(ana, "/api/projetos", "{\"nome\": \"Portal\", \"descricao\": true}");
        recusado(ana, "/api/projetos", "{\"nome\": 123.5}");
        recusado(null, "/api/auth/login", "{\"email\": 123, \"palavraPasse\": \"x\"}");
        assertEquals(projetosAntes, contas.contar("SELECT count(*) FROM projeto WHERE gestor_id = ?", ana.id()));

        String projeto = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("nome", "Portal"))
                .when().post("/api/projetos").then().statusCode(201).extract().path("id");
        recusado(ana, "/api/projetos/" + projeto + "/membros", "{\"utilizadorId\": 42}");
        String tarefa = given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body(Map.of("titulo", "Rever"))
                .when().post("/api/projetos/" + projeto + "/tarefas").then().statusCode(201).extract().path("id");
        recusado(ana, "/api/tarefas/" + tarefa + "/comentarios", "{\"texto\": 12345}");
        recusado(ana, "/api/tarefas/" + tarefa + "/responsaveis", "{\"utilizadorId\": false}");
        assertEquals(0, contas.contar("SELECT count(*) FROM comentario WHERE tarefa_id = ?", UUID.fromString(tarefa)));
    }

    @Test
    void textoContinuaASerAceite() {
        Conta ana = contas.membro("Ana Exemplo");
        given().cookie(COOKIE, ana.token()).contentType(ContentType.JSON).body("{\"nome\": \"12345\"}")
                .when().post("/api/projetos").then().statusCode(201).body("nome", equalTo("12345"));
    }
}
