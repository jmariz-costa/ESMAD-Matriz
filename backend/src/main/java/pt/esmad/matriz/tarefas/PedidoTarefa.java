package pt.esmad.matriz.tarefas;

import com.fasterxml.jackson.databind.JsonNode;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Lê os corpos de POST /projetos/{id}/tarefas e de PATCH /tarefas/{id}
 * (openapi.yaml, CriarTarefaPedido e EditarTarefaPedido).
 * É lido a partir do JSON para indicar o campo com problemas (ex.: uma data inválida)
 * e para distinguir um campo omitido de um campo a null.
 * As regras do texto (RN23) são verificadas pelo agregado {@link Tarefa}.
 */
final class PedidoTarefa {

    private static final Set<String> CAMPOS_CRIAR = Set.of("titulo", "descricao", "prazo", "prioridade");
    private static final Set<String> CAMPOS_EDITAR =
            Set.of("versao", "titulo", "descricao", "prazo", "prioridade", "estado");

    private PedidoTarefa() {
    }

    static NovaTarefa lerCriacao(JsonNode corpo) {
        exigirObjeto(corpo);
        Validacao validacao = new Validacao();
        recusarDesconhecidos(corpo, CAMPOS_CRIAR, validacao);
        String titulo = texto(corpo, "titulo", validacao);
        String descricao = texto(corpo, "descricao", validacao);
        LocalDate prazo = data(corpo, "prazo", validacao);
        Prioridade prioridade = prioridade(corpo, validacao);
        validacao.lancarSeHouverErros();
        return new NovaTarefa(titulo, descricao, prazo, prioridade);
    }

    /** {@code versao} obrigatória e pelo menos um campo a alterar; título e estado não podem ser null. */
    static AlteracoesTarefa lerEdicao(JsonNode corpo) {
        exigirObjeto(corpo);
        Validacao validacao = new Validacao();
        recusarDesconhecidos(corpo, CAMPOS_EDITAR, validacao);

        JsonNode versao = corpo.get("versao");
        if (versao == null || versao.isNull()) {
            validacao.erro("versao", "A versão é obrigatória.");
        } else if (!versao.canConvertToExactIntegral() || !versao.canConvertToInt() || versao.asInt() < 0) {
            validacao.erro("versao", "A versão tem de ser um número inteiro a partir de 0.");
        }
        if (corpo.size() < 2) {
            validacao.erro("corpo", "Indique pelo menos um campo a alterar.");
        }
        JsonNode titulo = corpo.get("titulo");
        if (titulo != null && titulo.isNull()) {
            validacao.erro("titulo", "O título é obrigatório.");
        }
        String novoTitulo = texto(corpo, "titulo", validacao);
        String descricao = texto(corpo, "descricao", validacao);
        LocalDate prazo = data(corpo, "prazo", validacao);
        Prioridade prioridade = prioridade(corpo, validacao);
        Estado estado = estado(corpo, validacao);
        validacao.lancarSeHouverErros();

        return new AlteracoesTarefa(versao.asInt(), novoTitulo,
                corpo.has("descricao"), descricao,
                corpo.has("prazo"), prazo,
                corpo.has("prioridade"), prioridade,
                estado);
    }

    private static Estado estado(JsonNode corpo, Validacao validacao) {
        JsonNode valor = corpo.get("estado");
        if (valor == null) {
            return null;
        }
        Estado estado = valor.isTextual() ? valorDe(Estado.class, valor.asText()) : null;
        if (estado == null) {
            validacao.erro("estado", "O estado tem de ser A_FAZER, EM_CURSO, EM_REVISAO ou CONCLUIDA.");
        }
        return estado;
    }

    static void exigirObjeto(JsonNode corpo) {
        if (corpo == null || !corpo.isObject()) {
            throw ErroAplicacao.validacao(List.of(new Erro.Detalhe("corpo", "O corpo tem de ser um objeto JSON.")));
        }
    }

    static void recusarDesconhecidos(JsonNode corpo, Set<String> permitidos, Validacao validacao) {
        for (Iterator<String> nomes = corpo.fieldNames(); nomes.hasNext(); ) {
            String campo = nomes.next();
            if (!permitidos.contains(campo)) {
                validacao.erro(campo, "Campo desconhecido.");
            }
        }
    }

    /** Texto ou null; ausente também dá null. A obrigatoriedade é verificada pelo agregado. */
    static String texto(JsonNode corpo, String campo, Validacao validacao) {
        JsonNode valor = corpo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (!valor.isTextual()) {
            validacao.erro(campo, "Este campo tem de ser texto.");
            return null;
        }
        return valor.asText();
    }

    /** Data no formato AAAA-MM-DD; qualquer data válida, incluindo passadas (RN12). */
    static LocalDate data(JsonNode corpo, String campo, Validacao validacao) {
        JsonNode valor = corpo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        try {
            if (valor.isTextual()) {
                return LocalDate.parse(valor.asText());
            }
        } catch (DateTimeParseException e) {
            // tratado abaixo
        }
        validacao.erro(campo, "O prazo tem de ser uma data válida no formato AAAA-MM-DD.");
        return null;
    }

    static Prioridade prioridade(JsonNode corpo, Validacao validacao) {
        JsonNode valor = corpo.get("prioridade");
        if (valor == null || valor.isNull()) {
            return null;
        }
        Prioridade prioridade = valor.isTextual() ? valorDe(Prioridade.class, valor.asText()) : null;
        if (prioridade == null) {
            validacao.erro("prioridade", "A prioridade tem de ser BAIXA, MEDIA ou ALTA.");
        }
        return prioridade;
    }

    /** O valor do enum com esse nome exato, ou {@code null} se não existir. */
    static <E extends Enum<E>> E valorDe(Class<E> tipo, String nome) {
        for (E valor : tipo.getEnumConstants()) {
            if (valor.name().equals(nome)) {
                return valor;
            }
        }
        return null;
    }
}
