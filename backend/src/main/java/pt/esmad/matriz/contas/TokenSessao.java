package pt.esmad.matriz.contas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Geração do token de sessão e do hash guardado na base de dados (D06).
 */
final class TokenSessao {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_TOKEN = 32;

    private TokenSessao() {
    }

    /** Token aleatório de 256 bits, em Base64 URL sem padding (43 caracteres). */
    static String gerar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 em hexadecimal (64 caracteres, coluna sessao.token_hash). */
    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
