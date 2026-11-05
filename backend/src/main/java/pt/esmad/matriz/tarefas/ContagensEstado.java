package pt.esmad.matriz.tarefas;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Número de tarefas de um projeto em cada estado (openapi.yaml, ContagensEstado; US18). */
public record ContagensEstado(
        @JsonProperty("A_FAZER") long aFazer,
        @JsonProperty("EM_CURSO") long emCurso,
        @JsonProperty("EM_REVISAO") long emRevisao,
        @JsonProperty("CONCLUIDA") long concluida) {

    public static final ContagensEstado ZERO = new ContagensEstado(0, 0, 0, 0);

    ContagensEstado somar(String estado, long quantidade) {
        return switch (estado) {
            case "A_FAZER" -> new ContagensEstado(aFazer + quantidade, emCurso, emRevisao, concluida);
            case "EM_CURSO" -> new ContagensEstado(aFazer, emCurso + quantidade, emRevisao, concluida);
            case "EM_REVISAO" -> new ContagensEstado(aFazer, emCurso, emRevisao + quantidade, concluida);
            case "CONCLUIDA" -> new ContagensEstado(aFazer, emCurso, emRevisao, concluida + quantidade);
            default -> throw new IllegalStateException("Estado desconhecido: " + estado);
        };
    }
}
