package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworks.dimensions.Dimensions;
import io.everyonecodes.project_module.artworks.dimensions.Frame;
import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.enums.CategoryEnum;
import io.everyonecodes.project_module.classification.enums.MediaEnum;
import io.everyonecodes.project_module.classification.enums.SupportEnum;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtworkOwnershipServiceTest {

    ArtworkOwnershipService service;

    @Mock
    ArtworkRepository repository;

    @BeforeEach
    void setup() {
        service = new ArtworkOwnershipService(repository);
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

    @Test
    void fetchOwnedArtworkSuccessfully() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(artwork));

        assertEquals(artwork, service.fetchOwnedArtwork(user.getId(), 1L));
    }

    @Test
    void fetchOwnedArtworkUnexistentOrDeleted() {
        when(repository.findByIdAndDeletedAtIsNull(eq(1L))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.fetchOwnedArtwork(user.getId(), 1L));
    }

    @Test
    void fetchOwnedArtworkNotOwnedByCaller() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(artwork));

        assertThrows(ForbiddenException.class, () -> service.fetchOwnedArtwork(2L, 1L));
    }
}