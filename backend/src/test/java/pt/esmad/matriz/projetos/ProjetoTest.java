package pt.esmad.matriz.projetos;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Erro;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras do agregado Projeto (RN23, RI01), sem base de dados. */
class ProjetoTest {

    private static final UUID GESTOR = UUID.randomUUID();

    private static Erro.Detalhe unicoErro(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        assertEquals(1, erro.detalhes().size());
        return erro.detalhes().getFirst();
    }

    @Test
    void quemCriaFicaGestorEMembro() {
        Projeto projeto = Projeto.criar("Portal do Cliente", null, GESTOR);
        assertTrue(projeto.eGestor(GESTOR));
        assertTrue(projeto.eMembro(GESTOR));
        assertFalse(projeto.eMembro(UUID.randomUUID()));
        assertFalse(projeto.arquivado());
    }

    @Test
    void espacosNasPontasNaoContam() {
        Projeto projeto = Projeto.criar("   abc   ", "  texto  ", GESTOR);
        assertEquals("abc", projeto.nome());
        assertEquals("texto", projeto.descricao());
        assertEquals("nome", unicoErro(() -> Projeto.criar("  ab  ", null, GESTOR)).campo());
    }

    @Test
    void limitesDoNome() {
        Projeto.criar("a".repeat(3), null, GESTOR);
        Projeto.criar("a".repeat(100), null, GESTOR);
        assertEquals("O nome deve ter entre 3 e 100 caracteres.",
                unicoErro(() -> Projeto.criar("a".repeat(101), null, GESTOR)).mensagem());
    }

    @Test
    void nomeObrigatorio() {
        assertEquals("O nome é obrigatório.", unicoErro(() -> Projeto.criar(null, null, GESTOR)).mensagem());
        assertEquals("O nome é obrigatório.", unicoErro(() -> Projeto.criar("   ", null, GESTOR)).mensagem());
    }

    @Test
    void limiteDaDescricao() {
        Projeto.criar("Projeto", "d".repeat(1000), GESTOR);
        Erro.Detalhe erro = unicoErro(() -> Projeto.criar("Projeto", "d".repeat(1001), GESTOR));
        assertEquals("descricao", erro.campo());
    }

    @Test
    void descricaoSoComEspacosFicaSemDescricao() {
        assertNull(Projeto.criar("Projeto", "   ", GESTOR).descricao());
    }

    /** Um emoji ocupa duas unidades em Java, mas é um só caracter, como na coluna VARCHAR(100). */
    @Test
    void contaCaracteresEnaoUnidadesJava() {
        Projeto.criar("😀".repeat(100), null, GESTOR);
        assertEquals("nome", unicoErro(() -> Projeto.criar("😀".repeat(101), null, GESTOR)).campo());
    }

    @Test
    void reportaTodosOsCamposInvalidos() {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class,
                () -> Projeto.criar("x", "d".repeat(1001), GESTOR));
        assertEquals(2, erro.detalhes().size());
    }

    @Test
    void adicionarMembroRepetidoDevolve409() {
        Projeto projeto = Projeto.criar("Projeto", null, GESTOR);
        UUID outro = UUID.randomUUID();
        projeto.adicionarMembro(outro);
        assertTrue(projeto.eMembro(outro));
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> projeto.adicionarMembro(outro));
        assertEquals(409, erro.estadoHttp());
        assertEquals(pt.esmad.matriz.comum.erros.CodigoErro.JA_EXISTE, erro.codigo());
    }

    @Test
    void oGestorNaoPodeSerRemovido() {
        Projeto projeto = Projeto.criar("Projeto", null, GESTOR);
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, () -> projeto.removerMembro(GESTOR));
        assertEquals(pt.esmad.matriz.comum.erros.CodigoErro.GESTOR_NAO_REMOVIVEL, erro.codigo());
        assertTrue(projeto.eMembro(GESTOR));
    }

    @Test
    void removerQuemNaoEMembroDevolve404() {
        Projeto projeto = Projeto.criar("Projeto", null, GESTOR);
        UUID outro = UUID.randomUUID();
        assertEquals(404, assertThrows(ErroAplicacao.class, () -> projeto.removerMembro(outro)).estadoHttp());
        projeto.adicionarMembro(outro);
        projeto.removerMembro(outro);
        assertFalse(projeto.eMembro(outro));
    }

    // Novo Gestor nomeado pelo Administrador (US14 crit. 3 e 4, RI07)

    @Test
    void novoGestorQueNaoEraMembroPassaASerEOAntigoContinuaMembro() {
        Projeto projeto = Projeto.criar("Portal do Cliente", null, GESTOR);
        UUID novo = UUID.randomUUID();

        projeto.nomearGestor(novo);

        assertTrue(projeto.eGestor(novo));
        assertTrue(projeto.eMembro(novo));
        assertFalse(projeto.eGestor(GESTOR));
        assertTrue(projeto.eMembro(GESTOR));
    }

    @Test
    void nomearGestorFuncionaNumProjetoArquivado() {
        Projeto projeto = Projeto.criar("Portal do Cliente", null, GESTOR);
        projeto.adicionarMembro(UUID.randomUUID());
        UUID novo = UUID.randomUUID();
        projeto.adicionarMembro(novo);
        arquivar(projeto);

        projeto.nomearGestor(novo);

        assertTrue(projeto.eGestor(novo));
        assertEquals(3, projeto.membros().size());
    }

    private static void arquivar(Projeto projeto) {
        try {
            var campo = Projeto.class.getDeclaredField("arquivado");
            campo.setAccessible(true);
            campo.setBoolean(projeto, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
