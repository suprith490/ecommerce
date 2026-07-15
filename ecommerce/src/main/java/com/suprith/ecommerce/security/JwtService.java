package com.suprith.ecommerce.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Handles creation and validation of JWTs. Tokens are delivered to the browser
 * as an HttpOnly cookie (see AuthServiceImpl / JwtAuthenticationFilter) rather
 * than in the response body, so they are never directly reachable from JS.
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserPrincipal principal, boolean rememberMe) {
        long ttl = rememberMe ? expirationMs * 7 : expirationMs;
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("name", principal.getName())
                .claim("role", principal.getAuthorities().iterator().next().getAuthority())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long resolveMaxAgeSeconds(boolean rememberMe) {
        long ttl = rememberMe ? expirationMs * 7 : expirationMs;
        return ttl / 1000;
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
