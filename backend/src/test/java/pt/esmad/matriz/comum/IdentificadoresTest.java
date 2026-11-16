package pt.esmad.matriz.comum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Identificadores no caminho (404) e no corpo (400), sem Quarkus nem base de dados. */
class IdentificadoresTest {

    private static final UUID ID = UUID.fromString("8f14e45f-ceea-4e7a-9b1c-2d3e4f5a6b7c");

    @Test
    void caminhoComUuidValido() {
        assertEquals(ID, Identificadores.paraUuid(ID.toString()));
    }

    @Test
    void caminhoComUuidInvalidoDa404() {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> Identificadores.paraUuid("abc"));
        assertEquals(404, erro.estadoHttp());
        assertEquals(CodigoErro.NAO_ENCONTRADO, erro.codigo());
    }

    @Test
    void corpoComUuidValidoEEspacosNasPontas() {
        assertEquals(ID, Identificadores.doCorpo(" " + ID + " ", "utilizadorId", "Em falta."));
    }

    @ParameterizedTest(name = "valor \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "   "})
    void corpoSemValorDa400ComAMensagemIndicada(String valor) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class,
                () -> Identificadores.doCorpo(valor, "utilizadorId", "Indique o utilizador."));
        assertEquals(400, erro.estadoHttp());
        assertEquals(List.of(new Erro.Detalhe("utilizadorId", "Indique o utilizador.")), erro.detalhes());
    }

    @Test
    void corpoComUuidInvalidoDa400() {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class,
                () -> Identificadores.doCorpo("123", "utilizadorId", "Indique o utilizador."));
        assertEquals(400, erro.estadoHttp());
        assertEquals(List.of(new Erro.Detalhe("utilizadorId", "O identificador não é válido.")), erro.detalhes());
    }
}
