package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final UserResponse expectedUser = new UserResponse(1L, "Bob Ross", "bob@ross.com",
            null, null, "Vienna", "1020", null, createdAt);

    @Test
    void saveFavoriteArtistSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.saveFavoriteArtist(any(), any())).thenReturn(expectedUser);

        UserResponse response = client.post()
                                      .uri("/user/1/favorite-artist/1")
                                      .header("Authorization", "Bearer " + token)
                                      .exchange()
                                      .expectStatus().isCreated()
                                      .expectBody(UserResponse.class)
                                      .returnResult()
                                      .getResponseBody();

        assertEquals(expectedUser, response);
    }

    @Test
    void saveFavoriteArtistSelfFollow() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new BadRequestException(ErrorMessages.SELF_FOLLOW));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isBadRequest();
    }

    @Test
    void saveFavoriteArtistUserNotFound() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void saveFavoriteArtistNotFound() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new NotFoundException(ErrorMessages.ARTIST_NOT_FOUND));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void saveFavoriteArtistDuplicate() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new ConflictException(ErrorMessages.DUPLICATE_FOLLOW));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isEqualTo(409);
    }

    @Test
    void saveFavoriteArtistAsDifferentUser() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.post()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void deleteFavoriteArtistSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");

        client.delete()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isNoContent();

        verify(service).deleteFavoriteArtist(1L, 1L);
    }

    @Test
    void deleteFavoriteArtistAsDifferentUser() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.delete()
              .uri("/user/1/favorite-artist/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void listFavoriteArtists() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.listFavoriteArtists(any())).thenReturn(List.of(expectedUser));

        List<UserResponse> response = client.get()
                                            .uri("user/1/favorite-artist")
                                            .header("Authorization", "Bearer " + token)
                                            .exchange()
                                            .expectStatus().isOk()
                                            .expectBody(new ParameterizedTypeReference<List<UserResponse>>() {
                                            })
                                            .returnResult()
                                            .getResponseBody();

        assertEquals(List.of(expectedUser), response);
    }

    @Test
    void listFavoriteArtistsAsDifferentUser() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.get()
              .uri("user/1/favorite-artist")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void listFavoriteArtistsEmpty() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.listFavoriteArtists(any())).thenReturn(List.of());

        List<UserResponse> response = client.get()
                                            .uri("user/1/favorite-artist")
                                            .header("Authorization", "Bearer " + token)
                                            .exchange()
                                            .expectStatus().isOk()
                                            .expectBody(new ParameterizedTypeReference<List<UserResponse>>() {
                                            })
                                            .returnResult()
                                            .getResponseBody();

        assertEquals(List.of(), response);
    }
}