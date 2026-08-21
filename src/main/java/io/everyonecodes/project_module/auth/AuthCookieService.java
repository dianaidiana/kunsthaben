package io.everyonecodes.project_module.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final long expirationMs;

    public AuthCookieService(@Value("${app.auth.cookie-name}") String cookieName,
                             @Value("${app.auth.cookie-secure}") boolean secure,
                             @Value("${jwt.expiration-ms}") long expirationMs) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.expirationMs = expirationMs;
    }

    public void attachAuthCookie(HttpServletResponse response, String token) {
        var cookie = ResponseCookie.from(cookieName, token)
                                   .httpOnly(true)
                                   .secure(secure)
                                   .sameSite("Lax")
                                   .path("/")
                                   .maxAge(Duration.ofMillis(expirationMs))
                                   .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        var cookie = ResponseCookie.from(cookieName, "")
                                   .httpOnly(true)
                                   .secure(secure)
                                   .sameSite("Lax")
                                   .path("/")
                                   .maxAge(0)
                                   .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
