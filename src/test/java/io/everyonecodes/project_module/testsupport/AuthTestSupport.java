package io.everyonecodes.project_module.testsupport;

import io.everyonecodes.project_module.auth.JwtService;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Shared by every @SpringBootTest controller test that hits a cookie-protected endpoint.
 * Composed into a test class (not extended), so each test keeps its own @SpringBootTest setup.
 */
public class AuthTestSupport {

    private final RestTestClient client;
    private final JwtService jwtService;
    private final String cookieName;

    public AuthTestSupport(RestTestClient client, JwtService jwtService, String cookieName) {
        this.client = client;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    /**
     * Attaches a matching CSRF (Cross-Site Request Forgery) cookie/header pair, mirroring what
     * a real client does: fetch a token from /auth/csrf, then echo it back as X-XSRF-TOKEN on
     * the actual state-changing request.
     */
    public RestTestClient.RequestHeadersSpec<?> withCsrf(RestTestClient.RequestHeadersSpec<?> request) {
        var csrfToken = client.get()
                              .uri("/auth/csrf")
                              .exchange()
                              .returnResult()
                              .getResponseCookies()
                              .getFirst("XSRF-TOKEN")
                              .getValue();

        return request.cookie("XSRF-TOKEN", csrfToken)
                      .header("X-XSRF-TOKEN", csrfToken);
    }

    /**
     * Attaches the auth cookie for (userId, email) on top of withCsrf, for endpoints that need
     * both an authenticated user and CSRF protection.
     */
    public RestTestClient.RequestHeadersSpec<?> authenticated(RestTestClient.RequestHeadersSpec<?> request,
                                                               Long userId, String email) {
        var authToken = jwtService.generateToken(userId, email);
        return withCsrf(request.cookie(cookieName, authToken));
    }
}