package pt.esmad.matriz.tarefas;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.texto.Texto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Tarefa e as suas atribuições (agregado do módulo Tarefas, ADR 0001).
 * Protege as regras do título e da descrição (RN10, RN23) e a versão (D08).
 */
@Entity
@Table(name = "tarefa")
public class Tarefa {

    static final int TITULO_MINIMO = 3;
    static final int TITULO_MAXIMO = 150;
    static final int DESCRICAO_MAXIMA = 2000;

    @Id
    private UUID id;

    @Column(name = "projeto_id")
    private UUID projetoId;

    private String titulo;

    private String descricao;

    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    /** Aumenta em cada alteração gravada; também impede que duas gravações simultâneas se sobreponham. */
    @Version
    private int versao;

    @ElementCollection
    @CollectionTable(name = "atribuicao_tarefa", joinColumns = @JoinColumn(name = "tarefa_id"))
    @Column(name = "utilizador_id")
    private Set<UUID> responsaveis = new HashSet<>();

    protected Tarefa() {
        // JPA
    }

    /** A tarefa começa sempre em "A fazer" (PR05), sem responsáveis e na versão 0. */
    public static Tarefa criar(UUID projetoId, NovaTarefa dados) {
        String tituloNormalizado = Texto.normalizar(dados.titulo());
        String descricaoNormalizada = Texto.normalizar(dados.descricao());
        Validacao validacao = new Validacao();
        validarTitulo(tituloNormalizado, validacao);
        validarDescricao(descricaoNormalizada, validacao);
        validacao.lancarSeHouverErros();

        Tarefa tarefa = new Tarefa();
        tarefa.id = UUID.randomUUID();
        tarefa.projetoId = projetoId;
        tarefa.titulo = tituloNormalizado;
        tarefa.descricao = descricaoNormalizada;
        tarefa.prazo = dados.prazo();
        tarefa.prioridade = dados.prioridade();
        tarefa.estado = Estado.A_FAZER;
        tarefa.versao = 0;
        return tarefa;
    }

    /** Um campo que mudou, com os valores em texto ({@code null} = vazio), para o histórico (RI12). */
    public record CampoAlterado(String campo, String valorAnterior, String valorNovo) {
    }

    /**
     * Altera os campos indicados e devolve os que mudaram de facto. Recusa a alteração se o
     * cliente tiver uma versão antiga (D08). Qualquer estado pode passar a qualquer outro (RN13, RN14).
     * O projeto não arquivado é garantido antes pelo serviço, através do módulo Projetos.
     */
    public List<CampoAlterado> editar(AlteracoesTarefa alteracoes) {
        if (alteracoes.versao() != versao) {
            throw ErroAplicacao.versaoDesatualizada();
        }
        String novoTitulo = alteracoes.titulo() != null ? Texto.normalizar(alteracoes.titulo()) : titulo;
        String novaDescricao = alteracoes.alteraDescricao() ? Texto.normalizar(alteracoes.descricao()) : descricao;
        Validacao validacao = new Validacao();
        validarTitulo(novoTitulo, validacao);
        validarDescricao(novaDescricao, validacao);
        validacao.lancarSeHouverErros();

        LocalDate novoPrazo = alteracoes.alteraPrazo() ? alteracoes.prazo() : prazo;
        Prioridade novaPrioridade = alteracoes.alteraPrioridade() ? alteracoes.prioridade() : prioridade;
        Estado novoEstado = alteracoes.estado() != null ? alteracoes.estado() : estado;

        List<CampoAlterado> alterados = new ArrayList<>();
        comparar("titulo", titulo, novoTitulo, alterados);
        comparar("descricao", descricao, novaDescricao, alterados);
        comparar("estado", estado, novoEstado, alterados);
        comparar("prazo", prazo, novoPrazo, alterados);
        comparar("prioridade", prioridade, novaPrioridade, alterados);

        titulo = novoTitulo;
        descricao = novaDescricao;
        estado = novoEstado;
        prazo = novoPrazo;
        prioridade = novaPrioridade;
        return alterados;
    }

    private static void comparar(String campo, Object anterior, Object novo, List<CampoAlterado> alterados) {
        if (!Objects.equals(anterior, novo)) {
            alterados.add(new CampoAlterado(campo, emTexto(anterior), emTexto(novo)));
        }
    }

    /** Datas em AAAA-MM-DD e enums pelo código (ex.: EM_CURSO). */
    private static String emTexto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static void validarTitulo(String titulo, Validacao validacao) {
        if (titulo == null) {
            validacao.erro("titulo", "O título é obrigatório.");
        } else if (Texto.comprimento(titulo) < TITULO_MINIMO || Texto.comprimento(titulo) > TITULO_MAXIMO) {
            validacao.erro("titulo", "O título deve ter entre 3 e 150 caracteres.");
        }
    }

    private static void validarDescricao(String descricao, Validacao validacao) {
        if (descricao != null && Texto.comprimento(descricao) > DESCRICAO_MAXIMA) {
            validacao.erro("descricao", "A descrição não pode ter mais de 2000 caracteres.");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID projetoId() {
        return projetoId;
    }

    public String titulo() {
        return titulo;
    }

    public String descricao() {
        return descricao;
    }

    public LocalDate prazo() {
        return prazo;
    }

    public Prioridade prioridade() {
        return prioridade;
    }

    public Estado estado() {
        return estado;
    }

    public int versao() {
        return versao;
    }

    public Set<UUID> responsaveis() {
        return Collections.unmodifiableSet(responsaveis);
    }
}
