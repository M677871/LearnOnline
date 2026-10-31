package com.csis231.api.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and reads the signed access tokens. Refuses to start with a key that is too short for HS256.
 */
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long expirationMillis;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expirationMillis) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.parser = Jwts.parserBuilder().setSigningKey(signingKey).build();
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Returns the username a token was issued to.
     *
     * @throws JwtException             if the token is malformed, tampered with or expired
     * @throws IllegalArgumentException if the token is empty
     */
    public String extractUsername(String token) {
        return parser.parseClaimsJws(token).getBody().getSubject();
    }
}
