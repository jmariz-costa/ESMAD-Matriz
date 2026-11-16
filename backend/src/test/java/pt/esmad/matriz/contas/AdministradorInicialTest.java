package pt.esmad.matriz.contas;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Configuração do Administrador inicial (D07, RN04) que não chega a usar a base de dados:
 * sem email, sem palavra-passe ou com palavra-passe curta, a conta não é criada.
 * Sem fonte de dados (null), qualquer acesso à base de dados falharia o teste.
 */
class AdministradorInicialTest {

    @ParameterizedTest(name = "email \"{0}\", palavra-passe \"{1}\"")
    @CsvSource(value = {
            "NADA, NADA",
            "'', segredo123",
            "admin@empresa.pt, NADA",
            "admin@empresa.pt, '   '",
            "admin@empresa.pt, curta12"
    }, nullValues = "NADA")
    void configuracaoIncompletaNaoCriaAConta(String email, String palavraPasse) {
        AdministradorInicial administrador = new AdministradorInicial(null, "Administrador",
                Optional.ofNullable(email), Optional.ofNullable(palavraPasse));
        assertDoesNotThrow(() -> administrador.aoArrancar(null));
    }
}
