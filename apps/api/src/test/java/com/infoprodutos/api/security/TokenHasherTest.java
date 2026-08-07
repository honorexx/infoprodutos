package com.infoprodutos.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void rawTokensAreUnique() {
        String a = TokenHasher.generateRawToken();
        String b = TokenHasher.generateRawToken();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).hasSizeGreaterThan(30);
    }

    @Test
    void hashIsDeterministicForSameInput() {
        String raw = "some-raw-token-value";

        assertThat(TokenHasher.hash(raw)).isEqualTo(TokenHasher.hash(raw));
    }

    @Test
    void hashDiffersForDifferentInput() {
        assertThat(TokenHasher.hash("token-a")).isNotEqualTo(TokenHasher.hash("token-b"));
    }

    @Test
    void hashNeverEqualsRawToken() {
        String raw = TokenHasher.generateRawToken();
        assertThat(TokenHasher.hash(raw)).isNotEqualTo(raw);
    }
}
