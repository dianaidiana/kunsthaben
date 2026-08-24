package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworks.dto.ArtworkCardResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkCreateRequest;
import io.everyonecodes.project_module.artworks.dto.ArtworkDetailResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkUpdateRequest;
import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.testsupport.AuthTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ArtworkControllerTest {

    @MockitoBean
    ArtworkService service;

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

    private final ArtworkDetailResponse expectedDetail = new ArtworkDetailResponse(
            1L, "happy accident", "this is a beautiful painting", 100.0, 2026,
            "Vienna", "1020",
            30, 40, null,
            false, null, null, null,
            1L, "Bob Ross", null,
            1L, "CAT_PAINTING", "Painting",
            1L, "Oil",
            1L, "Canvas",
            false, false, createdAt,
            List.of()
    );

    private final ArtworkCardResponse expectedCard = new ArtworkCardResponse(
            1L, null, "happy accident", "Bob Ross", 2026,
            "CAT_PAINTING", "Painting", "Oil", "Canvas",
            100.0, false, false, createdAt
    );

    private final ArtworkCreateRequest createRequest = new ArtworkCreateRequest(
            "happy accident", 100.0, 2026, "this is a beautiful painting",
            "Vienna", "1020",
            30, 40, null,
            false, null, null, null,
            1L, 1L, 1L
    );

    private final ArtworkUpdateRequest updateRequest = new ArtworkUpdateRequest(
            "happy accident", 100.0, 2026, "this is a beautiful painting",
            "Vienna", "1020",
            30, 40, null,
            false, null, null, null,
            1L, 1L, 1L
    );

    @Test
    void getDetailByExistentId() {
        when(service.getDetailById(1L)).thenReturn(Optional.of(expectedDetail));

        ArtworkDetailResponse response = client.get()
                                               .uri("/artwork/1")
                                               .exchange()
                                               .expectStatus().isOk()
                                               .expectBody(ArtworkDetailResponse.class)
                                               .returnResult()
                                               .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void getDetailByUnexistentId() {
        when(service.getDetailById(1L)).thenReturn(Optional.empty());

        client.get()
              .uri("/artwork/1")
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void getAllCardsSuccessfully() {
        when(service.getAllCards(any())).thenReturn(new SliceImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/artwork")
              .exchange()
              .expectStatus().isOk();

        verify(service).getAllCards(any());
    }

    @Test
    void searchSuccessfully() {
        when(service.search(any(), any())).thenReturn(new SliceImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/artwork/search?keywords={keywords}", "happy accident")
              .exchange()
              .expectStatus().isOk();

        verify(service).search(any(), any());
    }

    @Test
    void getUnsoldCardsByArtistIdSuccessfully() {
        when(service.getUnsoldCardsByArtistId(eq(1L), any())).thenReturn(new SliceImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/user/1/artwork")
              .exchange()
              .expectStatus().isOk();

        verify(service).getUnsoldCardsByArtistId(eq(1L), any());
    }

    @Test
    void getSoldCardsByArtistIdSuccessfully() {
        when(service.getSoldCardsByArtistId(eq(1L), any())).thenReturn(new SliceImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/user/1/artwork/sold")
              .exchange()
              .expectStatus().isOk();

        verify(service).getSoldCardsByArtistId(eq(1L), any());
    }

    @Test
    void createSuccessfully() {
        when(service.create(eq(1L), any())).thenReturn(expectedDetail);

        ArtworkDetailResponse response = auth.authenticated(
                client.post().uri("/user/1/artwork").contentType(MediaType.APPLICATION_JSON).body(createRequest),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ArtworkDetailResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void createWithUnexistentArtist() {
        when(service.create(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        auth.authenticated(
                client.post().uri("/user/1/artwork").contentType(MediaType.APPLICATION_JSON).body(createRequest),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createAsDifferentArtist() {
        auth.authenticated(
                client.post().uri("/user/1/artwork").contentType(MediaType.APPLICATION_JSON).body(createRequest),
                2L, "someone-else@example.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateSuccessfully() {
        when(service.update(eq(1L), eq(1L), any())).thenReturn(expectedDetail);

        ArtworkDetailResponse response = auth.authenticated(
                client.put().uri("/user/1/artwork/1").contentType(MediaType.APPLICATION_JSON).body(updateRequest),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArtworkDetailResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void updateArtworkNotOwnedByCaller() {
        when(service.update(eq(1L), eq(1L), any())).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));

        auth.authenticated(
                client.put().uri("/user/1/artwork/1").contentType(MediaType.APPLICATION_JSON).body(updateRequest),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateAsDifferentArtist() {
        auth.authenticated(
                client.put().uri("/user/1/artwork/1").contentType(MediaType.APPLICATION_JSON).body(updateRequest),
                2L, "someone-else@example.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteSuccessfully() {
        auth.authenticated(client.delete().uri("/user/1/artwork/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNoContent();

        verify(service).delete(1L, 1L);
    }

    @Test
    void deleteUnexistentArtwork() {
        doThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND)).when(service).delete(1L, 1L);

        auth.authenticated(client.delete().uri("/user/1/artwork/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void deleteAsDifferentArtist() {
        auth.authenticated(client.delete().uri("/user/1/artwork/1"), 2L, "someone-else@example.com")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void markReservedSuccessfully() {
        when(service.markReserved(eq(1L), eq(1L), eq(true))).thenReturn(expectedDetail);

        ArtworkDetailResponse response = auth.authenticated(
                client.patch().uri("/user/1/artwork/1/reserved?reserved={reserved}", true),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArtworkDetailResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void markReservedAsDifferentArtist() {
        auth.authenticated(
                client.patch().uri("/user/1/artwork/1/reserved?reserved={reserved}", true),
                2L, "someone-else@example.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void markSoldSuccessfully() {
        when(service.markSold(eq(1L), eq(1L), eq(true))).thenReturn(expectedDetail);

        ArtworkDetailResponse response = auth.authenticated(
                client.patch().uri("/user/1/artwork/1/sold?sold={sold}", true),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArtworkDetailResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void markSoldAsDifferentArtist() {
        auth.authenticated(
                client.patch().uri("/user/1/artwork/1/sold?sold={sold}", true),
                2L, "someone-else@example.com")
                .exchange()
                .expectStatus().isForbidden();
    }
}