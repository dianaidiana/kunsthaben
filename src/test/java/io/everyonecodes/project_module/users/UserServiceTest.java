package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.artworkimages.ArtworkImageService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.storage.S3StorageService;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    UserService service;

    @Mock
    UserRepository repository;

    @Mock
    S3StorageService s3StorageService;

    @Mock
    ArtworkImageService artworkImageService;

    @BeforeEach
    void setup() {
        service = new UserService(repository, s3StorageService, artworkImageService, encoder);
    }

    private static MockMultipartFile validImageFile() {
        return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", validJpegBytes());
    }

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

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void registerSuccessfully() {

        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123");
        when(repository.findByEmail("bob@ross.com")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            savedUser.setCreatedAt(createdAt);
            return savedUser;
        });

        var result = service.register(request);

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());
        var savedUser = userCaptor.getValue();

        assertNotEquals("password123", savedUser.getPasswordHash());
        assertTrue(encoder.matches("password123", savedUser.getPasswordHash()));

        var expectedUserResponse = new UserResponse(1L, "Bob Ross", "bob@ross.com", null, null, null, null, null, createdAt);
        assertEquals(expectedUserResponse, result);
    }

    @Test
    void registerWithTakenEmail() {
        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123");
        var expectedUser = new User(1L, "Bob Ross", "bob@ross.com", encoder.encode(request.getPassword()), null, null, "Vienna", "1020", null, createdAt);
        when(repository.findByEmail("bob@ross.com")).thenReturn(Optional.of(expectedUser));

        assertThrows(ConflictException.class, () -> service.register(request));
    }

    @Test
    void updateExistentUser() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        var userUpdateRequest = new UserUpdateRequest("Bob Ross", "Vienna", "1050", "Hi! I'm Bob!");
        var expectedResponse = new UserResponse(user.getId(), userUpdateRequest.getName(), user.getEmail(), user.getBannerUrl(), user.getAvatarUrl(), userUpdateRequest.getCity(), userUpdateRequest.getPostcode(), userUpdateRequest.getAbout(), user.getCreatedAt());
        assertEquals(expectedResponse, service.update(1L, userUpdateRequest));
    }

    @Test
    void updateUnexistentUser() {
        var userUpdateRequest = new UserUpdateRequest("Bob Ross", "Vienna", "1050", "Hi! I'm Bob!");
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.update(1L, userUpdateRequest));
    }

    @Test
    void updateAvatarFirstUpload() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(s3StorageService.uploadFile(any())).thenReturn("https://bucket.s3.region.amazonaws.com/new-avatar.jpg");

        var response = service.updateAvatar(1L, validImageFile());

        assertEquals("https://bucket.s3.region.amazonaws.com/new-avatar.jpg", response.getAvatarUrl());
        verify(s3StorageService, never()).deleteFile(any());
    }

    @Test
    void updateAvatarReplacesExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null,
                "https://bucket.s3.region.amazonaws.com/old-avatar.jpg", "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(s3StorageService.uploadFile(any())).thenReturn("https://bucket.s3.region.amazonaws.com/new-avatar.jpg");

        var response = service.updateAvatar(1L, validImageFile());

        assertEquals("https://bucket.s3.region.amazonaws.com/new-avatar.jpg", response.getAvatarUrl());
        verify(s3StorageService).deleteFile("https://bucket.s3.region.amazonaws.com/old-avatar.jpg");
    }

    @Test
    void updateAvatarWithInvalidFile() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        var invalidFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake bytes".getBytes());

        assertThrows(BadRequestException.class, () -> service.updateAvatar(1L, invalidFile));
        verify(s3StorageService, never()).uploadFile(any());
    }

    @Test
    void updateAvatarUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateAvatar(1L, validImageFile()));
    }

    @Test
    void updateBannerFirstUpload() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(s3StorageService.uploadFile(any())).thenReturn("https://bucket.s3.region.amazonaws.com/new-banner.jpg");

        var response = service.updateBanner(1L, validImageFile());

        assertEquals("https://bucket.s3.region.amazonaws.com/new-banner.jpg", response.getBannerUrl());
        verify(s3StorageService, never()).deleteFile(any());
    }

    @Test
    void updateBannerReplacesExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash",
                "https://bucket.s3.region.amazonaws.com/old-banner.jpg", null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(s3StorageService.uploadFile(any())).thenReturn("https://bucket.s3.region.amazonaws.com/new-banner.jpg");

        var response = service.updateBanner(1L, validImageFile());

        assertEquals("https://bucket.s3.region.amazonaws.com/new-banner.jpg", response.getBannerUrl());
        verify(s3StorageService).deleteFile("https://bucket.s3.region.amazonaws.com/old-banner.jpg");
    }

    @Test
    void updateBannerWithInvalidFile() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        var invalidFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake bytes".getBytes());

        assertThrows(BadRequestException.class, () -> service.updateBanner(1L, invalidFile));
        verify(s3StorageService, never()).uploadFile(any());
    }

    @Test
    void updateBannerUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.updateBanner(1L, validImageFile()));
    }

    @Test
    void deleteAvatarWithExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null,
                "https://bucket.s3.region.amazonaws.com/avatar.jpg", "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        service.deleteAvatar(1L);

        verify(s3StorageService).deleteFile("https://bucket.s3.region.amazonaws.com/avatar.jpg");
        assertNull(user.getAvatarUrl());
    }

    @Test
    void deleteAvatarWithoutExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        service.deleteAvatar(1L);

        verify(s3StorageService, never()).deleteFile(any());
        assertNull(user.getAvatarUrl());
    }

    @Test
    void deleteAvatarUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteAvatar(1L));
    }

    @Test
    void deleteBannerWithExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash",
                "https://bucket.s3.region.amazonaws.com/banner.jpg", null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        service.deleteBanner(1L);

        verify(s3StorageService).deleteFile("https://bucket.s3.region.amazonaws.com/banner.jpg");
        assertNull(user.getBannerUrl());
    }

    @Test
    void deleteBannerWithoutExisting() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        service.deleteBanner(1L);

        verify(s3StorageService, never()).deleteFile(any());
        assertNull(user.getBannerUrl());
    }

    @Test
    void deleteBannerUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteBanner(1L));
    }

    @Test
    void getByExistentId() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        var expectedResponse = new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getBannerUrl(), user.getAvatarUrl(), user.getCity(), user.getPostcode(),
                user.getAbout(), user.getCreatedAt());

        assertEquals(Optional.of(expectedResponse), service.getById(1L));
    }

    @Test
    void getByUnexistentId() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.getById(1L));
    }

    @Test
    void deleteExistentUser() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));

        service.delete(1L);

        verify(artworkImageService).deleteAllS3ImagesForArtist(1L);
        verify(repository).delete(user);
    }

    @Test
    void deleteUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(1L));
        verify(repository, never()).delete(any());
    }

}