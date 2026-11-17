package pt.esmad.matriz.tarefas;

import pt.esmad.matriz.contas.PessoaResumo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** openapi.yaml, schema MinhaTarefa: os campos de Tarefa, mais o projeto e a data de conclusão. */
public record MinhaTarefaResposta(UUID id, UUID projetoId, String titulo, String descricao, LocalDate prazo,
                                  Prioridade prioridade, Estado estado, List<PessoaResumo> responsaveis, int versao,
                                  ProjetoResumo projeto, Instant concluidaEm) {

    /** openapi.yaml, schema ProjetoResumo. */
    public record ProjetoResumo(UUID id, String nome) {
    }

    static MinhaTarefaResposta de(MinhasTarefasServico.MinhaTarefa minha) {
        Tarefa t = minha.detalhe().tarefa();
        return new MinhaTarefaResposta(t.id(), t.projetoId(), t.titulo(), t.descricao(), t.prazo(),
                t.prioridade(), t.estado(), minha.detalhe().responsaveis(), t.versao(),
                new ProjetoResumo(t.projetoId(), minha.projetoNome()), minha.concluidaEm());
    }
}
