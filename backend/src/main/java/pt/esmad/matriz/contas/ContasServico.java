package pt.esmad.matriz.contas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Operações do módulo Contas usadas pelos outros módulos (ADR 0001, regra 5)
 * e a pesquisa de Membros para adicionar a um projeto.
 */
@ApplicationScoped
public class ContasServico {

    private final UtilizadorRepositorio utilizadores;
    private final UtilizadorAtual utilizadorAtual;

    public ContasServico(UtilizadorRepositorio utilizadores, UtilizadorAtual utilizadorAtual) {
        this.utilizadores = utilizadores;
        this.utilizadorAtual = utilizadorAtual;
    }

    /** Resumo de cada utilizador pedido, indexado pelo id. Ids inexistentes ficam de fora. */
    public Map<UUID, PessoaResumo> pessoas(Collection<UUID> ids) {
        return contas(ids).values().stream()
                .collect(Collectors.toMap(ContaResumo::id, c -> new PessoaResumo(c.id(), c.nome())));
    }

    public PessoaResumo pessoa(UUID id) {
        PessoaResumo pessoa = pessoas(List.of(id)).get(id);
        if (pessoa == null) {
            throw new IllegalStateException("Utilizador inexistente: " + id);
        }
        return pessoa;
    }

    /** Dados das contas pedidas (para listar membros), indexados pelo id. */
    public Map<UUID, ContaResumo> contas(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return utilizadores.list("id in ?1", ids).stream()
                .map(u -> new ContaResumo(u.id(), u.nome(), u.email(), u.estaAtivo()))
                .collect(Collectors.toMap(ContaResumo::id, Function.identity()));
    }

    /** Só Membros com conta ativa podem ser adicionados a um projeto (RI16). */
    public boolean eMembroAtivo(UUID id) {
        return utilizadores.findByIdOptional(id)
                .filter(u -> u.tipo() == TipoUtilizador.MEMBRO && u.estaAtivo())
                .isPresent();
    }

    /**
     * Membros com conta ativa, para o Gestor escolher quem adicionar a um projeto.
     * O Administrador não participa em projetos (RN19) e recebe 403.
     */
    @Transactional
    public Pagina<UtilizadorResumo> procurarMembrosAtivos(String pesquisa, PedidoPagina pedido) {
        utilizadorAtual.exigirMembro();
        List<UtilizadorResumo> itens = utilizadores.membrosAtivos(pesquisa, pedido).stream()
                .map(u -> new UtilizadorResumo(u.id(), u.nome(), u.email()))
                .toList();
        return new Pagina<>(itens, utilizadores.contarMembrosAtivos(pesquisa), pedido.pagina(), pedido.tamanho());
    }
}
