package pt.esmad.matriz.historico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma entrada do histórico de uma tarefa (RI12): a criação, ou a alteração de um campo
 * com o valor anterior e o novo. As entradas nunca são alteradas.
 */
@Entity
@Table(name = "historico_tarefa")
class EntradaHistorico {

    @Id
    private UUID id;

    @Column(name = "tarefa_id")
    private UUID tarefaId;

    @Column(name = "autor_id")
    private UUID autorId;

    @Column(name = "data_hora")
    private Instant dataHora;

    private String tipo;

    private String campo;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_novo")
    private String valorNovo;

    protected EntradaHistorico() {
        // JPA
    }

    static EntradaHistorico criacao(UUID tarefaId, UUID autorId, Instant dataHora) {
        return nova(tarefaId, autorId, dataHora, "CRIACAO", null, null, null);
    }

    static EntradaHistorico alteracao(UUID tarefaId, UUID autorId, Instant dataHora,
                                      String campo, String valorAnterior, String valorNovo) {
        return nova(tarefaId, autorId, dataHora, "ALTERACAO", campo, valorAnterior, valorNovo);
    }

    private static EntradaHistorico nova(UUID tarefaId, UUID autorId, Instant dataHora, String tipo,
                                         String campo, String valorAnterior, String valorNovo) {
        EntradaHistorico entrada = new EntradaHistorico();
        entrada.id = UUID.randomUUID();
        entrada.tarefaId = tarefaId;
        entrada.autorId = autorId;
        entrada.dataHora = dataHora;
        entrada.tipo = tipo;
        entrada.campo = campo;
        entrada.valorAnterior = valorAnterior;
        entrada.valorNovo = valorNovo;
        return entrada;
    }
}
