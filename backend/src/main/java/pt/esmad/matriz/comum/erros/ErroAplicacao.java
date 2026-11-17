package pt.esmad.matriz.comum.erros;

import java.util.List;

/**
 * Erro de negócio ou de acesso, convertido numa resposta HTTP com o formato único.
 */
public class ErroAplicacao extends RuntimeException {

    private final int estadoHttp;
    private final CodigoErro codigo;
    private final List<Erro.Detalhe> detalhes;

    public ErroAplicacao(int estadoHttp, CodigoErro codigo, String mensagem) {
        this(estadoHttp, codigo, mensagem, List.of());
    }

    public ErroAplicacao(int estadoHttp, CodigoErro codigo, String mensagem, List<Erro.Detalhe> detalhes) {
        super(mensagem);
        this.estadoHttp = estadoHttp;
        this.codigo = codigo;
        this.detalhes = List.copyOf(detalhes);
    }

    /** 400 com problemas por campo. */
    public static ErroAplicacao validacao(List<Erro.Detalhe> detalhes) {
        return new ErroAplicacao(400, CodigoErro.VALIDACAO, "O pedido tem dados inválidos.", detalhes);
    }

    public static ErroAplicacao naoEncontrado() {
        return new ErroAplicacao(404, CodigoErro.NAO_ENCONTRADO, "O recurso pedido não existe.");
    }

    public static ErroAplicacao semPermissao() {
        return new ErroAplicacao(403, CodigoErro.SEM_PERMISSAO, "Não tem permissão para esta ação.");
    }

    /** O projeto está arquivado e é só de leitura (RN15, RI10). */
    public static ErroAplicacao projetoArquivado() {
        return new ErroAplicacao(409, CodigoErro.PROJETO_ARQUIVADO, "O projeto está arquivado e não pode ser alterado.");
    }

    /** Associação repetida (US04 crit. 3, CE8). */
    public static ErroAplicacao jaExiste(String mensagem) {
        return new ErroAplicacao(409, CodigoErro.JA_EXISTE, mensagem);
    }

    /** O Gestor não pode ser removido do seu projeto (US04 crit. 5, RI01). */
    public static ErroAplicacao gestorNaoRemovivel() {
        return new ErroAplicacao(409, CodigoErro.GESTOR_NAO_REMOVIVEL, "O Gestor não pode ser removido do projeto.");
    }

    /** Outra pessoa alterou o recurso depois de o cliente o obter (D08, US05 crit. 8). */
    public static ErroAplicacao versaoDesatualizada() {
        return new ErroAplicacao(409, CodigoErro.VERSAO_DESATUALIZADA,
                "A tarefa foi alterada por outra pessoa. Recarregue-a antes de guardar.");
    }

    /** Repor o acesso só em contas ativas (US19 crit. 2). */
    public static ErroAplicacao contaDesativada() {
        return new ErroAplicacao(409, CodigoErro.CONTA_DESATIVADA,
                "Esta conta está desativada. Reative-a antes de repor o acesso.");
    }

    /** Enquanto a palavra-passe temporária não for mudada, só se pode mudá-la (PR20). */
    public static ErroAplicacao palavraPasseTemporaria() {
        return new ErroAplicacao(403, CodigoErro.PALAVRA_PASSE_TEMPORARIA,
                "Defina uma palavra-passe nova antes de continuar.");
    }

    public int estadoHttp() {
        return estadoHttp;
    }

    public CodigoErro codigo() {
        return codigo;
    }

    public List<Erro.Detalhe> detalhes() {
        return detalhes;
    }
}
