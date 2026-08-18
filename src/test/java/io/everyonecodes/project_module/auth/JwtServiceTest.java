package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.users.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String secret = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzMjU2";
    private static final String otherSecret = "b3RoZXItdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYWxzby1sb25nLWVub3VnaA==";
    private static final User user = new User(1L, "Test User", "test@example.com", "hash", null, null, "Vienna", "1010", null, null);

    JwtService service;

    @BeforeEach
    void setup() {
        service = new JwtService(secret, 60000);
    }

    @Test
    void generateAndParseTokenReturnsSameUserIdAndEmail() {
        var token = service.generateToken(user);
        var principal = service.parseToken(token);

        assertEquals(user.getId(), principal.id());
        assertEquals(user.getEmail(), principal.email());
    }

    @Test
    void parseExpiredTokenThrows() {
        var expiredService = new JwtService(secret, -1000);
        var token = expiredService.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> service.parseToken(token));
    }

    @Test
    void parseTokenSignedWithDifferentKeyThrows() {
        var otherService = new JwtService(otherSecret, 60000);
        var token = otherService.generateToken(user);

        assertThrows(SignatureException.class, () -> service.parseToken(token));
    }

    @Test
    void parseMalformedTokenThrows() {
        assertThrows(MalformedJwtException.class, () -> service.parseToken("not-a-real-token"));
    }

}