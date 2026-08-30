package io.everyonecodes.project_module.artworkimages;


import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;
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

    @Autowired
    JwtService jwtService;

    @Value("${app.auth.cookie-name}")
    String cookieName;

    AuthTestSupport auth;

    @BeforeEach
    void setUpAuth() {
        auth = new AuthTestSupport(client, jwtService, cookieName);
    }

    private final MultipartBodyBuilder builder = new MultipartBodyBuilder();

    private final ArtworkImage expectedImage = new ArtworkImage(2L, null, "https://bucket.s3.region.amazonaws.com/key.jpg", 1);

    private final MockMultipartFile file = new MockMultipartFile(
            "file",                       // the request-param name (matches @RequestParam("file"))
            "painting.jpg",                     // original filename
            "image/jpeg",                       // content type
            "fake image bytes".getBytes()       // actual file content, as bytes
    );

    @Test
    void addImageSuccessfully() throws IOException {
        builder.part("file", new ByteArrayResource(file.getBytes()) {
                   @Override
                   public String getFilename() {
                       return file.getOriginalFilename();
                   }
               })
               .contentType(MediaType.IMAGE_JPEG);

        when(service.addImage(eq(1L), eq(1L), any())).thenReturn(expectedImage);

        ArtworkImage response = auth.authenticated(
                client.post().uri("/artwork/1/images").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ArtworkImage.class)
                .returnResult()
                .getResponseBody();

        assertEquals(expectedImage, response);
    }

    @Test
    void addImageWithInvalidFile() throws IOException {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",                       // the request-param name (matches @RequestParam("file"))
                "painting.pdf",                     // original filename
                "application/pdf",                       // content type
                "fake bytes".getBytes()       // actual file content, as bytes
        );

        builder.part("file", new ByteArrayResource(invalidFile.getBytes()) {
            @Override
            public String getFilename() {
                return invalidFile.getOriginalFilename();
            }
        });

        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new BadRequestException(ErrorMessages.INVALID_IMAGE_FILE));

        auth.authenticated(
                client.post().uri("/artwork/1/images").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void addImageToUnexistentArtwork() throws IOException {
        builder.part("file", new ByteArrayResource(file.getBytes()) {
                   @Override
                   public String getFilename() {
                       return file.getOriginalFilename();
                   }
               })
               .contentType(MediaType.IMAGE_JPEG);

        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));

        auth.authenticated(
                client.post().uri("/artwork/1/images").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void addImageToNotOwnedArtwork() throws IOException {
        builder.part("file", new ByteArrayResource(file.getBytes()) {
                   @Override
                   public String getFilename() {
                       return file.getOriginalFilename();
                   }
               })
               .contentType(MediaType.IMAGE_JPEG);

        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));

        auth.authenticated(
                client.post().uri("/artwork/1/images").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void addImageExceedingMaxImagesPerArtwork() throws IOException {
        builder.part("file", new ByteArrayResource(file.getBytes()) {
                   @Override
                   public String getFilename() {
                       return file.getOriginalFilename();
                   }
               })
               .contentType(MediaType.IMAGE_JPEG);

        when(service.addImage(eq(1L), eq(1L), any())).thenThrow(new BadRequestException(ErrorMessages.TOO_MANY_IMAGES));

        auth.authenticated(
                client.post().uri("/artwork/1/images").contentType(MediaType.MULTIPART_FORM_DATA).body(builder.build()),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void reorderImagesSuccessfully() {
        auth.authenticated(
                client.put().uri("/artwork/1/images/reorder").contentType(MediaType.APPLICATION_JSON).body(List.of(2L, 1L)),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNoContent();

        verify(service).reorderImages(1L, 1L, List.of(2L, 1L));
    }

    @Test
    void reorderImagesNotOwnedArtwork() {
        doThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER)).when(service).reorderImages(eq(1L), eq(1L), any());

        auth.authenticated(
                client.put().uri("/artwork/1/images/reorder").contentType(MediaType.APPLICATION_JSON).body(List.of(1L)),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void reorderImagesUnexistentArtwork() {
        doThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND)).when(service).reorderImages(eq(1L), eq(1L), any());

        auth.authenticated(
                client.put().uri("/artwork/1/images/reorder").contentType(MediaType.APPLICATION_JSON).body(List.of(1L)),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void reorderImagesSetMismatch() {
        doThrow(new BadRequestException(ErrorMessages.IMAGES_DO_NOT_MATCH)).when(service).reorderImages(eq(1L), eq(1L), any());

        auth.authenticated(
                client.put().uri("/artwork/1/images/reorder").contentType(MediaType.APPLICATION_JSON).body(List.of(1L, 3L)),
                1L, "bob@ross.com")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteImageSuccessfully() {
        auth.authenticated(client.delete().uri("/artwork/1/images/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNoContent();

        verify(service).deleteImage(1L, 1L, 1L);
    }

    @Test
    void deleteImageFromNotOwnedArtwork() {
        doThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER)).when(service).deleteImage(1L, 1L, 1L);

        auth.authenticated(client.delete().uri("/artwork/1/images/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void deleteUnexistentImage() {
        doThrow(new NotFoundException(ErrorMessages.IMAGE_NOT_FOUND)).when(service).deleteImage(1L, 1L, 1L);

        auth.authenticated(client.delete().uri("/artwork/1/images/1"), 1L, "bob@ross.com")
            .exchange()
            .expectStatus().isNotFound();
    }
}