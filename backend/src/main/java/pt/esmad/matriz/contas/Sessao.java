package pt.esmad.matriz.contas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sessão iniciada por um utilizador (D06). Só o hash do token é guardado;
 * o token em claro existe apenas no cookie do browser.
 */
@Entity
@Table(name = "sessao")
public class Sessao {

    static final Duration TEMPO_INATIVIDADE = Duration.ofMinutes(30); // D06

    @Id
    private UUID id;

    @Column(name = "utilizador_id")
    private UUID utilizadorId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "criada_em")
    private Instant criadaEm;

    @Column(name = "ultima_atividade")
    private Instant ultimaAtividade;

    protected Sessao() {
        // JPA
    }

    public static Sessao iniciar(UUID utilizadorId, String tokenHash, Instant agora) {
        Sessao sessao = new Sessao();
        sessao.id = UUID.randomUUID();
        sessao.utilizadorId = utilizadorId;
        sessao.tokenHash = tokenHash;
        sessao.criadaEm = agora;
        sessao.ultimaAtividade = agora;
        return sessao;
    }

    /** Expira ao fim de 30 minutos sem pedidos (D06). */
    public boolean expirou(Instant agora) {
        return !agora.isBefore(ultimaAtividade.plus(TEMPO_INATIVIDADE));
    }

    /** Cada pedido renova o prazo (D06). */
    public void renovar(Instant agora) {
        ultimaAtividade = agora;
    }

    public UUID id() {
        return id;
    }

    public UUID utilizadorId() {
        return utilizadorId;
    }

    public Instant ultimaAtividade() {
        return ultimaAtividade;
    }
}
