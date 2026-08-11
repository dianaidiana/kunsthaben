package io.everyonecodes.project_module.artworkimages;

import io.everyonecodes.project_module.artworkimages.dto.ArtworkImageRequest;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ArtworkImageControllerTest {

    @MockitoBean
    ArtworkImageService service;

    @Autowired
    RestTestClient client;

    private final ArtworkImage expectedImage = new ArtworkImage(2L, null, "urlurl.com", 1);
    private final ArtworkImageRequest validRequest = new ArtworkImageRequest("http://urlurl.com");

    @Test
    void addImageSuccessfully() {
        when(service.addImage(eq(1L), eq(1L), any())).thenReturn(expectedImage);

        ArtworkImage response = client.post()
                                      .uri("/user/1/artwork/1/images")
                                      .contentType(MediaType.APPLICATION_JSON)
                                      .body(validRequest)
                                      .exchange()
                                      .expectStatus().isOk()
                                      .expectBody(ArtworkImage.class)
                                      .returnResult()
                                      .getResponseBody();

        assertEquals(expectedImage, response);
    }

    @Test
    void addImageWithInvalidUrl() {
        client.post()
              .uri("/user/1/artwork/1/images")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new ArtworkImageRequest("not-a-url"))
              .exchange()
              .expectStatus().isBadRequest();
    }

    @Test
    void addImageToUnexistentArtwork() {
        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));

        client.post()
              .uri("/user/1/artwork/1/images")
              .contentType(MediaType.APPLICATION_JSON)
              .body(validRequest)
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void addImageToNotOwnedArtwork() {
        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));

        client.post()
              .uri("/user/1/artwork/1/images")
              .contentType(MediaType.APPLICATION_JSON)
              .body(validRequest)
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void addImageExceedingMaxImagesPerArtwork() {
        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new BadRequestException(ErrorMessages.TOO_MANY_IMAGES));

        client.post()
              .uri("/user/1/artwork/1/images")
              .contentType(MediaType.APPLICATION_JSON)
              .body(validRequest)
              .exchange()
              .expectStatus().isBadRequest();
    }

    @Test
    void reorderImagesSuccessfully() {
        client.put()
              .uri("/user/1/artwork/1/images/reorder")
              .contentType(MediaType.APPLICATION_JSON)
              .body(List.of(2L, 1L))
              .exchange()
              .expectStatus().isOk();

        verify(service).reorderImages(1L, 1L, List.of(2L, 1L));
    }

    @Test
    void reorderImagesNotOwnedArtwork() {
        doThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER)).when(service).reorderImages(eq(1L), eq(1L), any());

        client.put()
              .uri("/user/1/artwork/1/images/reorder")
              .contentType(MediaType.APPLICATION_JSON)
              .body(List.of(1L))
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void reorderImagesUnexistentArtwork() {
        doThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND)).when(service).reorderImages(eq(1L), eq(1L), any());

        client.put()
              .uri("/user/1/artwork/1/images/reorder")
              .contentType(MediaType.APPLICATION_JSON)
              .body(List.of(1L))
              .exchange()
              .expectStatus().isNotFound();
    }

    @Test
    void reorderImagesSetMismatch() {
        doThrow(new BadRequestException(ErrorMessages.IMAGES_DO_NOT_MATCH)).when(service).reorderImages(eq(1L), eq(1L), any());

        client.put()
              .uri("/user/1/artwork/1/images/reorder")
              .contentType(MediaType.APPLICATION_JSON)
              .body(List.of(1L, 3L))
              .exchange()
              .expectStatus().isBadRequest();
    }

    @Test
    void deleteImageSuccessfully() {
        client.delete()
              .uri("/user/1/artwork/1/images/1")
              .exchange()
              .expectStatus().isOk();

        verify(service).deleteImage(1L, 1L, 1L);
    }

    @Test
    void deleteImageFromNotOwnedArtwork() {
        doThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER)).when(service).deleteImage(1L, 1L, 1L);

        client.delete()
              .uri("/user/1/artwork/1/images/1")
              .exchange()
              .expectStatus().isForbidden();
    }

    @Test
    void deleteUnexistentImage() {
        doThrow(new NotFoundException(ErrorMessages.IMAGE_NOT_FOUND)).when(service).deleteImage(1L, 1L, 1L);

        client.delete()
              .uri("/user/1/artwork/1/images/1")
              .exchange()
              .expectStatus().isNotFound();
    }
}