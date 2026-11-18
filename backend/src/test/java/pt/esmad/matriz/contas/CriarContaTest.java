package pt.esmad.matriz.contas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras da criação de contas de Membro pelo Administrador (US21, RN23, RN04, RI13, R06), sem base de dados. */
class CriarContaTest {

    private static final String TEMPORARIA = "temporaria-123";
    private static final Optional<String> SEM_DOMINIO = Optional.empty();
    private static final Optional<String> EMPRESA = Optional.of("empresa.pt");

    private static List<Erro.Detalhe> erros(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        return erro.detalhes();
    }

    private static List<String> camposComErro(Runnable acao) {
        return erros(acao).stream().map(Erro.Detalhe::campo).toList();
    }

    private static Runnable comEmail(String email, Optional<String> dominio) {
        return () -> Utilizador.novoMembro("Eva Rocha", email, TEMPORARIA, dominio);
    }

    // Crit. 1: conta de Membro ativa, com palavra-passe temporária (PR20)

    @Test
    void criaUmMembroAtivoQueTemDeMudarAPalavraPasse() {
        Utilizador conta = Utilizador.novoMembro("  Eva Rocha  ", "  Eva.Rocha@Empresa.PT ", TEMPORARIA, EMPRESA);

        assertNotNull(conta.id());
        assertEquals("Eva Rocha", conta.nome());
        assertEquals("eva.rocha@empresa.pt", conta.email());
        assertEquals(TipoUtilizador.MEMBRO, conta.tipo());
        assertEquals(EstadoConta.ATIVO, conta.estado());
        assertTrue(conta.deveMudarPalavraPasse());
        assertNotEquals(TEMPORARIA, conta.passwordHash());
        assertTrue(PalavraPasse.confere(TEMPORARIA, conta.passwordHash()));
        assertEquals(0, conta.tentativasFalhadas());
    }

    @Test
    void indicaOsProblemasDeTodosOsCamposDeUmaVez() {
        assertEquals(List.of("nome", "email", "palavraPasseTemporaria"),
                camposComErro(() -> Utilizador.novoMembro(" A ", "sem-arroba", "1234567", EMPRESA)));
        assertEquals(List.of("nome", "email", "palavraPasseTemporaria"),
                camposComErro(() -> Utilizador.novoMembro(null, null, null, SEM_DOMINIO)));
    }

    @Test
    void oNomeTemEntre2E150Caracteres() {
        assertEquals("Al", Utilizador.novoMembro("Al", "al@empresa.pt", TEMPORARIA, EMPRESA).nome());
        assertEquals(150, Utilizador.novoMembro("x".repeat(150), "x@empresa.pt", TEMPORARIA, EMPRESA).nome().length());
        assertEquals(List.of("nome"),
                camposComErro(() -> Utilizador.novoMembro("x".repeat(151), "x@empresa.pt", TEMPORARIA, EMPRESA)));
        assertEquals(List.of("nome"),
                camposComErro(() -> Utilizador.novoMembro("   ", "x@empresa.pt", TEMPORARIA, EMPRESA)));
    }

    @Test
    void aPalavraPasseTemporariaTemPeloMenos8Caracteres() {
        Utilizador.novoMembro("Eva Rocha", "eva@empresa.pt", "12345678", EMPRESA);
        assertEquals(List.of("palavraPasseTemporaria"),
                camposComErro(() -> Utilizador.novoMembro("Eva Rocha", "eva@empresa.pt", "1234567", EMPRESA)));
        assertEquals(List.of("palavraPasseTemporaria"),
                camposComErro(() -> Utilizador.novoMembro("Eva Rocha", "eva@empresa.pt", "", EMPRESA)));
    }

    // Formato do email (RI13)

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", " ​"})
    void oEmailEObrigatorio(String email) {
        List<Erro.Detalhe> erros = erros(comEmail(email, SEM_DOMINIO));
        assertEquals(List.of(new Erro.Detalhe("email", "O email é obrigatório.")), erros);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem-arroba", "@empresa.pt", "eva@", "eva@@empresa.pt", "eva@x@empresa.pt",
            "eva rocha@empresa.pt", "eva@empresa .pt", "eva rocha@empresa.pt"})
    void recusaEmailsSemOFormatoNomeArrobaDominio(String email) {
        assertEquals(List.of(new Erro.Detalhe("email", "Indique um email válido, no formato nome@dominio.")),
                erros(comEmail(email, SEM_DOMINIO)));
    }

    @Test
    void oEmailTemNoMaximo254Caracteres() {
        String dominio = "@exemplo.pt";
        String com254 = "x".repeat(254 - dominio.length()) + dominio;
        assertEquals(com254, Utilizador.novoMembro("Eva Rocha", com254, TEMPORARIA, SEM_DOMINIO).email());
        assertEquals(List.of(new Erro.Detalhe("email", "O email deve ter no máximo 254 caracteres.")),
                erros(comEmail("x" + com254, SEM_DOMINIO)));
    }

    // Domínio da empresa (crit. 2, R06)

    @Test
    void comDominioConfiguradoSoAceitaEmailsDesseDominio() {
        assertEquals("eva@empresa.pt", Utilizador.novoMembro("Eva Rocha", "EVA@EMPRESA.PT", TEMPORARIA, EMPRESA).email());

        for (String outro : List.of("eva@outra.pt", "eva@sub.empresa.pt", "eva@empresa.pt.com", "eva@xempresa.pt")) {
            assertEquals(List.of(new Erro.Detalhe("email", "O email tem de ser do domínio empresa.pt.")),
                    erros(comEmail(outro, EMPRESA)), outro);
        }
    }

    @Test
    void semDominioConfiguradoAceitaQualquerDominio() {
        assertEquals("eva@outra.pt", Utilizador.novoMembro("Eva Rocha", "eva@outra.pt", TEMPORARIA, SEM_DOMINIO).email());
    }

    @Test
    void oDominioConfiguradoIgnoraEspacosArrobaEMaiusculas() {
        assertEquals(Optional.of("empresa.pt"), GestaoContasServico.dominioConfigurado(Optional.of("  @Empresa.PT ")));
        assertEquals(Optional.of("empresa.pt"), GestaoContasServico.dominioConfigurado(Optional.of("empresa.pt")));
        assertEquals(Optional.empty(), GestaoContasServico.dominioConfigurado(Optional.of("   ")));
        assertEquals(Optional.empty(), GestaoContasServico.dominioConfigurado(Optional.of("@")));
        assertEquals(Optional.empty(), GestaoContasServico.dominioConfigurado(Optional.empty()));
    }
}
