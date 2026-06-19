package com.virtualbank.user.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * The signing key for the platform's tokens. If {@code jwt.private-key} and
 * {@code jwt.public-key} are supplied as PEM, that pair is used so every instance
 * signs with the same key and tokens survive a restart. Otherwise an ephemeral
 * RSA-2048 pair is generated at startup, which is convenient for local runs and
 * tests where token persistence across restarts does not matter.
 */
@Configuration
public class JwtKeyConfig {

    private final RSAKey rsaKey;

    public JwtKeyConfig(@Value("${jwt.private-key:}") String privateKeyPem,
                        @Value("${jwt.public-key:}") String publicKeyPem) {
        this.rsaKey = (privateKeyPem.isBlank() || publicKeyPem.isBlank())
                ? generateEphemeralKey()
                : fromPem(privateKeyPem, publicKeyPem);
    }

    @Bean
    public RSAKey rsaKey() {
        return rsaKey;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        // The encoder signs with this source, so it must hold the private key.
        // The public JWKS endpoint exposes only the public half (see JwksController).
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Could not build JWT decoder from RSA public key", e);
        }
    }

    private static RSAKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return buildRsaKey((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate());
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate RSA key pair", e);
        }
    }

    private static RSAKey fromPem(String privateKeyPem, String publicKeyPem) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(decodePem(publicKeyPem)));
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(privateKeyPem)));
            return buildRsaKey(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse configured RSA key pair", e);
        }
    }

    private static RSAKey buildRsaKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static byte[] decodePem(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
