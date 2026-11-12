package pt.esmad.matriz.tarefas;

import pt.esmad.matriz.contas.PessoaResumo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** openapi.yaml, schema Tarefa. Os campos opcionais sem valor vêm a null. */
public record TarefaResposta(UUID id, UUID projetoId, String titulo, String descricao, LocalDate prazo,
                             Prioridade prioridade, Estado estado, List<PessoaResumo> responsaveis, int versao) {

    static TarefaResposta de(TarefasServico.TarefaDetalhe detalhe) {
        Tarefa t = detalhe.tarefa();
        return new TarefaResposta(t.id(), t.projetoId(), t.titulo(), t.descricao(), t.prazo(),
                t.prioridade(), t.estado(), detalhe.responsaveis(), t.versao());
    }
}
