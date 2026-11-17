package pt.esmad.matriz.projetos;

import org.junit.jupiter.api.Test;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.projetos.GestoresServico.NovoGestor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regras da escolha dos novos Gestores ao desativar um Gestor (US14 crit. 3, RN17), sem base de dados. */
class GestoresServicoTest {

    private static final UUID ANA = UUID.randomUUID();
    private static final UUID BRUNO = UUID.randomUUID();
    private static final UUID CARLA = UUID.randomUUID();
    private static final UUID DESATIVADO = UUID.randomUUID();
    /** A Ana (a desativar), o Bruno e a Carla são Membros ativos; o DESATIVADO não. */
    private static final Predicate<UUID> ATIVOS = Set.of(ANA, BRUNO, CARLA)::contains;

    private final Projeto website = Projeto.criar("Website", null, ANA);
    private final Projeto portal = Projeto.criar("Portal", null, ANA);

    private static List<String> mensagens(Runnable acao) {
        ErroAplicacao erro = assertThrows(ErroAplicacao.class, acao::run);
        assertEquals(400, erro.estadoHttp());
        erro.detalhes().forEach(d -> assertEquals("novosGestores", d.campo()));
        return erro.detalhes().stream().map(Erro.Detalhe::mensagem).toList();
    }

    @Test
    void semProjetosGeridosNaoPrecisaDeNovosGestores() {
        assertEquals(Map.of(), GestoresServico.validar(ANA, List.of(), List.of(), ATIVOS));
    }

    @Test
    void umNovoGestorAtivoPorProjetoEAceite() {
        Map<UUID, UUID> resultado = GestoresServico.validar(ANA, List.of(website, portal),
                List.of(new NovoGestor(website.id(), BRUNO), new NovoGestor(portal.id(), CARLA)), ATIVOS);

        assertEquals(Map.of(website.id(), BRUNO, portal.id(), CARLA), resultado);
    }

    @Test
    void faltarUmProjetoDizQualEMostraTodosOsQueFaltam() {
        assertEquals(List.of("Escolha o novo Gestor do projeto «Website».", "Escolha o novo Gestor do projeto «Portal»."),
                mensagens(() -> GestoresServico.validar(ANA, List.of(website, portal), List.of(), ATIVOS)));
        assertEquals(List.of("Escolha o novo Gestor do projeto «Portal»."),
                mensagens(() -> GestoresServico.validar(ANA, List.of(website, portal),
                        List.of(new NovoGestor(website.id(), BRUNO)), ATIVOS)));
    }

    @Test
    void oNovoGestorTemDeSerUmMembroAtivoDiferenteDaPessoa() {
        String esperado = "O novo Gestor do projeto «Website» tem de ser um Membro ativo, diferente da pessoa a desativar.";
        assertEquals(List.of(esperado), mensagens(() -> GestoresServico.validar(ANA, List.of(website),
                List.of(new NovoGestor(website.id(), ANA)), ATIVOS)));
        assertEquals(List.of(esperado), mensagens(() -> GestoresServico.validar(ANA, List.of(website),
                List.of(new NovoGestor(website.id(), DESATIVADO)), ATIVOS)));
    }

    @Test
    void recusaProjetosQueNaoSaoDaPessoaEProjetosRepetidos() {
        Projeto doBruno = Projeto.criar("Do Bruno", null, BRUNO);
        assertEquals(List.of("Um dos projetos indicados não é gerido por esta pessoa."),
                mensagens(() -> GestoresServico.validar(ANA, List.of(website),
                        List.of(new NovoGestor(website.id(), BRUNO), new NovoGestor(doBruno.id(), CARLA)), ATIVOS)));
        assertEquals(List.of("Indique um só novo Gestor por projeto."),
                mensagens(() -> GestoresServico.validar(ANA, List.of(website),
                        List.of(new NovoGestor(website.id(), BRUNO), new NovoGestor(website.id(), CARLA)), ATIVOS)));
    }
}
