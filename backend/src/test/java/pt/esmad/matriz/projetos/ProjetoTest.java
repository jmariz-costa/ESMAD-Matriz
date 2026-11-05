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
}
