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
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Conta de Membro ou de Administrador (agregado do módulo Contas).
 * Protege as regras do limite de tentativas de login (D09), do nome (RN23), do email das contas
 * novas (RI13, R06) e da palavra-passe temporária (PR20).
 */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    static final int MAXIMO_FALHAS_SEGUIDAS = 5;               // D09
    static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15); // D09
    static final int NOME_MINIMO = 2;   // RN23, US19
    static final int NOME_MAXIMO = 150; // RN23, US19
    static final int EMAIL_MAXIMO = 254; // tamanho da coluna email (VARCHAR(254))

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

    /**
     * US21: conta de Membro ativa, com a palavra-passe temporária definida pelo Administrador, que
     * tem de ser mudada no primeiro login (PR20, RI21). Os problemas do nome, do email e da
     * palavra-passe são devolvidos todos juntos. Não verifica se o email já tem conta: isso é do
     * serviço, que consulta a base de dados.
     *
     * @param dominio domínio da empresa sem "@" e em minúsculas, ou vazio se não estiver configurado
     */
    static Utilizador novoMembro(String nome, String email, String palavraPasseTemporaria, Optional<String> dominio) {
        Validacao validacao = new Validacao();
        String nomeValido = validarNome(nome, validacao);
        String emailValido = validarEmail(email, dominio, validacao);
        PalavraPasse.validar(palavraPasseTemporaria, "palavraPasseTemporaria", validacao);
        validacao.lancarSeHouverErros();
        Utilizador conta = new Utilizador(UUID.randomUUID(), nomeValido, emailValido,
                PalavraPasse.hash(palavraPasseTemporaria), TipoUtilizador.MEMBRO, EstadoConta.ATIVO);
        conta.deveMudarPalavraPasse = true;
        return conta;
    }

    /** US19 crit. 1: só o nome muda, entre 2 e 150 caracteres sem contar os espaços nas pontas. */
    void alterarNome(String novoNome) {
        Validacao validacao = new Validacao();
        String normalizado = validarNome(novoNome, validacao);
        validacao.lancarSeHouverErros();
        nome = normalizado;
    }

    /** RN23: entre 2 e 150 caracteres, sem contar os espaços nas pontas. Devolve o nome sem eles. */
    private static String validarNome(String nome, Validacao validacao) {
        String normalizado = Texto.normalizar(nome);
        if (normalizado == null) {
            validacao.erro("nome", "O nome é obrigatório.");
        } else if (Texto.comprimento(normalizado) < NOME_MINIMO || Texto.comprimento(normalizado) > NOME_MAXIMO) {
            validacao.erro("nome", "O nome deve ter entre 2 e 150 caracteres.");
        }
        return normalizado;
    }

    /**
     * US21 crit. 2, RI13: sem espaços nas pontas e em minúsculas; um só "@", com texto antes e
     * depois, sem espaços no meio; até 254 caracteres; e, se houver domínio configurado, desse
     * domínio exatamente (R06). Devolve o email normalizado.
     */
    private static String validarEmail(String email, Optional<String> dominio, Validacao validacao) {
        String normalizado = Texto.normalizar(email);
        if (normalizado == null) {
            validacao.erro("email", "O email é obrigatório.");
            return null;
        }
        normalizado = normalizado.toLowerCase(Locale.ROOT);
        int arroba = normalizado.indexOf('@');
        boolean formatoValido = arroba > 0
                && arroba == normalizado.lastIndexOf('@')
                && arroba < normalizado.length() - 1
                && normalizado.codePoints().noneMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
        if (Texto.comprimento(normalizado) > EMAIL_MAXIMO) {
            validacao.erro("email", "O email deve ter no máximo 254 caracteres.");
        } else if (!formatoValido) {
            validacao.erro("email", "Indique um email válido, no formato nome@dominio.");
        } else if (dominio.isPresent() && !normalizado.substring(arroba + 1).equals(dominio.get())) {
            validacao.erro("email", "O email tem de ser do domínio " + dominio.get() + ".");
        }
        return normalizado;
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
