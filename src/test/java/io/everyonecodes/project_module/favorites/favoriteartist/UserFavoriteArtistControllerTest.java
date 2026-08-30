package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.testsupport.AuthTestSupport;
import io.everyonecodes.project_module.users.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class UserFavoriteArtistControllerTest {

    @MockitoBean
    UserFavoriteArtistService service;

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

    @Test
    void saveFavoriteArtistSuccessfully() {
        when(service.saveFavoriteArtist(any(), any())).thenReturn(expectedUser);

        UserResponse response = auth.authenticated(client.post().uri("/favorite-artist/1"), 1L, "bob@ross.com")
                                     .exchange()
                                     .expectStatus().isCreated()
                                     .expectBody(UserResponse.class)
                                     .returnResult()
                                     .getResponseBody();

        assertEquals(expectedUser, response);
    }

    @Test
    void saveFavoriteArtistSelfFollow() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new BadRequestException(ErrorMessages.SELF_FOLLOW));

        auth.authenticated(client.post().uri("/favorite-artist/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void saveFavoriteArtistUserNotFound() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(client.post().uri("/favorite-artist/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNotFound();
    }


    @Test
    void saveFavoriteArtistDuplicate() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new ConflictException(ErrorMessages.DUPLICATE_FOLLOW));

        auth.authenticated(client.post().uri("/favorite-artist/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isEqualTo(409);
    }

    @Test
    void deleteFavoriteArtistSuccessfully() {
        auth.authenticated(client.delete().uri("/favorite-artist/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNoContent();

        verify(service).deleteFavoriteArtist(1L, 1L);
    }

    @Test
    void listFavoriteArtists() {
        when(service.listFavoriteArtists(any())).thenReturn(List.of(expectedUser));

        List<UserResponse> response = auth.authenticated(client.get().uri("/favorite-artist"), 1L, "bob@ross.com")
                                           .exchange()
                                           .expectStatus().isOk()
                                           .expectBody(new ParameterizedTypeReference<List<UserResponse>>() {
                                           })
                                           .returnResult()
                                           .getResponseBody();

        assertEquals(List.of(expectedUser), response);
    }

    @Test
    void listFavoriteArtistsEmpty() {
        when(service.listFavoriteArtists(any())).thenReturn(List.of());

        List<UserResponse> response = auth.authenticated(client.get().uri("/favorite-artist"), 1L, "bob@ross.com")
                                           .exchange()
                                           .expectStatus().isOk()
                                           .expectBody(new ParameterizedTypeReference<List<UserResponse>>() {
                                           })
                                           .returnResult()
                                           .getResponseBody();

        assertEquals(List.of(), response);
    }
}