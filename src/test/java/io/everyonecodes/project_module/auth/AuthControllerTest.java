package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.auth.dto.AuthResponse;
import io.everyonecodes.project_module.auth.dto.LoginRequest;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class AuthControllerTest {

    @MockitoBean
    AuthService authService;

    @Autowired
    RestTestClient client;

    @Test
    void loginSuccessfully() {
        var request = new LoginRequest("bob@ross.com", "password123");
        var expectedResponse = new AuthResponse("fake-token");
        when(authService.login(any())).thenReturn(expectedResponse);

        AuthResponse response = client.post()
                                      .uri("/auth/login")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .body(request)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(AuthResponse.class)
                                      .returnResult()
                                      .getResponseBody();
        assertEquals(expectedResponse, response);
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
              .expectStatus().isEqualTo(401);
    }

}