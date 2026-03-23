package com.libreuml.backend.infrastructure.security;

import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAdapter implements TokenProviderPort {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:900000}")
    private long jwtExpiration;

    @PostConstruct
    void validateSecret() {
        byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
        if (secretBytes.length < 64) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 64 bytes (512 bits) of Base64-encoded random data. " +
                "Generate with: openssl rand -base64 64"
            );
        }
    }

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole());
        claims.put("pwdVersion", user.getPasswordVersion());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
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

    @Override
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public int getPwdVersionFromToken(String token) {
        return parseClaims(token).get("pwdVersion", Integer.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Base64-decode the secret before deriving the HMAC key.
    // Using raw string bytes would produce a key with far less entropy than intended.
    private Key getSigningKey() {
        byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
