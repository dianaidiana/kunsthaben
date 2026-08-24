package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/auth/csrf")
    CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/auth/login")
    void login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var token = authService.login(request);
        authCookieService.attachAuthCookie(response, token);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/auth/logout")
    void logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
    }
}
