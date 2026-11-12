package pt.esmad.matriz.comentarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.texto.Texto;

import java.time.Instant;
import java.util.UUID;

/**
 * Comentário de uma tarefa (agregado do módulo Comentários, ADR 0001).
 * Protege as regras do texto (RN23): 1 a 2000 caracteres, sem contar os espaços nas pontas.
 * Editar e apagar os próprios comentários é da Fase 2 (US12 crit. 3 e 4).
 */
@Entity
@Table(name = "comentario")
public class Comentario {

    static final int TEXTO_MAXIMO = 2000;

    @Id
    private UUID id;

    @Column(name = "tarefa_id")
    private UUID tarefaId;

    @Column(name = "autor_id")
    private UUID autorId;

    private String texto;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "editado_em")
    private Instant editadoEm;

    protected Comentario() {
        // JPA
    }

    /** O autor e a data vêm sempre do Backend (sessão e relógio do servidor), nunca do pedido (US12 crit. 1). */
    public static Comentario criar(UUID tarefaId, UUID autorId, String texto, Instant criadoEm) {
        String normalizado = Texto.normalizar(texto);
        Validacao validacao = new Validacao();
        if (normalizado == null) {
            validacao.erro("texto", "O comentário não pode estar vazio.");
        } else if (Texto.comprimento(normalizado) > TEXTO_MAXIMO) {
            validacao.erro("texto", "O comentário não pode ter mais de 2000 caracteres.");
        }
        validacao.lancarSeHouverErros();

        Comentario comentario = new Comentario();
        comentario.id = UUID.randomUUID();
        comentario.tarefaId = tarefaId;
        comentario.autorId = autorId;
        comentario.texto = normalizado;
        comentario.criadoEm = criadoEm;
        return comentario;
    }

    public UUID id() {
        return id;
    }

    public UUID tarefaId() {
        return tarefaId;
    }

    public UUID autorId() {
        return autorId;
    }

    public String texto() {
        return texto;
    }

    public Instant criadoEm() {
        return criadoEm;
    }

    public Instant editadoEm() {
        return editadoEm;
    }
}
