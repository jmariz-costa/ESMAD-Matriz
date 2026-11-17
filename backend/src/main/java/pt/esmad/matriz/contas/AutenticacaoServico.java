package pt.esmad.matriz.contas;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import pt.esmad.matriz.comum.autenticacao.Identidade;
import pt.esmad.matriz.comum.erros.CodigoErro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de início e fim de sessão (US02, D06, D09) e de mudar a palavra-passe (US20).
 */
@ApplicationScoped
public class AutenticacaoServico {

    /** Mensagem única para qualquer falha de login (PR03). */
    static final String MENSAGEM_CREDENCIAIS = "Email ou palavra-passe incorretos.";

    /**
     * Hash usado quando o email não tem conta, para que a resposta demore o mesmo
     * tempo e não revele que o email não está registado.
     */
    private static final String HASH_FICTICIO = BcryptUtil.bcryptHash(UUID.randomUUID().toString());

    private final UtilizadorRepositorio utilizadores;
    private final SessaoRepositorio sessoes;

    public AutenticacaoServico(UtilizadorRepositorio utilizadores, SessaoRepositorio sessoes) {
        this.utilizadores = utilizadores;
        this.sessoes = sessoes;
    }

    public record SessaoIniciada(Utilizador utilizador, String token) {
    }

    /**
     * Verifica as credenciais e cria a sessão. Qualquer falha devolve o mesmo erro (PR03).
     * A transação não é revertida no erro, para que a falha fique contada (D09).
     */
    @Transactional(dontRollbackOn = ErroAplicacao.class)
    public SessaoIniciada entrar(String email, String palavraPasse) {
        Instant agora = Instant.now();
        Optional<Utilizador> encontrado = utilizadores.procurarPorEmail(email);

        String hash = encontrado.map(Utilizador::passwordHash).orElse(HASH_FICTICIO);
        boolean palavraPasseCerta = BcryptUtil.matches(palavraPasse, hash);

        if (encontrado.isEmpty()) {
            throw credenciaisInvalidas();
        }
        Utilizador utilizador = encontrado.get();
        if (utilizador.estaBloqueado(agora)) {
            throw credenciaisInvalidas();
        }
        if (!palavraPasseCerta) {
            utilizador.registarFalha(agora);
            throw credenciaisInvalidas();
        }
        if (!utilizador.estaAtivo()) {
            throw credenciaisInvalidas();
        }

        utilizador.registarSucesso();
        String token = TokenSessao.gerar();
        sessoes.persist(Sessao.iniciar(utilizador.id(), TokenSessao.hash(token), agora));
        return new SessaoIniciada(utilizador, token);
    }

    /**
     * Valida o token do cookie e renova o prazo da sessão (D06). Uma sessão expirada
     * é apagada. Uma conta que deixou de estar ativa também deixa de ter acesso.
     */
    @Transactional
    public Optional<Identidade> validarSessao(String token) {
        Instant agora = Instant.now();
        Optional<Sessao> encontrada = sessoes.procurarPorTokenHash(TokenSessao.hash(token));
        if (encontrada.isEmpty()) {
            return Optional.empty();
        }
        Sessao sessao = encontrada.get();
        if (sessao.expirou(agora)) {
            sessoes.delete(sessao);
            return Optional.empty();
        }
        Optional<Utilizador> utilizador = utilizadores.findByIdOptional(sessao.utilizadorId())
                .filter(Utilizador::estaAtivo);
        if (utilizador.isEmpty()) {
            sessoes.delete(sessao);
            return Optional.empty();
        }
        sessao.renovar(agora);
        Utilizador u = utilizador.get();
        return Optional.of(new Identidade(u.id(), u.nome(), u.email(),
                u.tipo() == TipoUtilizador.ADMINISTRADOR, u.deveMudarPalavraPasse()));
    }

    /**
     * US20: só Membros (o Administrador recebe 403, RN21). A atual tem de estar certa e a nova
     * ter pelo menos 8 caracteres (RN04) e ser diferente; deixa de ser temporária (PR20) e as
     * outras sessões terminam, mantendo a do token atual (PR21).
     */
    @Transactional
    public void mudarPalavraPasse(Identidade quem, String tokenAtual, String atual, String nova) {
        if (quem.administrador()) {
            throw ErroAplicacao.semPermissao();
        }
        Utilizador utilizador = utilizadores.findById(quem.id());
        Validacao validacao = new Validacao();
        if (atual == null || atual.isEmpty()) {
            validacao.erro("palavraPasseAtual", "Indique a palavra-passe atual.");
        } else if (!PalavraPasse.confere(atual, utilizador.passwordHash())) {
            validacao.erro("palavraPasseAtual", "A palavra-passe atual está errada.");
        } else if (atual.equals(nova)) {
            validacao.erro("palavraPasseNova", "A palavra-passe nova tem de ser diferente da atual.");
        }
        PalavraPasse.validar(nova, "palavraPasseNova", validacao);
        validacao.lancarSeHouverErros();
        utilizador.definirPalavraPasse(PalavraPasse.hash(nova));
        sessoes.apagarOutrasDoUtilizador(utilizador.id(), TokenSessao.hash(tokenAtual));
    }

    /** Apaga a sessão do token, se existir (US02 crit. 4). */
    @Transactional
    public void sair(String token) {
        sessoes.delete("tokenHash", TokenSessao.hash(token));
    }

    private static ErroAplicacao credenciaisInvalidas() {
        return new ErroAplicacao(401, CodigoErro.CREDENCIAIS_INVALIDAS, MENSAGEM_CREDENCIAIS);
    }
}
