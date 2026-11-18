package pt.esmad.matriz.contas;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.projetos.GestoresServico;
import pt.esmad.matriz.tarefas.AtribuicoesServico;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestão das contas de Membro pelo Administrador (US14, US19, US21). Ao desativar, pede ao módulo
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
    private final Optional<String> dominioEmail;

    GestaoContasServico(UtilizadorRepositorio utilizadores, SessaoRepositorio sessoes, GestoresServico gestores,
                        AtribuicoesServico atribuicoes, UtilizadorAtual utilizadorAtual,
                        // Optional: sem MATRIZ_DOMINIO_EMAIL, o domínio não é verificado (IF01)
                        @ConfigProperty(name = "matriz.contas.dominio-email") Optional<String> dominioEmail) {
        this.utilizadores = utilizadores;
        this.sessoes = sessoes;
        this.gestores = gestores;
        this.atribuicoes = atribuicoes;
        this.utilizadorAtual = utilizadorAtual;
        this.dominioEmail = dominioConfigurado(dominioEmail);
    }

    /** O domínio configurado sem espaços, sem "@" no início e em minúsculas; vazio se não houver. */
    static Optional<String> dominioConfigurado(Optional<String> valor) {
        return valor.map(d -> d.strip().toLowerCase(Locale.ROOT))
                .map(d -> d.startsWith("@") ? d.substring(1) : d)
                .filter(d -> !d.isEmpty());
    }

    /**
     * US21: conta de Membro ativa, com palavra-passe temporária a mudar no primeiro login (PR20).
     * Um email que já tem conta (de Membro ou de Administrador, sem distinguir maiúsculas) dá 409.
     */
    @Transactional
    public ContaResposta criar(String nome, String email, String palavraPasseTemporaria) {
        utilizadorAtual.exigirAdministrador();
        Utilizador conta = Utilizador.novoMembro(nome, email, palavraPasseTemporaria, dominioEmail);
        if (utilizadores.procurarPorEmail(conta.email()).isPresent()) {
            throw ErroAplicacao.jaExiste("Este email já tem conta.");
        }
        utilizadores.persist(conta);
        return ContaResposta.de(conta);
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

    /** US19 crit. 1: só o nome muda; o email não. */
    @Transactional
    public ContaResposta editarNome(UUID contaId, String nome) {
        utilizadorAtual.exigirAdministrador();
        Utilizador conta = contaMembro(contaId);
        conta.alterarNome(nome);
        return ContaResposta.de(conta);
    }

    /**
     * US19 crit. 2: palavra-passe temporária (RN04), a mudar no próximo login (PR20); as sessões
     * da pessoa terminam e o bloqueio é limpo (RI21). Uma conta desativada dá 409.
     */
    @Transactional
    public void reporAcesso(UUID contaId, String palavraPasseTemporaria) {
        utilizadorAtual.exigirAdministrador();
        Utilizador conta = contaMembro(contaId);
        Validacao validacao = new Validacao();
        PalavraPasse.validar(palavraPasseTemporaria, "palavraPasseTemporaria", validacao);
        validacao.lancarSeHouverErros();
        conta.reporAcesso(PalavraPasse.hash(palavraPasseTemporaria));
        sessoes.apagarDoUtilizador(conta.id());
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
