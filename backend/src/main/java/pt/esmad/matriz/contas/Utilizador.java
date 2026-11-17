package pt.esmad.matriz.contas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.texto.Texto;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Conta de Membro ou de Administrador (agregado do módulo Contas).
 * Protege as regras do limite de tentativas de login (D09), do nome (RN23) e da
 * palavra-passe temporária (PR20).
 */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    static final int MAXIMO_FALHAS_SEGUIDAS = 5;               // D09
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15); // D09
    static final int NOME_MINIMO = 2;   // RN23, US19
    static final int NOME_MAXIMO = 150; // RN23, US19

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

    @Column(name = "deve_mudar_palavra_passe")
    private boolean deveMudarPalavraPasse;

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

    /** US19 crit. 1: só o nome muda, entre 2 e 150 caracteres sem contar os espaços nas pontas. */
    void alterarNome(String novoNome) {
        String normalizado = Texto.normalizar(novoNome);
        Validacao validacao = new Validacao();
        if (normalizado == null) {
            validacao.erro("nome", "O nome é obrigatório.");
        } else if (Texto.comprimento(normalizado) < NOME_MINIMO || Texto.comprimento(normalizado) > NOME_MAXIMO) {
            validacao.erro("nome", "O nome deve ter entre 2 e 150 caracteres.");
        }
        validacao.lancarSeHouverErros();
        nome = normalizado;
    }

    /**
     * US19 crit. 2: a palavra-passe temporária substitui a anterior e tem de ser mudada no
     * próximo login (PR20); o bloqueio por tentativas falhadas é limpo (D09). Só em contas ativas.
     */
    void reporAcesso(String hashTemporario) {
        if (!estaAtivo()) {
            throw ErroAplicacao.contaDesativada();
        }
        passwordHash = hashTemporario;
        deveMudarPalavraPasse = true;
        registarSucesso();
    }

    /** US20: a palavra-passe nova deixa de ser temporária. */
    void definirPalavraPasse(String novoHash) {
        passwordHash = novoHash;
        deveMudarPalavraPasse = false;
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

    public boolean deveMudarPalavraPasse() {
        return deveMudarPalavraPasse;
    }

    int tentativasFalhadas() {
        return tentativasFalhadas;
    }
}
