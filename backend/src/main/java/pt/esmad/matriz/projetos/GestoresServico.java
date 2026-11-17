package pt.esmad.matriz.projetos;

import jakarta.enterprise.context.ApplicationScoped;
import pt.esmad.matriz.comum.erros.Validacao;
import pt.esmad.matriz.contas.ContasServico;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Gestores dos projetos, pedidos pelo módulo Contas ao desativar uma conta (US14 crit. 3;
 * ADR 0001, regra 5). O Administrador só vê o id, o nome e se o projeto está arquivado (S04).
 * Tem de ser chamado dentro da transação de quem pede, que já verificou que é o Administrador.
 */
@ApplicationScoped
public class GestoresServico {

    static final String CAMPO = "novosGestores";

    private final ProjetoRepositorio projetos;
    private final ContasServico contas;

    GestoresServico(ProjetoRepositorio projetos, ContasServico contas) {
        this.projetos = projetos;
        this.contas = contas;
    }

    /** openapi.yaml, schema ProjetoGerido. */
    public record ProjetoGerido(UUID id, String nome, boolean arquivado) {
    }

    /** O novo Gestor escolhido para um projeto. */
    public record NovoGestor(UUID projetoId, UUID gestorId) {
    }

    /** Projetos de que a pessoa é Gestora, incluindo arquivados, ordenados pelo nome. */
    public List<ProjetoGerido> geridosPor(UUID gestorId) {
        return projetos.geridosPor(gestorId).stream()
                .map(p -> new ProjetoGerido(p.id(), p.nome(), p.arquivado()))
                .toList();
    }

    /**
     * Cada projeto da pessoa passa ao novo Gestor indicado, que fica membro se ainda não for
     * (RN17, RI07). Tem de haver exatamente um novo Gestor por projeto, Membro ativo e diferente
     * da pessoa; senão, 400 no campo {@code novosGestores} e nenhum projeto é alterado.
     */
    public void substituir(UUID gestorAntigo, List<NovoGestor> novos) {
        List<Projeto> geridos = projetos.geridosPor(gestorAntigo);
        Map<UUID, UUID> porProjeto = validar(gestorAntigo, geridos, novos, contas::eMembroAtivo);
        for (Projeto projeto : geridos) {
            projeto.nomearGestor(porProjeto.get(projeto.id()));
        }
    }

    /**
     * As regras da escolha dos novos Gestores, sem base de dados: devolve o novo Gestor de
     * cada projeto, ou lança 400 com todos os problemas encontrados.
     */
    static Map<UUID, UUID> validar(UUID gestorAntigo, List<Projeto> geridos, List<NovoGestor> novos,
                                   Predicate<UUID> eMembroAtivo) {
        Set<UUID> idsGeridos = geridos.stream().map(Projeto::id).collect(Collectors.toSet());
        Map<UUID, UUID> porProjeto = new HashMap<>();
        Validacao validacao = new Validacao();

        for (NovoGestor novo : novos) {
            if (!idsGeridos.contains(novo.projetoId())) {
                validacao.erro(CAMPO, "Um dos projetos indicados não é gerido por esta pessoa.");
            } else if (porProjeto.put(novo.projetoId(), novo.gestorId()) != null) {
                validacao.erro(CAMPO, "Indique um só novo Gestor por projeto.");
            }
        }
        for (Projeto projeto : geridos) {
            UUID novo = porProjeto.get(projeto.id());
            if (novo == null) {
                validacao.erro(CAMPO, "Escolha o novo Gestor do projeto «" + projeto.nome() + "».");
            } else if (novo.equals(gestorAntigo) || !eMembroAtivo.test(novo)) {
                validacao.erro(CAMPO, "O novo Gestor do projeto «" + projeto.nome()
                        + "» tem de ser um Membro ativo, diferente da pessoa a desativar.");
            }
        }
        validacao.lancarSeHouverErros();
        return porProjeto;
    }
}
