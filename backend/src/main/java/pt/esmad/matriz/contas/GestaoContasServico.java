package pt.esmad.matriz.contas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.projetos.GestoresServico;
import pt.esmad.matriz.tarefas.AtribuicoesServico;

import java.util.List;
import java.util.UUID;

/**
 * Gestão das contas de Membro pelo Administrador (US14). Ao desativar, pede ao módulo
 * Projetos os novos Gestores e ao módulo Tarefas que retire as atribuições (ADR 0001,
 * regra 5; architecture.md §6), tudo na mesma transação.
 */
@ApplicationScoped
public class GestaoContasServico {

    private static final List<EstadoConta> ATIVAS_E_DESATIVADAS = List.of(EstadoConta.ATIVO, EstadoConta.DESATIVADO);

    private final UtilizadorRepositorio utilizadores;
    private final SessaoRepositorio sessoes;
    private final GestoresServico gestores;
    private final AtribuicoesServico atribuicoes;
    private final UtilizadorAtual utilizadorAtual;

    GestaoContasServico(UtilizadorRepositorio utilizadores, SessaoRepositorio sessoes, GestoresServico gestores,
                        AtribuicoesServico atribuicoes, UtilizadorAtual utilizadorAtual) {
        this.utilizadores = utilizadores;
        this.sessoes = sessoes;
        this.gestores = gestores;
        this.atribuicoes = atribuicoes;
        this.utilizadorAtual = utilizadorAtual;
    }

    /** Contas de Membro ativas e desativadas (ou só as de {@code estado}); sem Administradores (RN21). */
    @Transactional
    public Pagina<ContaResposta> listar(String pesquisa, EstadoConta estado, PedidoPagina pedido) {
        utilizadorAtual.exigirAdministrador();
        List<EstadoConta> estados = estado == null ? ATIVAS_E_DESATIVADAS : List.of(estado);
        List<ContaResposta> itens = utilizadores.contasMembro(pesquisa, estados, pedido).stream()
                .map(ContaResposta::de)
                .toList();
        return new Pagina<>(itens, utilizadores.contarContasMembro(pesquisa, estados), pedido.pagina(),
                pedido.tamanho());
    }

    /** Projetos de que a pessoa é Gestora, para nomear os novos Gestores (US14 crit. 3, S04). */
    @Transactional
    public List<GestoresServico.ProjetoGerido> projetosGeridos(UUID contaId) {
        utilizadorAtual.exigirAdministrador();
        return gestores.geridosPor(contaMembro(contaId).id());
    }

    /**
     * US14 crit. 1 a 3: novos Gestores, retirar das tarefas não concluídas (com o Administrador
     * como autor no histórico), desativar e terminar as sessões (PR16). Se algum novo Gestor
     * for inválido, nada é alterado. Uma conta já desativada fica como está.
     */
    @Transactional
    public ContaResposta desativar(UUID contaId, List<GestoresServico.NovoGestor> novosGestores) {
        Identidade administrador = utilizadorAtual.exigirAdministrador();
        Utilizador conta = contaMembro(contaId);
        if (!conta.estaAtivo()) {
            return ContaResposta.de(conta);
        }
        gestores.substituir(conta.id(), novosGestores);
        atribuicoes.retirarDeTodasAsTarefasNaoConcluidas(conta.id(), administrador.id());
        conta.desativar();
        sessoes.apagarDoUtilizador(conta.id());
        return ContaResposta.de(conta);
    }

    /** US14 crit. 6: volta como Membro, sem tarefas nem papel de Gestor (já os perdeu ao desativar). */
    @Transactional
    public ContaResposta reativar(UUID contaId) {
        utilizadorAtual.exigirAdministrador();
        Utilizador conta = contaMembro(contaId);
        conta.reativar();
        return ContaResposta.de(conta);
    }

    /** 404 se não existir; 403 se for de Administrador, incluindo a própria (US14 crit. 7, RN21). */
    private Utilizador contaMembro(UUID contaId) {
        Utilizador conta = utilizadores.findByIdOptional(contaId).orElseThrow(ErroAplicacao::naoEncontrado);
        if (conta.tipo() == TipoUtilizador.ADMINISTRADOR) {
            throw ErroAplicacao.semPermissao();
        }
        return conta;
    }
}
