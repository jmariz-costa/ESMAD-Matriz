package pt.esmad.matriz.projetos;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.autenticacao.UtilizadorAtual;
import pt.esmad.matriz.comum.paginacao.Pagina;
import pt.esmad.matriz.comum.paginacao.PedidoPagina;
import pt.esmad.matriz.comum.texto.Texto;
import pt.esmad.matriz.contas.ContasServico;
import pt.esmad.matriz.contas.PessoaResumo;
import pt.esmad.matriz.tarefas.ContagemTarefasServico;
import pt.esmad.matriz.tarefas.ContagensEstado;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso dos projetos (US03, US16, US17, US18). As permissões são verificadas
 * aqui, a partir da identidade da sessão (S03).
 */
@ApplicationScoped
public class ProjetosServico {

    private final ProjetoRepositorio projetos;
    private final ContasServico contas;
    private final ContagemTarefasServico contagens;
    private final UtilizadorAtual utilizadorAtual;
    private final AcessoProjeto acesso;

    ProjetosServico(ProjetoRepositorio projetos, ContasServico contas, ContagemTarefasServico contagens,
                    UtilizadorAtual utilizadorAtual, AcessoProjeto acesso) {
        this.projetos = projetos;
        this.contas = contas;
        this.contagens = contagens;
        this.utilizadorAtual = utilizadorAtual;
        this.acesso = acesso;
    }

    /** Um projeto com os dados que a API devolve sobre ele. */
    public record ProjetoDetalhe(Projeto projeto, PessoaResumo gestor, boolean souGestor,
                                 ContagensEstado contagens) {
    }

    /** O Gestor é sempre quem tem a sessão, nunca um valor enviado pelo cliente. */
    @Transactional
    public ProjetoDetalhe criar(String nome, String descricao) {
        Identidade quem = utilizadorAtual.exigirMembro();
        Projeto projeto = Projeto.criar(nome, descricao, quem.id());
        projetos.persist(projeto);
        return detalhe(projeto, quem);
    }

    /**
     * Os projetos não arquivados do utilizador (RN20, US18), com pesquisa pelo nome (US17)
     * e paginação. As contagens e os Gestores são obtidos numa consulta para toda a página.
     */
    @Transactional
    public Pagina<ProjetoDetalhe> listar(String pesquisa, PedidoPagina pedido) {
        Identidade quem = utilizadorAtual.exigirMembro();
        String texto = Texto.normalizar(pesquisa);
        List<Projeto> pagina = projetos.doMembro(quem.id(), texto, pedido);
        long total = projetos.contarDoMembro(quem.id(), texto);

        Map<UUID, PessoaResumo> gestores = contas.pessoas(pagina.stream().map(Projeto::gestorId).toList());
        Map<UUID, ContagensEstado> contagensPorProjeto = contagens.porProjeto(pagina.stream().map(Projeto::id).toList());
        List<ProjetoDetalhe> itens = pagina.stream()
                .map(p -> new ProjetoDetalhe(p, gestores.get(p.gestorId()), p.eGestor(quem.id()),
                        contagensPorProjeto.get(p.id())))
                .toList();
        return new Pagina<>(itens, total, pedido.pagina(), pedido.tamanho());
    }

    /** Só para membros do projeto (RN20); o projeto pode estar arquivado. */
    @Transactional
    public ProjetoDetalhe consultar(UUID projetoId) {
        AcessoProjeto.ProjetoAcessivel a = acesso.comoMembro(projetoId);
        return detalhe(a.projeto(), a.quem());
    }

    /** Só o Gestor (RN22); um projeto arquivado não pode ser editado (RI10). */
    @Transactional
    public ProjetoDetalhe editar(UUID projetoId, AlteracoesProjeto alteracoes) {
        AcessoProjeto.ProjetoAcessivel a = acesso.comoGestor(projetoId);
        a.projeto().editar(alteracoes);
        return detalhe(a.projeto(), a.quem());
    }

    /**
     * Só o Gestor (RN22), também em projetos arquivados. A base de dados apaga em cascata
     * os membros, tarefas, responsáveis, comentários, histórico e tarefas eliminadas (RI17).
     */
    @Transactional
    public void eliminar(UUID projetoId) {
        projetos.delete(acesso.comoGestor(projetoId).projeto());
    }

    private ProjetoDetalhe detalhe(Projeto projeto, Identidade quem) {
        return new ProjetoDetalhe(projeto, contas.pessoa(projeto.gestorId()), projeto.eGestor(quem.id()),
                contagens.doProjeto(projeto.id()));
    }
}
