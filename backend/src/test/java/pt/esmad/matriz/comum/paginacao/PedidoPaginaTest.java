package pt.esmad.matriz.comum.paginacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Parâmetros de paginação (openapi.yaml, Pagina e Tamanho; US17), sem Quarkus nem base de dados. */
class PedidoPaginaTest {

    private static List<String> camposComErro(String pagina, String tamanho) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> PedidoPagina.de(pagina, tamanho));
        assertEquals(400, erro.estadoHttp());
        assertEquals(CodigoErro.VALIDACAO, erro.codigo());
        return erro.detalhes().stream().map(Erro.Detalhe::campo).toList();
    }

    @Test
    void semParametrosUsaOsValoresPorOmissao() {
        assertEquals(new PedidoPagina(1, 20), PedidoPagina.de(null, null));
        assertEquals(new PedidoPagina(1, 20), PedidoPagina.de("", "  "));
    }

    @Test
    void aceitaEspacosNasPontasEOsLimites() {
        assertEquals(new PedidoPagina(2, 1), PedidoPagina.de(" 2 ", "1"));
        assertEquals(new PedidoPagina(1, 100), PedidoPagina.de("1", "100"));
    }

    @ParameterizedTest(name = "pagina \"{0}\"")
    @ValueSource(strings = {"0", "-1", "um", "1.5"})
    void paginaInvalida(String pagina) {
        assertEquals(List.of("pagina"), camposComErro(pagina, null));
    }

    @ParameterizedTest(name = "tamanho \"{0}\"")
    @ValueSource(strings = {"0", "101", "vinte", "99999999999"})
    void tamanhoInvalido(String tamanho) {
        assertEquals(List.of("tamanho"), camposComErro(null, tamanho));
    }

    @Test
    void indicaOsDoisCamposQuandoAmbosEstaoErrados() {
        assertEquals(List.of("pagina", "tamanho"), camposComErro("0", "0"));
    }

    @ParameterizedTest(name = "página {0} de {1} começa em {2}")
    @CsvSource({"1, 20, 0", "3, 20, 40", "2, 100, 100"})
    void inicio(int pagina, int tamanho, int inicio) {
        assertEquals(inicio, new PedidoPagina(pagina, tamanho).inicio());
    }
}
