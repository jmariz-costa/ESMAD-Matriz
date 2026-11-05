package pt.esmad.matriz.comum.autenticacao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um endpoint que não exige sessão (no contrato: {@code security: []}).
 * Todos os outros endpoints exigem uma sessão válida.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Publico {
}
