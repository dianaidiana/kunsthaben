package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworks.dto.ArtworkCardResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkCreateRequest;
import io.everyonecodes.project_module.artworks.dto.ArtworkDetailResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkUpdateRequest;
import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
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
        when(service.getAllCards(any())).thenReturn(new PageImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/artwork")
              .exchange()
              .expectStatus().isOk();

        verify(service).getAllCards(any());
    }

    @Test
    void searchSuccessfully() {
        when(service.search(any(), any())).thenReturn(new PageImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/artwork/search?keywords={keywords}", "happy accident")
              .exchange()
              .expectStatus().isOk();

        verify(service).search(any(), any());
    }

    @Test
    void getUnsoldCardsByArtistIdSuccessfully() {
        when(service.getUnsoldCardsByArtistId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/user/1/artwork")
              .exchange()
              .expectStatus().isOk();

        verify(service).getUnsoldCardsByArtistId(eq(1L), any());
    }

    @Test
    void getSoldCardsByArtistIdSuccessfully() {
        when(service.getSoldCardsByArtistId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(expectedCard)));

        client.get()
              .uri("/user/1/artwork/sold")
              .exchange()
              .expectStatus().isOk();

        verify(service).getSoldCardsByArtistId(eq(1L), any());
    }

    @Test
    void createSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.create(eq(1L), any())).thenReturn(expectedDetail);

        ArtworkDetailResponse response = client.post()
                                               .uri("/user/1/artwork")
                                               .header("Authorization", "Bearer " + token)
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .body(createRequest)
                                               .exchange()
                                               .expectStatus().isCreated()
                                               .expectBody(ArtworkDetailResponse.class)
                                               .returnResult()
                                               .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void createWithUnexistentArtist() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.create(eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        client.post()
              .uri("/user/1/artwork")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(createRequest)
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void createAsDifferentArtist() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.post()
              .uri("/user/1/artwork")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(createRequest)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void updateSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.update(eq(1L), eq(1L), any())).thenReturn(expectedDetail);

        ArtworkDetailResponse response = client.put()
                                               .uri("/user/1/artwork/1")
                                               .header("Authorization", "Bearer " + token)
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .body(updateRequest)
                                               .exchange()
                                               .expectStatus().isOk()
                                               .expectBody(ArtworkDetailResponse.class)
                                               .returnResult()
                                               .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void updateArtworkNotOwnedByCaller() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.update(eq(1L), eq(1L), any())).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));

        client.put()
              .uri("/user/1/artwork/1")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(updateRequest)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void updateAsDifferentArtist() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.put()
              .uri("/user/1/artwork/1")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(updateRequest)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void deleteSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");

        client.delete()
              .uri("/user/1/artwork/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isNoContent();

        verify(service).delete(1L, 1L);
    }

    @Test
    void deleteUnexistentArtwork() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        doThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND)).when(service).delete(1L, 1L);

        client.delete()
              .uri("/user/1/artwork/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void deleteAsDifferentArtist() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.delete()
              .uri("/user/1/artwork/1")
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void markReservedSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.markReserved(eq(1L), eq(1L), eq(true))).thenReturn(expectedDetail);

        ArtworkDetailResponse response = client.patch()
                                               .uri("/user/1/artwork/1/reserved?reserved={reserved}", true)
                                               .header("Authorization", "Bearer " + token)
                                               .exchange()
                                               .expectStatus().isOk()
                                               .expectBody(ArtworkDetailResponse.class)
                                               .returnResult()
                                               .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void markReservedAsDifferentArtist() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.patch()
              .uri("/user/1/artwork/1/reserved?reserved={reserved}", true)
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void markSoldSuccessfully() {
        var token = jwtService.generateToken(1L, "bob@ross.com");
        when(service.markSold(eq(1L), eq(1L), eq(true))).thenReturn(expectedDetail);

        ArtworkDetailResponse response = client.patch()
                                               .uri("/user/1/artwork/1/sold?sold={sold}", true)
                                               .header("Authorization", "Bearer " + token)
                                               .exchange()
                                               .expectStatus().isOk()
                                               .expectBody(ArtworkDetailResponse.class)
                                               .returnResult()
                                               .getResponseBody();

        assertEquals(expectedDetail, response);
    }

    @Test
    void markSoldAsDifferentArtist() {
        var token = jwtService.generateToken(2L, "someone-else@example.com");

        client.patch()
              .uri("/user/1/artwork/1/sold?sold={sold}", true)
              .header("Authorization", "Bearer " + token)
              .exchange()
              .expectStatus().isForbidden();
    }
}