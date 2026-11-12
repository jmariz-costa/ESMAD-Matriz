package pt.esmad.matriz.projetos;

import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.contas.ContasServico;

import java.util.UUID;

/**
 * Verificação de acesso a um projeto, pedida pelos outros módulos (ADR 0001, regra 5),
 * por exemplo Tarefas. Segue a mesma ordem de {@link AcessoProjeto}: Administrador → 403,
 * projeto inexistente → 404, não membro → 403, não Gestor → 403; depois, projeto arquivado → 409.
 * Tem de ser chamado dentro da transação de quem pede.
 */
@ApplicationScoped
public class AcessoProjetosServico {

    private final AcessoProjeto acesso;
    private final ProjetoRepositorio projetos;
    private final ContasServico contas;

    AcessoProjetosServico(AcessoProjeto acesso, ProjetoRepositorio projetos, ContasServico contas) {
        this.acesso = acesso;
        this.projetos = projetos;
        this.contas = contas;
    }

    /** Para consultar: só membros do projeto (RN20); o projeto pode estar arquivado. */
    public Identidade comoMembro(UUID projetoId) {
        return acesso.comoMembro(projetoId).quem();
    }

    /** Para alterar como membro: o projeto não pode estar arquivado (RN15, RI10). */
    public Identidade comoMembroParaAlterar(UUID projetoId) {
        AcessoProjeto.ProjetoAcessivel a = acesso.comoMembro(projetoId);
        a.projeto().garantirAlteravel();
        return a.quem();
    }

    /** Para operações só do Gestor (ex.: eliminar tarefas, RN09), num projeto não arquivado. */
    public Identidade comoGestorParaAlterar(UUID projetoId) {
        AcessoProjeto.ProjetoAcessivel a = acesso.comoGestor(projetoId);
        a.projeto().garantirAlteravel();
        return a.quem();
    }

    /** Só membros do projeto com conta ativa podem ser responsáveis por tarefas (RI02). */
    public boolean eMembroAtivo(UUID projetoId, UUID utilizadorId) {
        return projetos.findByIdOptional(projetoId)
                .filter(p -> p.eMembro(utilizadorId))
                .isPresent() && contas.eMembroAtivo(utilizadorId);
    }
}
