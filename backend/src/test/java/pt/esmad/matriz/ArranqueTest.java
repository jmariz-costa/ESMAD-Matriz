package pt.esmad.matriz;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Confirma que a aplicação arranca, aplica as migrations e acede à base de dados (I0).
 */
@QuarkusTest
@Tag("integracao")
class ArranqueTest {

    @Test
    void aplicacaoArrancaComBaseDeDadosDisponivel() {
        given()
            .when().get("/q/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
