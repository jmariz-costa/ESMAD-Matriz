package pt.esmad.matriz.contas;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Operações do módulo Contas usadas pelos outros módulos (ADR 0001, regra 5).
 */
@ApplicationScoped
public class ContasServico {

    private final UtilizadorRepositorio utilizadores;

    public ContasServico(UtilizadorRepositorio utilizadores) {
        this.utilizadores = utilizadores;
    }

    /** Resumo de cada utilizador pedido, indexado pelo id. Ids inexistentes ficam de fora. */
    public Map<UUID, PessoaResumo> pessoas(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return utilizadores.list("id in ?1", ids).stream()
                .map(u -> new PessoaResumo(u.id(), u.nome()))
                .collect(Collectors.toMap(PessoaResumo::id, Function.identity()));
    }

    public PessoaResumo pessoa(UUID id) {
        PessoaResumo pessoa = pessoas(java.util.List.of(id)).get(id);
        if (pessoa == null) {
            throw new IllegalStateException("Utilizador inexistente: " + id);
        }
        return pessoa;
    }
}
