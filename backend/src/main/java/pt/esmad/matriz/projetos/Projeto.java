package pt.esmad.matriz.projetos;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.texto.Texto;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projeto e os seus membros (agregado do módulo Projetos, ADR 0001).
 * Protege as regras do nome e da descrição (RN23) e do Gestor (RI01).
 */
@Entity
@Table(name = "projeto")
public class Projeto {

    static final int NOME_MINIMO = 3;
    static final int NOME_MAXIMO = 100;
    static final int DESCRICAO_MAXIMA = 1000;

    @Id
    private UUID id;

    private String nome;

    private String descricao;

    private boolean arquivado;

    @Column(name = "gestor_id")
    private UUID gestorId;

    @ElementCollection
    @CollectionTable(name = "membro_projeto", joinColumns = @JoinColumn(name = "projeto_id"))
    @Column(name = "utilizador_id")
    private Set<UUID> membros = new HashSet<>();

    protected Projeto() {
        // JPA
    }

    /** Quem cria o projeto fica como Gestor e como membro (RN05, RI01). */
    public static Projeto criar(String nome, String descricao, UUID gestorId) {
        String nomeNormalizado = Texto.normalizar(nome);
        String descricaoNormalizada = Texto.normalizar(descricao);
        Validacao validacao = new Validacao();
        validarNome(nomeNormalizado, validacao);
        validarDescricao(descricaoNormalizada, validacao);
        validacao.lancarSeHouverErros();

        Projeto projeto = new Projeto();
        projeto.id = UUID.randomUUID();
        projeto.nome = nomeNormalizado;
        projeto.descricao = descricaoNormalizada;
        projeto.arquivado = false;
        projeto.gestorId = gestorId;
        projeto.membros.add(gestorId);
        return projeto;
    }

    /**
     * Altera só os campos indicados; {@code descricao} a {@code null} limpa a descrição.
     * Um projeto arquivado não pode ser editado (RI10).
     */
    public void editar(AlteracoesProjeto alteracoes) {
        garantirAlteravel();
        String novoNome = alteracoes.alteraNome() ? Texto.normalizar(alteracoes.nome()) : nome;
        String novaDescricao = alteracoes.alteraDescricao() ? Texto.normalizar(alteracoes.descricao()) : descricao;
        Validacao validacao = new Validacao();
        validarNome(novoNome, validacao);
        validarDescricao(novaDescricao, validacao);
        validacao.lancarSeHouverErros();
        nome = novoNome;
        descricao = novaDescricao;
    }

    /** Recusa alterações num projeto arquivado (RN15, RI10). */
    public void garantirAlteravel() {
        if (arquivado) {
            throw ErroAplicacao.projetoArquivado();
        }
    }

    private static void validarNome(String nome, Validacao validacao) {
        if (nome == null) {
            validacao.erro("nome", "O nome é obrigatório.");
        } else if (Texto.comprimento(nome) < NOME_MINIMO || Texto.comprimento(nome) > NOME_MAXIMO) {
            validacao.erro("nome", "O nome deve ter entre 3 e 100 caracteres.");
        }
    }

    private static void validarDescricao(String descricao, Validacao validacao) {
        if (descricao != null && Texto.comprimento(descricao) > DESCRICAO_MAXIMA) {
            validacao.erro("descricao", "A descrição não pode ter mais de 1000 caracteres.");
        }
    }

    public boolean eMembro(UUID utilizadorId) {
        return membros.contains(utilizadorId);
    }

    public boolean eGestor(UUID utilizadorId) {
        return gestorId.equals(utilizadorId);
    }

    public UUID id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public boolean arquivado() {
        return arquivado;
    }

    public UUID gestorId() {
        return gestorId;
    }

    public Set<UUID> membros() {
        return Collections.unmodifiableSet(membros);
    }
}
