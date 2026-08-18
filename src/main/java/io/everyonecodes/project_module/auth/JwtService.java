package io.everyonecodes.project_module.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }


    public String generateToken(Long userId, String email) {
        var now = new Date();
        var expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                   .subject(email)
                   .claim("userId", userId)
                   .issuedAt(now)
                   .expiration(expiry)
                   .signWith(key)
                   .compact();
    }

    public AuthPrincipal parseToken(String token) {
        var claims = Jwts.parser()
                         .verifyWith(key)
                         .build()
                         .parseSignedClaims(token)
                         .getPayload();
        var userId = claims.get("userId", Integer.class).longValue();
        return new AuthPrincipal(userId, claims.getSubject());
    }
}
