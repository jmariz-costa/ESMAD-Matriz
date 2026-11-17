package pt.esmad.matriz.comum.autenticacao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint que continua disponível enquanto a pessoa tem uma palavra-passe
 * temporária por mudar (PR20): consultar a sessão e mudar a palavra-passe. Todos os
 * outros endpoints com sessão devolvem 403 {@code PALAVRA_PASSE_TEMPORARIA}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermitidoComPalavraPasseTemporaria {
}
