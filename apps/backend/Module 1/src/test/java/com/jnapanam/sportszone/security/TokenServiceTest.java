package com.jnapanam.sportszone.security;

import static org.assertj.core.api.Assertions.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenServiceTest {
    @Test void acceptsA32ByteSecret() {
        String encoded = Base64.getEncoder().encodeToString(new byte[32]);
        assertThat(TokenService.secretKey(encoded).getAlgorithm()).isEqualTo("HmacSHA256");
    }
    @Test void rejectsShortSecrets() {
        String encoded = Base64.getEncoder().encodeToString(new byte[10]);
        assertThatThrownBy(() -> TokenService.secretKey(encoded)).isInstanceOf(IllegalArgumentException.class);
    }
}
