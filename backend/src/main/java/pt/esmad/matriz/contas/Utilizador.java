package pt.esmad.matriz.contas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Conta de Membro ou de Administrador (agregado do módulo Contas).
 * Protege a regra do limite de tentativas de login (D09).
 */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    static final int MAXIMO_FALHAS_SEGUIDAS = 5;               // D09
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15); // D09

    @Id
    private UUID id;

    private String nome;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private TipoUtilizador tipo;

    @Enumerated(EnumType.STRING)
    private EstadoConta estado;

    @Column(name = "tentativas_falhadas")
    private int tentativasFalhadas;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    protected Utilizador() {
        // JPA
    }

    public Utilizador(UUID id, String nome, String email, String passwordHash,
                      TipoUtilizador tipo, EstadoConta estado) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.passwordHash = passwordHash;
        this.tipo = tipo;
        this.estado = estado;
    }

    /** Só contas ativas podem iniciar sessão. */
    public boolean estaAtivo() {
        return estado == EstadoConta.ATIVO;
    }

    /** US14 crit. 1: deixa de poder entrar. */
    void desativar() {
        estado = EstadoConta.DESATIVADO;
    }

    /** US14 crit. 6: volta a poder entrar. */
    void reativar() {
        estado = EstadoConta.ATIVO;
    }

    /** Durante o bloqueio o login é recusado, mesmo com a palavra-passe certa (D09). */
    public boolean estaBloqueado(Instant agora) {
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    /**
     * Regista uma palavra-passe errada. À 5.ª falha seguida, bloqueia durante 15 minutos
     * e recomeça a contagem. Falhas durante o bloqueio não o prolongam.
     */
    public void registarFalha(Instant agora) {
        if (estaBloqueado(agora)) {
            return;
        }
        tentativasFalhadas++;
        if (tentativasFalhadas >= MAXIMO_FALHAS_SEGUIDAS) {
            bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
            tentativasFalhadas = 0;
        }
    }

    /** Um login correto volta a pôr o contador a zero (D09). */
    public void registarSucesso() {
        tentativasFalhadas = 0;
        bloqueadoAte = null;
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public TipoUtilizador tipo() {
        return tipo;
    }

    public EstadoConta estado() {
        return estado;
    }

    int tentativasFalhadas() {
        return tentativasFalhadas;
    }
}
