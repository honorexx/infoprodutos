package com.infoprodutos.api.video;

import com.infoprodutos.api.config.JwtProperties;
import com.infoprodutos.api.config.VideoStorageProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Assina URLs de streaming de curta duração para que o &lt;video&gt; possa
 * carregar sem header Authorization. Nunca expõe a storage_key bruta.
 */
@Component
public class StreamUrlSigner {

    private final byte[] secret;
    private final int ttlSeconds;

    public StreamUrlSigner(JwtProperties jwtProperties, VideoStorageProperties storageProperties) {
        this.secret = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = storageProperties.streamUrlTtlSeconds();
    }

    public SignedUrl sign(UUID videoId) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String signature = hmac(videoId + ":" + expiresAt);
        return new SignedUrl(expiresAt, signature, ttlSeconds);
    }

    public boolean isValid(UUID videoId, long expiresAt, String signature) {
        if (expiresAt < Instant.now().getEpochSecond()) {
            return false;
        }
        String expected = hmac(videoId + ":" + expiresAt);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar URL de stream", e);
        }
    }

    public record SignedUrl(long expiresAt, String signature, int ttlSeconds) {}
}
