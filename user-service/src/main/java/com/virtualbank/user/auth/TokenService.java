package com.virtualbank.user.auth;

import com.virtualbank.user.AppUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Mints the RS256 access tokens that carry a user's identity across the platform. */
@Service
public class TokenService {

    static final String ISSUER = "vbank-user-service";
    static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public IssuedToken issue(AppUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(TOKEN_TTL);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, TOKEN_TTL.toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
