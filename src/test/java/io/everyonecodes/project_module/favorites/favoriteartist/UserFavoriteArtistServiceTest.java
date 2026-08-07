package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserRepository;
import io.everyonecodes.project_module.users.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFavoriteArtistServiceTest {

    UserFavoriteArtistService service;

    @Mock
    UserFavoriteArtistRepository repository;

    @Mock
    UserRepository userRepository;

    @BeforeEach
    void setup() {
        service = new UserFavoriteArtistService(repository, userRepository);
    }

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final User user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash",
            null, null, "Vienna", "1020", null, createdAt);

    private final User artist = new User(2L, "Naomi H", "naomi@h.com", "hashhashhash",
            null, null, "Vienna", "1030", null, createdAt);

    @Test
    void saveFavoriteArtistSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));
        when(repository.existsByUserIdAndArtistId(1L, 2L)).thenReturn(false);

        var expectedResponse = new UserResponse(artist.getId(), artist.getName(), artist.getEmail(),
                artist.getBannerUrl(), artist.getAvatarUrl(), artist.getCity(), artist.getPostcode(),
                artist.getAbout(), artist.getCreatedAt());

        assertEquals(expectedResponse, service.saveFavoriteArtist(1L, 2L));
        verify(repository).save(any());
    }

    @Test
    void saveFavoriteArtistSelfFollow() {
        assertThrows(BadRequestException.class, () -> service.saveFavoriteArtist(1L, 1L));
        verifyNoInteractions(userRepository, repository);
    }

    @Test
    void saveFavoriteArtistUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.saveFavoriteArtist(1L, 2L));
    }

    @Test
    void saveFavoriteArtistArtistNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.saveFavoriteArtist(1L, 2L));
    }

    @Test
    void saveFavoriteArtistDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));
        when(repository.existsByUserIdAndArtistId(1L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.saveFavoriteArtist(1L, 2L));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteFavoriteArtistWhenFollowExists() {
        var id = new UserFavoriteArtistId(1L, 2L);
        when(repository.existsById(id)).thenReturn(true);

        service.deleteFavoriteArtist(1L, 2L);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteFavoriteArtistWhenFollowDoesNotExist() {
        var id = new UserFavoriteArtistId(1L, 2L);
        when(repository.existsById(id)).thenReturn(false);

        service.deleteFavoriteArtist(1L, 2L);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void listFavoriteArtistsSuccessfully() {
        var favorite = new UserFavoriteArtist(user, artist);
        when(repository.findByUserId(1L)).thenReturn(List.of(favorite));

        var expectedResponse = new UserResponse(artist.getId(), artist.getName(), artist.getEmail(),
                artist.getBannerUrl(), artist.getAvatarUrl(), artist.getCity(), artist.getPostcode(),
                artist.getAbout(), artist.getCreatedAt());

        assertEquals(List.of(expectedResponse), service.listFavoriteArtists(1L));
    }

    @Test
    void listFavoriteArtistsEmpty() {
        when(repository.findByUserId(1L)).thenReturn(List.of());

        assertEquals(List.of(), service.listFavoriteArtists(1L));
    }

}