package io.everyonecodes.project_module.favorites.favoriteartist;

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

    @Autowired
    UserFavoriteArtistController controller;

    @MockitoBean
    UserFavoriteArtistService service;

    @Autowired
    RestTestClient client;

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final UserResponse expectedUser = new UserResponse(1L, "Bob Ross", "bob@ross.com",
            null, null, "Vienna", "1020", null, createdAt);

    @Test
    void saveFavoriteArtistSuccessfully() {
        when(service.saveFavoriteArtist(any(), any())).thenReturn(expectedUser);

        UserResponse response = client.post()
                                      .uri("/user/1/favorite-artist/1")
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

        client.post()
              .uri("/user/1/favorite-artist/1")
              .exchange()
              .expectStatus().isBadRequest();
    }

    @Test
    void saveFavoriteArtistUserNotFound() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void saveFavoriteArtistNotFound() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new NotFoundException(ErrorMessages.ARTIST_NOT_FOUND));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void saveFavoriteArtistDuplicate() {
        when(service.saveFavoriteArtist(any(), any())).thenThrow(new ConflictException(ErrorMessages.DUPLICATE_FOLLOW));

        client.post()
              .uri("/user/1/favorite-artist/1")
              .exchange()
              .expectStatus().isEqualTo(409);
    }

    @Test
    void deleteFavoriteArtistSuccessfully() {
        client.delete()
              .uri("/user/1/favorite-artist/1")
              .exchange()
              .expectStatus().isOk();

        verify(service).deleteFavoriteArtist(1L, 1L);
    }

    @Test
    void listFavoriteArtists() {
        when(service.listFavoriteArtists(any())).thenReturn(List.of(expectedUser));

        List<UserResponse> response = client.get()
                                            .uri("user/1/favorite-artist")
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

        List<UserResponse> response = client.get()
                                            .uri("user/1/favorite-artist")
                                            .exchange()
                                            .expectStatus().isOk()
                                            .expectBody(new ParameterizedTypeReference<List<UserResponse>>() {
                                            })
                                            .returnResult()
                                            .getResponseBody();

        assertEquals(List.of(), response);
    }
}