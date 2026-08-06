package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.category.CategoryRepository;
import io.everyonecodes.project_module.classification.enums.CategoryEnum;
import io.everyonecodes.project_module.classification.enums.MediaEnum;
import io.everyonecodes.project_module.classification.enums.SupportEnum;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.media.MediaRepository;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.classification.support.SupportRepository;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {

    ArtworkService service;

    @Mock
    ArtworkRepository repository;

    @Mock
    UserRepository userRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    MediaRepository mediaRepository;

    @Mock
    SupportRepository supportRepository;

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");
    private final User user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);

    private final Category category = new Category(1L, CategoryEnum.PAINTING.getName(), CategoryEnum.PAINTING.getCode());
    private final Media media = new Media(1L, MediaEnum.OIL.getName(), MediaEnum.OIL.getCode(), category);
    private final Support support = new Support(1L, SupportEnum.CANVAS.getName(), SupportEnum.CANVAS.getCode(), category);

    private final Artwork olderArtwork = new Artwork(
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

    private final ArtworkDetailResponse olderArtworkDetailResponse = new ArtworkDetailResponse(
            1L, "happy accident", "this is a beautiful painting", 100.0, 2026,
            "Vienna", "1020",
            30, 40, null,
            false, null, null, null,
            user.getId(), user.getName(), user.getAbout(),
            category.getId(), category.getCode(), category.getName(),
            media.getId(), media.getName(),
            support.getId(), support.getName(),
            false, false, createdAt,
            new ArrayList<>()
    );

    private final ArtworkCardResponse olderArtworkCardResponse = new ArtworkCardResponse(
            1L, null, "happy accident", "Bob Ross", 2026,
            category.getCode(), category.getName(), media.getName(), support.getName(),
            100.0, false, false, createdAt
    );

    private final Artwork newerArtwork = new Artwork(
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

    private final ArtworkCardResponse newerArtworkCardResponse = new ArtworkCardResponse(
            2L, null, "sunrise", "Bob Ross", 2025,
            category.getCode(), category.getName(), media.getName(), support.getName(),
            200.0, false, false, createdAt.plusDays(1)
    );

    private final ArtworkCreateRequest artworkCreateRequest = new ArtworkCreateRequest(
            "happy accident", 100.0, 2026, "this is a beautiful painting",
            "Vienna", "1020",
            30, 40, null,
            false, null, null, null,
            1L, 1L, 1L
    );

    private final ArtworkUpdateRequest artworkUpdateRequest = new ArtworkUpdateRequest(
            "sunset", 150.0, 2023, "a different painting",
            "Vienna", "1030",
            25, 35, null,
            false, null, null, null,
            1L, 1L, 1L
    );

    @BeforeEach
    void setup() {
        service = new ArtworkService(
                repository,
                userRepository,
                categoryRepository,
                mediaRepository,
                supportRepository
        );
    }

    //TODO: test filter search, using @DataJpaTest

    @Test
    void getDetailByExistentId() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        var oResult = service.getDetailById(1L);

        assertTrue(oResult.isPresent());
        assertEquals(olderArtworkDetailResponse, oResult.get());
    }

    @Test
    void getDetailByUnexistentId() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());
        var oResult = service.getDetailById(1L);

        assertTrue(oResult.isEmpty());
    }

    @Test
    void getAllCardsReturnsNonDeletedNewestFirst() {
        when(repository.findAllByDeletedAtIsNullAndSold(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(newerArtwork, olderArtwork)));
        var result = service.getAllCards(PageRequest.of(0, 20));

        assertEquals(List.of(newerArtworkCardResponse, olderArtworkCardResponse), result.getContent());
    }

    @Test
    void getAllCardsWhenNoneExist() {
        when(repository.findAllByDeletedAtIsNullAndSold(eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.getAllCards(PageRequest.of(0, 20));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getUnsoldCardsByArtistIdReturnsNewestFirst() {
        when(repository.findAllByArtistIdAndDeletedAtIsNullAndSold(eq(1L), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(newerArtwork, olderArtwork)));
        var result = service.getUnsoldCardsByArtistId(1L, PageRequest.of(0, 20));

        assertEquals(List.of(newerArtworkCardResponse, olderArtworkCardResponse), result.getContent());
    }

    @Test
    void getUnsoldCardsByArtistIdWhenArtistHasNone() {
        when(repository.findAllByArtistIdAndDeletedAtIsNullAndSold(eq(1L), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.getUnsoldCardsByArtistId(1L, PageRequest.of(0, 20));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getSoldCardsByArtistIdReturnsNewestFirst() {
        olderArtwork.setSold(true);
        newerArtwork.setSold(true);
        olderArtworkCardResponse.setSold(true);
        newerArtworkCardResponse.setSold(true);

        when(repository.findAllByArtistIdAndDeletedAtIsNullAndSold(eq(1L), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(newerArtwork, olderArtwork)));

        var result = service.getSoldCardsByArtistId(1L, PageRequest.of(0, 20));

        assertEquals(List.of(newerArtworkCardResponse, olderArtworkCardResponse), result.getContent());
    }

    @Test
    void getSoldCardsByArtistIdWhenArtistHasNoneSold() {
        when(repository.findAllByArtistIdAndDeletedAtIsNullAndSold(eq(1L), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.getSoldCardsByArtistId(1L, PageRequest.of(0, 20));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void createSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(supportRepository.findById(1L)).thenReturn(Optional.of(support));
        when(repository.save(any())).thenAnswer(invocation -> {
            Artwork savedArtwork = invocation.getArgument(0);
            savedArtwork.setId(1L);
            savedArtwork.setCreatedAt(createdAt);
            return savedArtwork;
        });

        var result = service.create(1L, artworkCreateRequest);

        var artworkCaptor = ArgumentCaptor.forClass(Artwork.class);
        verify(repository).save(artworkCaptor.capture());
        var savedArtwork = artworkCaptor.getValue();

        assertEquals(user, savedArtwork.getArtist());
        assertNull(savedArtwork.getDeletedAt());
        assertFalse(savedArtwork.isSold());
        assertFalse(savedArtwork.isReserved());

        assertEquals(olderArtworkDetailResponse, result);
    }

    @Test
    void createWithUnexistentArtist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(1L, artworkCreateRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void createWithUnexistentCategory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(1L, artworkCreateRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void createWithUnexistentMedium() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(1L, artworkCreateRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void createWithUnexistentSupport() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(supportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(1L, artworkCreateRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void updateExistentArtworkByOwner() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(supportRepository.findById(1L)).thenReturn(Optional.of(support));

        var result = service.update(1L, 1L, artworkUpdateRequest);

        var expected = new ArtworkDetailResponse(
                1L, "sunset", "a different painting", 150.0, 2023,
                "Vienna", "1030",
                25, 35, null,
                false, null, null, null,
                user.getId(), user.getName(), user.getAbout(),
                category.getId(), category.getCode(), category.getName(),
                media.getId(), media.getName(),
                support.getId(), support.getName(),
                false, false, createdAt,
                new ArrayList<>()
        );

        assertEquals(expected, result);
    }

    @Test
    void updateUnexistentOrDeletedArtwork() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, 1L, artworkUpdateRequest));
    }

    @Test
    void updateArtworkNotOwnedByCaller() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        assertThrows(ForbiddenException.class, () -> service.update(2L, 1L, artworkUpdateRequest));
    }

    @Test
    void updateWithUnexistentCategory() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, 1L, artworkUpdateRequest));
    }

    @Test
    void updateWithUnexistentMedium() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, 1L, artworkUpdateRequest));
    }

    @Test
    void updateWithUnexistentSupport() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(supportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, 1L, artworkUpdateRequest));
    }

    @Test
    void deleteExistentArtworkByOwner() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));
        service.delete(1L, 1L);

        assertNotNull(olderArtwork.getDeletedAt());
        verify(repository, never()).delete(any(Artwork.class));
    }

    @Test
    void deleteUnexistentOrDeletedArtwork() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(1L, 1L));
    }

    @Test
    void deleteArtworkNotOwnedByCaller() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        assertThrows(ForbiddenException.class, () -> service.delete(2L, 1L));
    }

    @Test
    void markReservedTrueByOwner() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        var result = service.markReserved(1L, 1L, true);

        assertTrue(result.isReserved());
    }

    @Test
    void markReservedFalseByOwner() {
        olderArtwork.setReserved(true);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        var result = service.markReserved(1L, 1L, false);

        assertFalse(result.isReserved());
    }

    @Test
    void markReservedUnexistentArtwork() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.markReserved(1L, 1L, true));
    }

    @Test
    void markReservedNotOwnedByCaller() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        assertThrows(ForbiddenException.class, () -> service.markReserved(2L, 1L, true));
    }

    @Test
    void markSoldTrueByOwner() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        var result = service.markSold(1L, 1L, true);

        assertTrue(result.isSold());
    }

    @Test
    void markSoldFalseByOwner() {
        olderArtwork.setSold(true);
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        var result = service.markSold(1L, 1L, false);

        assertFalse(result.isSold());
    }

    @Test
    void markSoldUnexistentArtwork() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.markSold(1L, 1L, true));
    }

    @Test
    void markSoldNotOwnedByCaller() {
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(olderArtwork));

        assertThrows(ForbiddenException.class, () -> service.markSold(2L, 1L, true));
    }
}

