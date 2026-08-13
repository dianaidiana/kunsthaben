package io.everyonecodes.project_module.artworkimages;


import io.everyonecodes.project_module.artworks.Artwork;
import io.everyonecodes.project_module.artworks.ArtworkService;
import io.everyonecodes.project_module.artworks.dimensions.Dimensions;
import io.everyonecodes.project_module.artworks.dimensions.Frame;
import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.enums.CategoryEnum;
import io.everyonecodes.project_module.classification.enums.MediaEnum;
import io.everyonecodes.project_module.classification.enums.SupportEnum;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.storage.S3StorageService;
import io.everyonecodes.project_module.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtworkImageServiceTest {

    ArtworkImageService service;

    @Mock
    ArtworkImageRepository repository;

    @Mock
    ArtworkService artworkService;

    @Mock
    S3StorageService s3StorageService;

    @BeforeEach
    void setup() {
        service = new ArtworkImageService(repository, artworkService, s3StorageService);
    }

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");
    private final User user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);

    private final Category category = new Category(1L, CategoryEnum.PAINTING.getName(), CategoryEnum.PAINTING.getCode());
    private final Media media = new Media(1L, MediaEnum.OIL.getName(), MediaEnum.OIL.getCode(), category);
    private final Support support = new Support(1L, SupportEnum.CANVAS.getName(), SupportEnum.CANVAS.getCode(), category);

    private final Artwork artwork = new Artwork(
            1L, user,
            "happy accident", 100.0, 2026, "this is a beautiful painting",
            "Vienna", "1020",
            Dimensions.of(30, 40, null),
            Frame.of(false, null, null, null),
            category, media, support,
            createdAt, null,
            false, false,
            new ArrayList<>()
    );

    private final Artwork anotherArtwork = new Artwork(
            2L, user,
            "sunrise", 200.0, 2025, "a colorful sunrise",
            "Vienna", "1020",
            Dimensions.of(20, 20, null),
            Frame.of(false, null, null, null),
            category, media, support,
            createdAt.plusDays(1), null,
            false, false,
            new ArrayList<>()
    );

    private final MockMultipartFile file = new MockMultipartFile(
            "file",                       // the request-param name (matches @RequestParam("file"))
            "painting.jpg",                     // original filename
            "image/jpeg",                       // content type
            validJpegBytes()                    // actual file content: a real, decodable 1x1 JPEG
    );

    // ImageIO.read(...) in ArtworkImageService only accepts genuinely decodable image bytes,
    // so tests need real (if tiny) image content, not an arbitrary byte array.
    private static byte[] validJpegBytes() {
        try {
            var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            var out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final ArtworkImage artworkImage1 = new ArtworkImage(1L, artwork, "urlurl1.com", 0);
    private final ArtworkImage artworkImage2 = new ArtworkImage(2L, artwork, "urlurl2.com", 1);
    private final ArtworkImage artworkImage3 = new ArtworkImage(3L, artwork, "urlurl3.com", 2);

    @Test
    void addImageToUnexistentArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));
        assertThrows(NotFoundException.class, () -> service.addImage(artistId, artworkId, file));
    }

    @Test
    void addImageToNotOwnedArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));
        assertThrows(ForbiddenException.class, () -> service.addImage(artistId, artworkId, file));
    }

    @Test
    void addImageExceedingMaxBoundary() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(repository.countByArtworkId(artworkId)).thenReturn(ArtworkImageService.MAX_IMAGES_PER_ARTWORK);
        assertThrows(BadRequestException.class, () -> service.addImage(artistId, artworkId, file));
    }

    @Test
    void addImageAtMaxBoundarySucceeds() {
        when(artworkService.fetchOwnedArtwork(user.getId(), artwork.getId())).thenReturn(artwork);
        when(repository.countByArtworkId(artwork.getId())).thenReturn(ArtworkImageService.MAX_IMAGES_PER_ARTWORK - 1);
        when(s3StorageService.uploadFile(file)).thenReturn("https://bucket.s3.region.amazonaws.com/key.jpg");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArtworkImage image = service.addImage(user.getId(), artwork.getId(), file);
        assertEquals(ArtworkImageService.MAX_IMAGES_PER_ARTWORK - 1, image.getSortOrder());
    }

    @Test
    void addImageSuccessfully() {
        artwork.getImages().add(artworkImage1);
        when(artworkService.fetchOwnedArtwork(user.getId(), artworkImage1.getId())).thenReturn(artwork);
        when(repository.countByArtworkId(artwork.getId())).thenReturn(1);
        when(s3StorageService.uploadFile(file)).thenReturn("https://bucket.s3.region.amazonaws.com/key.jpg");
        when(repository.save(any())).thenAnswer(invocation -> {
            ArtworkImage savedImage = invocation.getArgument(0);
            savedImage.setId(2L);
            return savedImage;
        });

        ArtworkImage image = service.addImage(user.getId(), artwork.getId(), file);
        assertEquals(new ArtworkImage(2L, artwork, "https://bucket.s3.region.amazonaws.com/key.jpg", 1), image);
    }

    @Test
    void addImageWithInvalidContentType() {
        when(artworkService.fetchOwnedArtwork(user.getId(), artwork.getId())).thenReturn(artwork);
        when(repository.countByArtworkId(artwork.getId())).thenReturn(0);

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "painting.pdf", "application/pdf", "fake bytes".getBytes()
        );

        assertThrows(BadRequestException.class,
                () -> service.addImage(user.getId(), artwork.getId(), invalidFile));

        verifyNoInteractions(s3StorageService);
    }

    @Test
    void reorderImagesNotOwnedArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));
        assertThrows(ForbiddenException.class, () -> service.reorderImages(artistId, artworkId, List.of(1L)));
    }

    @Test
    void reorderImagesUnexistentArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));
        assertThrows(NotFoundException.class, () -> service.reorderImages(artistId, artworkId, List.of(1L)));
    }

    @Test
    void reorderImagesDifferentSet() {
        when(artworkService.fetchOwnedArtwork(user.getId(), artwork.getId())).thenReturn(artwork);
        artwork.getImages().add(artworkImage1);
        artwork.getImages().add(artworkImage2);

        when(repository.findArtworkImageByArtworkId(artwork.getId())).thenReturn(List.of(artworkImage1, artworkImage2));
        assertThrows(BadRequestException.class, () -> service.reorderImages(user.getId(), artwork.getId(), List.of(artworkImage1.getId(), artworkImage3.getId())));
    }

    @Test
    void reorderImagesLargerSet() {
        when(artworkService.fetchOwnedArtwork(user.getId(), artwork.getId())).thenReturn(artwork);
        artwork.getImages().add(artworkImage1);
        artwork.getImages().add(artworkImage2);

        when(repository.findArtworkImageByArtworkId(artwork.getId())).thenReturn(List.of(artworkImage1, artworkImage2));
        assertThrows(BadRequestException.class, () -> service.reorderImages(user.getId(), artwork.getId(), List.of(artworkImage1.getId(), artworkImage2.getId(), artworkImage3.getId())));
    }

    @Test
    void reorderImagesSuccessfully() {
        when(artworkService.fetchOwnedArtwork(user.getId(), artwork.getId())).thenReturn(artwork);
        artwork.getImages().add(artworkImage1);
        artwork.getImages().add(artworkImage2);

        when(repository.findArtworkImageByArtworkId(artwork.getId())).thenReturn(List.of(artworkImage1, artworkImage2));
        when(repository.getReferenceById(artworkImage1.getId())).thenReturn(artworkImage1);
        when(repository.getReferenceById(artworkImage2.getId())).thenReturn(artworkImage2);

        service.reorderImages(user.getId(), artwork.getId(), List.of(artworkImage2.getId(), artworkImage1.getId()));
        assertEquals(0, artworkImage2.getSortOrder());
        assertEquals(1, artworkImage1.getSortOrder());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteNotOwnedArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER));
        assertThrows(ForbiddenException.class, () -> service.deleteImage(artistId, artworkId, 1L));
    }

    @Test
    void deleteFromUnexistentArtwork() {
        Long artistId = 1L;
        Long artworkId = 1L;
        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));
        assertThrows(NotFoundException.class, () -> service.deleteImage(artistId, artworkId, 1L));
    }

    @Test
    void deleteUnexistentImage() {
        when(artworkService.fetchOwnedArtwork(any(), any())).thenReturn(artwork);
        when(repository.findById(any())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteImage(user.getId(), artwork.getId(), 1L));
    }

    @Test
    void deleteImageFromOtherArtwork() {
        when(artworkService.fetchOwnedArtwork(any(), any())).thenReturn(artwork);
        when(repository.findById(any())).thenReturn(Optional.of(
                new ArtworkImage(2L, anotherArtwork, "url.com", 1)
        ));
        assertThrows(NotFoundException.class, () -> service.deleteImage(user.getId(), artwork.getId(), 2L));
    }

    @Test
    void deleteImageSuccessfully() {
        Long artistId = user.getId();
        Long artworkId = artwork.getId();

        artwork.getImages().add(artworkImage1);
        artwork.getImages().add(artworkImage2);
        artwork.getImages().add(artworkImage3);

        when(artworkService.fetchOwnedArtwork(artistId, artworkId)).thenReturn(artwork);
        when(repository.findById(artworkImage2.getId())).thenReturn(Optional.of(artworkImage2));
        // findArtworkImageByArtworkId(artworkId) will be called twice: first before deletion,
        // where a filter is applied so that one gets the remainingIds
        // and second after deletion for the reordering.
        // Provided the filter in the first case, it is safe to just return the findArtworkImageByArtworkId(artworkId)
        // for the testing purposes
        when(repository.findArtworkImageByArtworkId(artworkId)).thenReturn(List.of(artworkImage1, artworkImage3));
        when(repository.getReferenceById(artworkImage1.getId())).thenReturn(artworkImage1);
        when(repository.getReferenceById(artworkImage3.getId())).thenReturn(artworkImage3);

        service.deleteImage(artistId, artworkId, artworkImage2.getId());

        verify(repository).delete(artworkImage2);
        assertEquals(0, artworkImage1.getSortOrder());
        assertEquals(1, artworkImage3.getSortOrder());
    }

}