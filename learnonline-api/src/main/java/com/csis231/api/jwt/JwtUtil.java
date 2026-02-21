package com.csis231.api.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility component for creating and validating JSON Web Tokens (JWT)
 * used by the authentication module of the online learning platform.
 *
 * <p>The signing key and token expiration are configured via the
 * {@code jwt.secret} and {@code jwt.expiration} application properties.</p>
 */

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * Returns the HMAC signing key derived from the configured secret.
     *
     * @return the {@link SecretKey} used to sign and validate tokens
     */

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extracts the username (subject) from the given JWT.
     *
     * @param token the JWT string
     * @return the username stored as the token's subject
     */

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the given JWT.
     *
     * @param token the JWT string
     * @return the expiration date
     */

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the given JWT.
     *
     * @param token          the JWT string
     * @param claimsResolver a function used to read a value from the token's claims
     * @param <T>            the type of the value to be returned
     * @return the value returned by {@code claimsResolver}
     */

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Generates a new JWT for the given username.
     *
     * @param username the username to embed as the token's subject
     * @return a signed JWT string
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    /**
     * Validates a token against the given {@link UserDetails}.
     *
     * <p>The token is considered valid if the username matches and the
     * token has not expired.</p>
     *
     * @param token       the JWT string
     * @param userDetails the user details to validate against
     * @return {@code true} if the token is valid; {@code false} otherwise
     */


    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Validates the structural integrity and signature of a token.
     *
     * <p>This method does not compare the username; it only checks that
     * the token can be parsed using the configured signing key and that
     * the signature is valid.</p>
     *
     * @param token the JWT string
     * @return {@code true} if the token is syntactically valid and signed
     *         with the expected key; {@code false} otherwise
     */

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
