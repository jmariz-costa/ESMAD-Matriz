package pt.esmad.matriz.contas;

import io.quarkus.elytron.security.common.BcryptUtil;
import pt.esmad.matriz.comum.erros.Validacao;

/** Regras e hash das palavras-passe novas (RN04, S01). */
final class PalavraPasse {

    static final int TAMANHO_MINIMO = 8; // RN04

    private PalavraPasse() {
    }

    /** Regista um erro no campo se a palavra-passe faltar ou tiver menos de 8 caracteres (RN04). */
    static void validar(String palavraPasse, String campo, Validacao validacao) {
        if (palavraPasse == null || palavraPasse.isEmpty()) {
            validacao.erro(campo, "A palavra-passe é obrigatória.");
        } else if (palavraPasse.codePointCount(0, palavraPasse.length()) < TAMANHO_MINIMO) {
            validacao.erro(campo, "A palavra-passe deve ter pelo menos 8 caracteres.");
        }
    }

    /** Hash irreversível, próprio para palavras-passe (bcrypt, S01). */
    static String hash(String palavraPasse) {
        return BcryptUtil.bcryptHash(palavraPasse);
    }

    static boolean confere(String palavraPasse, String hash) {
        return BcryptUtil.matches(palavraPasse, hash);
    }
}
