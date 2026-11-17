package pt.esmad.matriz.tarefas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leitura do parâmetro {@code concluidas} de "As minhas tarefas" (US11), sem Quarkus nem base de dados. */
class MinhasTarefasRecursoTest {

    @Test
    void ausenteOuVazioEFalse() {
        assertFalse(MinhasTarefasRecurso.lerConcluidas(null));
        assertFalse(MinhasTarefasRecurso.lerConcluidas(""));
        assertFalse(MinhasTarefasRecurso.lerConcluidas("   "));
    }

    @Test
    void aceitaTrueEFalseComEspacosNasPontas() {
        assertTrue(MinhasTarefasRecurso.lerConcluidas("true"));
        assertTrue(MinhasTarefasRecurso.lerConcluidas(" true "));
        assertFalse(MinhasTarefasRecurso.lerConcluidas("false"));
        assertFalse(MinhasTarefasRecurso.lerConcluidas(" false "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sim", "1", "TRUE", "verdadeiro"})
    void outroValorDa400NoCampoConcluidas(String valor) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> MinhasTarefasRecurso.lerConcluidas(valor));
        assertEquals(400, erro.estadoHttp());
        assertEquals(CodigoErro.VALIDACAO, erro.codigo());
        assertEquals(List.of("concluidas"), erro.detalhes().stream().map(Erro.Detalhe::campo).toList());
    }
}
