package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.auth.AuthService;
import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.testsupport.AuthTestSupport;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class UserControllerTest {

    @MockitoBean
    UserService service;

    @MockitoBean
    AuthService authService;

    @Autowired
    RestTestClient client;

    @Autowired
    JwtService jwtService;

    @Value("${app.auth.cookie-name}")
    String cookieName;

    AuthTestSupport auth;

    @BeforeEach
    void setUpAuth() {
        auth = new AuthTestSupport(client, jwtService, cookieName);
    }

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final UserResponse expectedUser = new UserResponse(1L, "Bob Ross", "bob@ross.com",
            null, null, "Vienna", "1020", null, createdAt);

    private final MultipartBodyBuilder builder = new MultipartBodyBuilder();

    private void addFilePart(String filename) {
        builder.part("file", new ByteArrayResource("fake image bytes".getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.IMAGE_JPEG);
    }

    @Test
    void registerSuccessfully() {
        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123");
        when(service.register(any())).thenReturn(expectedUser);
        when(authService.issueToken(1L, "bob@ross.com")).thenReturn("fake-token");

        UserResponse response = auth.withCsrf(
                client.post().uri("/user/register").contentType(MediaType.APPLICATION_JSON).body(request))
                .exchange()
                .expectStatus().isCreated()
                .expectCookie().valueEquals("auth_token", "fake-token")
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertEquals(expectedUser, response);
    }

    @Test
    void registerWithTakenEmail() {
        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123");
        when(service.register(any())).thenThrow(new ConflictException(ErrorMessages.EMAIL_ALREADY_TAKEN));

        auth.withCsrf(client.post().uri("/user/register").contentType(MediaType.APPLICATION_JSON).body(request))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getByExistentId() {
        when(service.getById(eq(1L))).thenReturn(Optional.of(expectedUser));

        UserResponse response = client.get()
                                      .uri("/user/1")
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(UserResponse.class)
                                      .returnResult()
                                      .getResponseBody();
        assertEquals(expectedUser, response);
    }

    @Test
    void getByUnexistentId() {
        when(service.getById(eq(1L))).thenReturn(Optional.empty());

        client.get()
              .uri("/user/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void updateSuccessfully() {
        var request = new UserUpdateRequest("Bob Ross", "Vienna", "1020", "Updated bio");
        var expectedUserWithBio = new UserResponse(1L, "Bob Ross", "bob@ross.com",
                null, null, "Vienna", "1020", "Updated bio", createdAt);
        when(service.update(eq(1L), any())).thenReturn(expectedUserWithBio);

        UserResponse response = auth.authenticated(
                client.put().uri("/user/1").contentType(MediaType.APPLICATION_JSON).body(request),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertEquals(expectedUserWithBio, response);
    }

    @Test
    void updateUnexistentUser() {
        var request = new UserUpdateRequest("Bob Ross", "Vienna", "1020", "Updated bio");
        when(service.update(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(
                client.put().uri("/user/1").contentType(MediaType.APPLICATION_JSON).body(request),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateNotOwnedProfile() {
        var request = new UserUpdateRequest("Bob Ross", "Vienna", "1020", "Updated bio");
        when(service.update(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(
                client.put().uri("/user/1").contentType(MediaType.APPLICATION_JSON).body(request),
                2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateAvatarSuccessfully() {
        addFilePart("avatar.jpg");
        var expectedUserWithAvatar = new UserResponse(1L, "Bob Ross", "bob@ross.com",
                null, "https://bucket.s3.region.amazonaws.com/avatar.jpg", "Vienna", "1020", null, createdAt);
        when(service.updateAvatar(eq(1L), any())).thenReturn(expectedUserWithAvatar);

        UserResponse response = auth.authenticated(
                client.put().uri("/user/1/avatar").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertEquals(expectedUserWithAvatar, response);
    }

    @Test
    void updateAvatarUnexistentUser() {
        addFilePart("avatar.jpg");
        when(service.updateAvatar(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(
                client.put().uri("/user/1/avatar").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateAvatarNotOwnedProfile() {
        addFilePart("avatar.jpg");

        auth.authenticated(
                client.put().uri("/user/1/avatar").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateBannerSuccessfully() {
        addFilePart("banner.jpg");
        var expectedUserWithBanner = new UserResponse(1L, "Bob Ross", "bob@ross.com",
                "https://bucket.s3.region.amazonaws.com/banner.jpg", null, "Vienna", "1020", null, createdAt);
        when(service.updateBanner(eq(1L), any())).thenReturn(expectedUserWithBanner);

        UserResponse response = auth.authenticated(
                client.put().uri("/user/1/banner").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertEquals(expectedUserWithBanner, response);
    }

    @Test
    void updateBannerUnexistentUser() {
        addFilePart("banner.jpg");
        when(service.updateBanner(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(
                client.put().uri("/user/1/banner").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateBannerNotOwnedProfile() {
        addFilePart("banner.jpg");

        auth.authenticated(
                client.put().uri("/user/1/banner").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteAvatarSuccessfully() {
        auth.authenticated(client.delete().uri("/user/1/avatar"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNoContent();

        verify(service).deleteAvatar(1L);
    }

    @Test
    void deleteAvatarUnexistentUser() {
        org.mockito.Mockito.doThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND))
                           .when(service).deleteAvatar(1L);

        auth.authenticated(client.delete().uri("/user/1/avatar"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteAvatarNotOwnedProfile() {
        auth.authenticated(client.delete().uri("/user/1/avatar"), 2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteBannerSuccessfully() {
        auth.authenticated(client.delete().uri("/user/1/banner"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNoContent();

        verify(service).deleteBanner(1L);
    }

    @Test
    void deleteBannerUnexistentUser() {
        org.mockito.Mockito.doThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND))
                           .when(service).deleteBanner(1L);

        auth.authenticated(client.delete().uri("/user/1/banner"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteBannerNotOwnedProfile() {
        auth.authenticated(client.delete().uri("/user/1/banner"), 2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteSuccessfully() {
        auth.authenticated(client.delete().uri("/user/1"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNoContent();

        verify(service).delete(1L);
    }

    @Test
    void deleteUnexistentUser() {
        org.mockito.Mockito.doThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND))
                           .when(service).delete(1L);

        auth.authenticated(client.delete().uri("/user/1"), 1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteNotOwnedProfile() {
        auth.authenticated(client.delete().uri("/user/1"), 2L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

}