package pt.esmad.matriz.comentarios;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.contas.ContasServico;
import pt.esmad.matriz.contas.PessoaResumo;
import pt.esmad.matriz.tarefas.AcessoTarefasServico;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso dos comentários (US12 crit. 1 e 2). O acesso é pedido ao módulo Tarefas
 * (ADR 0001, regra 5), a partir da identidade da sessão (S03).
 */
@ApplicationScoped
public class ComentariosServico {

    private final ComentarioRepositorio comentarios;
    private final AcessoTarefasServico tarefas;
    private final ContasServico contas;

    ComentariosServico(ComentarioRepositorio comentarios, AcessoTarefasServico tarefas, ContasServico contas) {
        this.comentarios = comentarios;
        this.tarefas = tarefas;
        this.contas = contas;
    }

    /** Um comentário com o nome do autor. */
    public record ComentarioDetalhe(Comentario comentario, PessoaResumo autor) {
    }

    /** Só membros do projeto da tarefa; o projeto pode estar arquivado. */
    @Transactional
    public Pagina<ComentarioDetalhe> listar(UUID tarefaId, PedidoPagina pedido) {
        tarefas.comoMembro(tarefaId);
        List<Comentario> pagina = comentarios.daTarefa(tarefaId, pedido);
        Map<UUID, PessoaResumo> autores = contas.pessoas(pagina.stream().map(Comentario::autorId).distinct().toList());
        List<ComentarioDetalhe> itens = pagina.stream()
                .map(c -> new ComentarioDetalhe(c, autores.get(c.autorId())))
                .toList();
        return new Pagina<>(itens, comentarios.contarDaTarefa(tarefaId), pedido.pagina(), pedido.tamanho());
    }

    /** Qualquer membro, num projeto não arquivado (RN15); o autor é quem tem a sessão (US12 crit. 1). */
    @Transactional
    public ComentarioDetalhe comentar(UUID tarefaId, String texto) {
        Identidade quem = tarefas.comoMembroParaAlterar(tarefaId);
        Comentario comentario = Comentario.criar(tarefaId, quem.id(), texto,
                // O PostgreSQL guarda microssegundos: assim a resposta e a listagem mostram a mesma hora
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        comentarios.persist(comentario);
        return new ComentarioDetalhe(comentario, new PessoaResumo(quem.id(), quem.nome()));
    }
}
