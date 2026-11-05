package pt.esmad.matriz.projetos;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.contas.ContaResumo;
import pt.esmad.matriz.contas.ContasServico;
import pt.esmad.matriz.tarefas.AtribuicoesServico;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso dos membros de um projeto (US04, RN07, RN08, RI01, RI04, RI16).
 */
@ApplicationScoped
public class MembrosServico {

    private static final Collator ORDEM_NOMES = Collator.getInstance(Locale.forLanguageTag("pt-PT"));

    private final AcessoProjeto acesso;
    private final ContasServico contas;
    private final AtribuicoesServico atribuicoes;

    MembrosServico(AcessoProjeto acesso, ContasServico contas, AtribuicoesServico atribuicoes) {
        this.acesso = acesso;
        this.contas = contas;
        this.atribuicoes = atribuicoes;
    }

    /** Todos os membros, incluindo os de conta desativada (PR15), ordenados pelo nome. */
    @Transactional
    public List<MembroResposta> listar(UUID projetoId) {
        Projeto projeto = acesso.comoMembro(projetoId).projeto();
        return membros(projeto);
    }

    /** Só o Gestor (RN07); só Membros com conta ativa (RI16); sem duplicados (US04 crit. 3). */
    @Transactional
    public MembroResposta adicionar(UUID projetoId, UUID utilizadorId) {
        Projeto projeto = acesso.comoGestor(projetoId).projeto();
        projeto.garantirAlteravel();
        if (!contas.eMembroAtivo(utilizadorId)) {
            throw ErroAplicacao.validacao(List.of(new Erro.Detalhe("utilizadorId",
                    "Só é possível adicionar Membros com conta ativa.")));
        }
        projeto.adicionarMembro(utilizadorId);
        return membro(projeto, contas.contas(List.of(utilizadorId)).get(utilizadorId));
    }

    /**
     * Só o Gestor (RN07); o Gestor não pode ser removido (RI01). Quem sai deixa de ser
     * responsável pelas tarefas não concluídas do projeto (RN08, RI04), na mesma transação.
     */
    @Transactional
    public void remover(UUID projetoId, UUID utilizadorId) {
        Projeto projeto = acesso.comoGestor(projetoId).projeto();
        projeto.removerMembro(utilizadorId);
        atribuicoes.retirarDasTarefasNaoConcluidas(projetoId, utilizadorId);
    }

    private List<MembroResposta> membros(Projeto projeto) {
        Map<UUID, ContaResumo> dados = contas.contas(projeto.membros());
        return dados.values().stream()
                .map(conta -> membro(projeto, conta))
                .sorted(Comparator.comparing(MembroResposta::nome, ORDEM_NOMES).thenComparing(MembroResposta::id))
                .toList();
    }

    private static MembroResposta membro(Projeto projeto, ContaResumo conta) {
        return new MembroResposta(conta.id(), conta.nome(), conta.email(), conta.ativo(), projeto.eGestor(conta.id()));
    }
}
