package pt.esmad.matriz.contas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import pt.esmad.matriz.ContasDeTeste;
import pt.esmad.matriz.ContasDeTeste.Conta;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static pt.esmad.matriz.ContasDeTeste.COOKIE;

/** GET /utilizadores: Membros ativos para adicionar a um projeto (RI16, S07). */
@QuarkusTest
class UtilizadoresApiTest {

    @Inject
    ContasDeTeste contas;

    private static Response procurar(Conta quem, String... parametros) {
        var pedido = given().cookie(COOKIE, quem.token());
        for (int i = 0; i < parametros.length; i += 2) {
            pedido = pedido.queryParam(parametros[i], parametros[i + 1]);
        }
        return pedido.when().get("/api/utilizadores");
    }

    /** Um texto único por teste, para não encontrar contas criadas por outros testes. */
    private static String marca() {
        return "zx" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void soDevolveMembrosAtivosOrdenadosPeloNome() {
        String m = marca();
        Conta ana = contas.membro("Beatriz " + m);
        contas.membro("alberto " + m);
        contas.semSessao("Carlos " + m, "MEMBRO", "DESATIVADO");
        contas.semSessao("Diana " + m, "MEMBRO", "POR_CONFIRMAR");
        contas.semSessao("Admin " + m, "ADMINISTRADOR", "ATIVO");

        procurar(ana, "q", m).then().statusCode(200)
                .body("total", equalTo(2))
                .body("itens.nome", contains("alberto " + m, "Beatriz " + m))
                .body("itens[0]", hasKey("email"))
                .body("itens[0]", not(hasKey("tipo")))
                .body("itens[0]", not(hasKey("ativo")));
    }

    @Test
    void pesquisaNoNomeOuNoEmailSemDistinguirMaiusculas() {
        String m = marca();
        Conta ana = contas.membro("Ana " + m);

        procurar(ana, "q", m.toUpperCase()).then().body("itens.nome", contains("Ana " + m));
        procurar(ana, "q", ana.email().substring(0, 20)).then().body("itens.email", hasItem(ana.email()));
    }

    @Test
    void paginacao() {
        String m = marca();
        Conta ana = contas.membro("A " + m);
        contas.membro("B " + m);
        contas.membro("C " + m);

        procurar(ana, "q", m, "pagina", "2", "tamanho", "2").then().statusCode(200)
                .body("total", equalTo(3))
                .body("pagina", equalTo(2))
                .body("itens.nome", contains("C " + m));
        procurar(ana, "tamanho", "0").then().statusCode(400).body("codigo", equalTo("VALIDACAO"));
    }

    @Test
    void administradorRecebe403ESemSessao401() {
        procurar(contas.administrador()).then().statusCode(403).body("codigo", equalTo("SEM_PERMISSAO"));
        given().when().get("/api/utilizadores").then().statusCode(401);
    }
}
