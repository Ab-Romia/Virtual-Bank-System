package com.virtualbank.user.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the public half of the signing key as a JWK Set so the other services
 * can validate tokens via their {@code jwk-set-uri} without trusting a shared secret.
 */
@RestController
public class JwksController {

    private final JWKSet publicJwkSet;

    public JwksController(RSAKey rsaKey) {
        this.publicJwkSet = new JWKSet(rsaKey.toPublicJWK());
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return publicJwkSet.toJSONObject();
    }
}
