package pt.esmad.matriz.tarefas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.historico.HistoricoServico;
import pt.esmad.matriz.projetos.AcessoProjetosServico;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * "As minhas tarefas" (US11): as tarefas de que quem tem a sessão é responsável, nos projetos
 * não arquivados de que é membro. Os projetos vêm do módulo Projetos e as datas de conclusão do
 * módulo Histórico (ADR 0001, regra 5).
 */
@ApplicationScoped
public class MinhasTarefasServico {

    private final TarefaRepositorio tarefas;
    private final TarefasServico tarefasServico;
    private final AcessoProjetosServico projetos;
    private final HistoricoServico historico;
    private final UtilizadorAtual utilizadorAtual;

    MinhasTarefasServico(TarefaRepositorio tarefas, TarefasServico tarefasServico, AcessoProjetosServico projetos,
                         HistoricoServico historico, UtilizadorAtual utilizadorAtual) {
        this.tarefas = tarefas;
        this.tarefasServico = tarefasServico;
        this.projetos = projetos;
        this.historico = historico;
        this.utilizadorAtual = utilizadorAtual;
    }

    /** Uma tarefa com o nome do projeto e, se estiver concluída, a data de conclusão (ou null). */
    public record MinhaTarefa(TarefasServico.TarefaDetalhe detalhe, String projetoNome, Instant concluidaEm) {
    }

    /**
     * Só Membros (RN19). Sem {@code concluidas}, as não concluídas por prazo (US11 crit. 1 e 3);
     * com {@code concluidas}, o histórico da conclusão mais recente para a mais antiga (crit. 5).
     */
    @Transactional
    public Pagina<MinhaTarefa> listar(boolean concluidas, PedidoPagina pedido) {
        Identidade quem = utilizadorAtual.exigirMembro();
        Map<UUID, String> nomesProjetos = projetos.naoArquivadosDoMembro(quem.id());
        if (nomesProjetos.isEmpty()) {
            return new Pagina<>(List.of(), 0, pedido.pagina(), pedido.tamanho());
        }
        return concluidas
                ? concluidas(quem.id(), nomesProjetos, pedido)
                : porConcluir(quem.id(), nomesProjetos, pedido);
    }

    private Pagina<MinhaTarefa> porConcluir(UUID utilizadorId, Map<UUID, String> nomesProjetos, PedidoPagina pedido) {
        List<Tarefa> pagina = tarefas.porConcluirDe(utilizadorId, nomesProjetos.keySet(), pedido);
        long total = tarefas.contarPorConcluirDe(utilizadorId, nomesProjetos.keySet());
        List<MinhaTarefa> itens = tarefasServico.detalhes(pagina).stream()
                .map(d -> new MinhaTarefa(d, nomesProjetos.get(d.tarefa().projetoId()), null))
                .toList();
        return new Pagina<>(itens, total, pedido.pagina(), pedido.tamanho());
    }

    /**
     * A ordem depende de datas guardadas noutro módulo, por isso ordena e pagina em memória
     * (no máximo 50 pessoas, RNF04): conclusão mais recente primeiro, sem data no fim, e título.
     */
    private Pagina<MinhaTarefa> concluidas(UUID utilizadorId, Map<UUID, String> nomesProjetos, PedidoPagina pedido) {
        List<Tarefa> todas = tarefas.concluidasDe(utilizadorId, nomesProjetos.keySet());
        Map<UUID, Instant> datas = historico.datasConclusao(todas.stream().map(Tarefa::id).toList());
        List<Tarefa> ordenadas = todas.stream()
                .sorted(Comparator.comparing((Tarefa t) -> datas.get(t.id()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(t -> t.titulo().toLowerCase(Locale.ROOT))
                        .thenComparing(Tarefa::id))
                .toList();
        int inicio = Math.min(pedido.inicio(), ordenadas.size());
        int fim = Math.min(inicio + pedido.tamanho(), ordenadas.size());
        List<MinhaTarefa> itens = tarefasServico.detalhes(ordenadas.subList(inicio, fim)).stream()
                .map(d -> new MinhaTarefa(d, nomesProjetos.get(d.tarefa().projetoId()), datas.get(d.tarefa().id())))
                .toList();
        return new Pagina<>(itens, ordenadas.size(), pedido.pagina(), pedido.tamanho());
    }
}
