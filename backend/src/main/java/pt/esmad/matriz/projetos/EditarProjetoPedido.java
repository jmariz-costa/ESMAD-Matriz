package pt.esmad.matriz.projetos;

import com.fasterxml.jackson.databind.JsonNode;
import pt.esmad.matriz.comum.erros.Erro;
import pt.esmad.matriz.comum.erros.ErroAplicacao;
import pt.esmad.matriz.comum.erros.Validacao;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Corpo de PATCH /projetos/{id} (openapi.yaml, EditarProjetoPedido).
 * É lido a partir do JSON, porque um record não distingue um campo omitido de um campo a null.
 */
final class EditarProjetoPedido {

    private static final Set<String> CAMPOS = Set.of("nome", "descricao");

    private EditarProjetoPedido() {
    }

    static AlteracoesProjeto ler(JsonNode corpo) {
        if (corpo == null || !corpo.isObject()) {
            throw ErroAplicacao.validacao(List.of(new Erro.Detalhe("corpo", "O corpo tem de ser um objeto JSON.")));
        }
        Validacao validacao = new Validacao();
        if (corpo.isEmpty()) {
            validacao.erro("corpo", "Indique pelo menos um campo a alterar.");
        }
        for (Iterator<String> nomes = corpo.fieldNames(); nomes.hasNext(); ) {
            String campo = nomes.next();
            if (!CAMPOS.contains(campo)) {
                validacao.erro(campo, "Campo desconhecido.");
            }
        }
        JsonNode nome = corpo.get("nome");
        if (nome != null && !nome.isTextual()) {
            validacao.erro("nome", nome.isNull() ? "O nome é obrigatório." : "O nome tem de ser texto.");
        }
        JsonNode descricao = corpo.get("descricao");
        if (descricao != null && !descricao.isTextual() && !descricao.isNull()) {
            validacao.erro("descricao", "A descrição tem de ser texto.");
        }
        validacao.lancarSeHouverErros();

        return new AlteracoesProjeto(
                nome != null, nome == null ? null : nome.asText(),
                descricao != null, descricao == null || descricao.isNull() ? null : descricao.asText());
    }
}
