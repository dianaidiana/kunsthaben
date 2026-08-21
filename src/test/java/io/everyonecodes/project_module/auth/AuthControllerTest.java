package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.auth.dto.LoginRequest;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class AuthControllerTest {

    @MockitoBean
    AuthService authService;

    @Autowired
    RestTestClient client;

    @Value("${app.auth.cookie-secure}")
    boolean cookieSecure;

    @Test
    void loginSuccessfully() {
        var request = new LoginRequest("bob@ross.com", "password123");
        when(authService.login(any())).thenReturn("fake-token");

        client.post()
              .uri("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .exchange()
              .expectStatus().isNoContent()
              .expectCookie().valueEquals("auth_token", "fake-token")
              .expectCookie().httpOnly("auth_token", true)
              .expectCookie().secure("auth_token", cookieSecure)
              .expectCookie().sameSite("auth_token", "Lax")
              .expectCookie().maxAge("auth_token", Duration.ofMillis(86400000));
    }

    @Test
    void loginWithInvalidCredentials() {
        var request = new LoginRequest("bob@ross.com", "wrongpassword");
        when(authService.login(any())).thenThrow(new UnauthorizedException(ErrorMessages.INVALID_CREDENTIALS));

        client.post()
              .uri("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .exchange()
              .expectStatus().isEqualTo(401)
              .expectCookie().doesNotExist("auth_token");
    }

    @Test
    void logoutClearsCookie() {
        client.post()
              .uri("/auth/logout")
              .exchange()
              .expectStatus().isNoContent()
              .expectCookie().valueEquals("auth_token", "")
              .expectCookie().maxAge("auth_token", Duration.ZERO);
    }

}