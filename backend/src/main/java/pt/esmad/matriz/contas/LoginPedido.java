package pt.esmad.matriz.contas;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Corpo de POST /auth/login (openapi.yaml, schema LoginPedido). */
public record LoginPedido(
        @NotBlank(message = "O email é obrigatório.")
        @Email(message = "O email não é válido.")
        @Size(max = 254, message = "O email não pode ter mais de 254 caracteres.")
        String email,

        @NotEmpty(message = "A palavra-passe é obrigatória.")
        String palavraPasse) {
}
