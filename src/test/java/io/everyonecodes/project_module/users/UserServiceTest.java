package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

    @BeforeEach
    void setup() {
        service = new UserService(repository);
    }

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void registerSuccessfully() {

        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123", "Vienna", "1020");
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

        var expectedUserResponse = new UserResponse(1L, "Bob Ross", "bob@ross.com", null, null, "Vienna", "1020", null, createdAt);
        assertEquals(expectedUserResponse, result);
    }

    @Test
    void registerWithTakenEmail() {
        var request = new UserRegisterRequest("Bob Ross", "bob@ross.com", "password123", "Vienna", "1020");
        var expectedUser = new User(1L, "Bob Ross", "bob@ross.com", encoder.encode(request.getPassword()), null, null, "Vienna", "1020", null, createdAt);
        when(repository.findByEmail("bob@ross.com")).thenReturn(Optional.of(expectedUser));

        assertThrows(ConflictException.class, () -> service.register(request));
    }

    @Test
    void updateExistentUser() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
        when(repository.findById(eq(1L))).thenReturn(Optional.of(user));
        var userUpdateRequest = new UserUpdateRequest("Bob Ross", null, null, "Vienna", "1050", "Hi! I'm Bob!");
        var expectedResponse = new UserResponse(user.getId(), userUpdateRequest.getName(), user.getEmail(), userUpdateRequest.getBannerUrl(), userUpdateRequest.getAvatarUrl(), userUpdateRequest.getCity(), userUpdateRequest.getPostcode(), userUpdateRequest.getAbout(), user.getCreatedAt());
        assertEquals(expectedResponse, service.update(1L, userUpdateRequest));
    }

    @Test
    void updateUnexistentUser() {
        var userUpdateRequest = new UserUpdateRequest("Bob Ross", null, null, "Vienna", "1050", "Hi! I'm Bob!");
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.update(1L, userUpdateRequest));
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

        verify(repository).delete(user);
    }

    @Test
    void deleteUnexistentUser() {
        when(repository.findById(eq(1L))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(1L));
        verify(repository, never()).delete(any());
    }

}