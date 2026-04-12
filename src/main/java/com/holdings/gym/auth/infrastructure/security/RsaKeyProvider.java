package com.holdings.gym.auth.infrastructure.security;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class RsaKeyProvider {

    @Getter
    private final String keyId;
    
    private final KeyPair keyPair;

    public RsaKeyProvider() {
        this.keyId = UUID.randomUUID().toString();
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            this.keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing RSA Keys", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public Map<String, Object> getJwkMap() {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        return Map.of(
                "kty", "RSA",
                "e", encode(rsaPublicKey.getPublicExponent()),
                "n", encode(rsaPublicKey.getModulus()),
                "kid", keyId,
                "alg", "RS256",
                "use", "sig"
        );
    }

    private String encode(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // Remove leading zero byte added by BigInteger.toByteArray() for positive numbers if highest bit is set
        if (bytes[0] == 0x00 && bytes.length > 1) {
            byte[] withoutLeadingZero = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, withoutLeadingZero, 0, withoutLeadingZero.length);
            bytes = withoutLeadingZero;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
