package pt.esmad.matriz.tarefas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Filtro de prioridade (US17 crit. 3) e contagens por estado (US18), sem Quarkus nem base de dados. */
class FiltrosEContagensTest {

    @Test
    void filtroDePrioridade() {
        assertEquals(Prioridade.BAIXA, FiltroPrioridade.BAIXA.prioridade());
        assertEquals(Prioridade.MEDIA, FiltroPrioridade.MEDIA.prioridade());
        assertEquals(Prioridade.ALTA, FiltroPrioridade.ALTA.prioridade());
        assertNull(FiltroPrioridade.SEM_PRIORIDADE.prioridade());
    }

    @Test
    void somarCadaEstado() {
        ContagensEstado contagens = ContagensEstado.ZERO
                .somar("A_FAZER", 4)
                .somar("EM_CURSO", 3)
                .somar("EM_REVISAO", 2)
                .somar("CONCLUIDA", 1)
                .somar("A_FAZER", 1);
        assertEquals(new ContagensEstado(5, 3, 2, 1), contagens);
    }

    @Test
    void estadoDesconhecidoEUmErroDoPrograma() {
        assertThrows(IllegalStateException.class, () -> ContagensEstado.ZERO.somar("ARQUIVADA", 1));
    }
}
