package pt.esmad.matriz.comum.erros;

import java.util.ArrayList;
import java.util.List;

/**
 * Junta os problemas de vários campos e lança um único 400 VALIDACAO.
 * Usado pelos agregados para validar as regras de texto (RN23).
 */
public final class Validacao {

    private final List<Erro.Detalhe> detalhes = new ArrayList<>();

    public Validacao erro(String campo, String mensagem) {
        detalhes.add(new Erro.Detalhe(campo, mensagem));
        return this;
    }

    public void lancarSeHouverErros() {
        if (!detalhes.isEmpty()) {
            throw ErroAplicacao.validacao(detalhes);
        }
    }
}
