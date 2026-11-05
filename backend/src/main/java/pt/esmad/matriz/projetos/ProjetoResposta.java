package pt.esmad.matriz.projetos;

import pt.esmad.matriz.contas.PessoaResumo;
import pt.esmad.matriz.tarefas.ContagensEstado;

import java.util.UUID;

/** openapi.yaml, schema Projeto. */
public record ProjetoResposta(UUID id, String nome, String descricao, boolean arquivado,
                              PessoaResumo gestor, boolean souGestor, ContagensEstado contagens) {

    static ProjetoResposta de(ProjetosServico.ProjetoDetalhe detalhe) {
        Projeto projeto = detalhe.projeto();
        return new ProjetoResposta(projeto.id(), projeto.nome(), projeto.descricao(), projeto.arquivado(),
                detalhe.gestor(), detalhe.souGestor(), detalhe.contagens());
    }
}
