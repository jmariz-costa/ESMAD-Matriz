package pt.esmad.matriz.comum.demo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Casos em que os dados de demonstração não são criados e a base de dados nem é usada:
 * seed desligado, ou palavra-passe em falta ou curta.
 * Sem fonte de dados (null), qualquer acesso à base de dados falharia o teste.
 */
class DadosDemonstracaoTest {

    @Test
    void seedDesligadoNaoFazNada() {
        DadosDemonstracao dados = new DadosDemonstracao(null, false, Optional.of("segredo123"));
        assertDoesNotThrow(() -> dados.aoArrancar(null));
    }

    @Test
    void palavraPasseEmFaltaOuCurtaNaoCriaOsDados() {
        DadosDemonstracao semPalavraPasse = new DadosDemonstracao(null, true, Optional.empty());
        assertDoesNotThrow(() -> semPalavraPasse.aoArrancar(null));
        DadosDemonstracao curta = new DadosDemonstracao(null, true, Optional.of("curta12"));
        assertDoesNotThrow(() -> curta.aoArrancar(null));
    }
}
