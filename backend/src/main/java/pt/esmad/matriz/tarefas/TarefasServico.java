package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.contas.ContasServico;
import pt.esmad.matriz.contas.PessoaResumo;
import pt.esmad.matriz.historico.HistoricoServico;
import pt.esmad.matriz.projetos.AcessoProjetosServico;

import java.text.Collator;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Casos de uso das tarefas (US05, US06, US07, US09, US17). As permissões são verificadas
 * aqui, a partir da identidade da sessão (S03), pedindo ao módulo Projetos o acesso ao projeto.
 */
@ApplicationScoped
public class TarefasServico {

    private static final Collator ORDEM_NOMES = Collator.getInstance(Locale.forLanguageTag("pt-PT"));

    private final TarefaRepositorio tarefas;
    private final TarefaEliminadaRepositorio eliminadas;
    private final AcessoProjetosServico projetos;
    private final ContasServico contas;
    private final HistoricoServico historico;
    private final UtilizadorAtual utilizadorAtual;

    TarefasServico(TarefaRepositorio tarefas, TarefaEliminadaRepositorio eliminadas, AcessoProjetosServico projetos,
                   ContasServico contas, HistoricoServico historico, UtilizadorAtual utilizadorAtual) {
        this.tarefas = tarefas;
        this.eliminadas = eliminadas;
        this.projetos = projetos;
        this.contas = contas;
        this.historico = historico;
        this.utilizadorAtual = utilizadorAtual;
    }

    /** Uma tarefa com os nomes dos responsáveis, ordenados pelo nome. */
    public record TarefaDetalhe(Tarefa tarefa, List<PessoaResumo> responsaveis) {
    }

    /**
     * Só membros, num projeto não arquivado (RN15). A criação fica no histórico (RI12),
     * com quem tem a sessão como autor.
     */
    @Transactional
    public TarefaDetalhe criar(UUID projetoId, NovaTarefa dados) {
        Identidade quem = projetos.comoMembroParaAlterar(projetoId);
        Tarefa tarefa = Tarefa.criar(projetoId, dados);
        tarefas.persist(tarefa);
        // A tarefa tem de existir na base de dados antes da entrada do histórico que a referencia
        tarefas.flush();
        historico.registarCriacao(tarefa.id(), quem.id());
        return detalhe(tarefa);
    }

    /** Só membros do projeto da tarefa (RN20); o projeto pode estar arquivado. */
    @Transactional
    public TarefaDetalhe consultar(UUID tarefaId) {
        Tarefa tarefa = carregar(tarefaId);
        projetos.comoMembro(tarefa.projetoId());
        return detalhe(tarefa);
    }

    /** Só membros (RN20); filtros combinados (US17 crit. 3) e paginação (US17 crit. 4). */
    @Transactional
    public Pagina<TarefaDetalhe> listar(UUID projetoId, Estado estado, FiltroPrioridade prioridade,
                                        PedidoPagina pedido) {
        projetos.comoMembro(projetoId);
        TarefaRepositorio.Filtros filtros = new TarefaRepositorio.Filtros(estado, prioridade);
        List<Tarefa> pagina = tarefas.doProjeto(projetoId, filtros, pedido);
        long total = tarefas.contarDoProjeto(projetoId, filtros);

        Map<UUID, PessoaResumo> pessoas = contas.pessoas(
                pagina.stream().flatMap(t -> t.responsaveis().stream()).distinct().toList());
        List<TarefaDetalhe> itens = pagina.stream()
                .map(t -> new TarefaDetalhe(t, ordenados(t.responsaveis(), pessoas)))
                .toList();
        return new Pagina<>(itens, total, pedido.pagina(), pedido.tamanho());
    }

    /**
     * Só membros, num projeto não arquivado (RN15, US06 crit. 3). A versão enviada tem de ser a
     * atual (D08); cada campo alterado fica no histórico (RI12, US05 crit. 6, US06 crit. 2).
     * Se nada mudar, a versão mantém-se e não há entradas no histórico.
     */
    @Transactional
    public TarefaDetalhe editar(UUID tarefaId, AlteracoesTarefa alteracoes) {
        Tarefa tarefa = carregar(tarefaId);
        Identidade quem = projetos.comoMembroParaAlterar(tarefa.projetoId());
        List<Tarefa.CampoAlterado> alterados = tarefa.editar(alteracoes);
        if (!alterados.isEmpty()) {
            try {
                // Grava já, para a nova versão vir na resposta e para detetar uma gravação simultânea
                tarefas.flush();
            } catch (OptimisticLockException e) {
                throw ErroAplicacao.versaoDesatualizada();
            }
            historico.registarAlteracoes(tarefa.id(), quem.id(), alterados.stream()
                    .map(c -> new HistoricoServico.Alteracao(c.campo(), c.valorAnterior(), c.valorNovo()))
                    .toList());
        }
        return detalhe(tarefa);
    }

    /**
     * Só o Gestor (RN09), num projeto não arquivado. Fica um registo com o título, quem eliminou
     * e quando (RI09); a base de dados apaga em cascata as atribuições, comentários e histórico.
     */
    @Transactional
    public void eliminar(UUID tarefaId) {
        Tarefa tarefa = carregar(tarefaId);
        Identidade quem = projetos.comoGestorParaAlterar(tarefa.projetoId());
        eliminadas.persist(TarefaEliminada.de(tarefa, quem.id(), Instant.now()));
        tarefas.delete(tarefa);
    }

    /**
     * Administrador → 403 (RN19) antes de procurar a tarefa; tarefa inexistente → 404.
     * O acesso ao projeto é verificado a seguir por quem chama.
     */
    private Tarefa carregar(UUID tarefaId) {
        utilizadorAtual.exigirMembro();
        return tarefas.findByIdOptional(tarefaId).orElseThrow(ErroAplicacao::naoEncontrado);
    }

    private TarefaDetalhe detalhe(Tarefa tarefa) {
        return new TarefaDetalhe(tarefa, ordenados(tarefa.responsaveis(), contas.pessoas(tarefa.responsaveis())));
    }

    private static List<PessoaResumo> ordenados(Collection<UUID> ids, Map<UUID, PessoaResumo> pessoas) {
        return ids.stream()
                .map(pessoas::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PessoaResumo::nome, ORDEM_NOMES).thenComparing(PessoaResumo::id))
                .toList();
    }
}
