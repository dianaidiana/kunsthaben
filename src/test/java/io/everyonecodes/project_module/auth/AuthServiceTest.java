package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.auth.dto.LoginRequest;
import io.everyonecodes.project_module.exceptions.UnauthorizedException;
import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    AuthService service;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    UserRepository userRepository;

    @Mock
    JwtService jwtService;

    @BeforeEach
    void setup() {
        service = new AuthService(authenticationManager, userRepository, jwtService);
    }

    @Test
    void loginSuccessfully() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, null, null, null, null);
        var request = new LoginRequest("bob@ross.com", "password123");
        when(userRepository.findByEmail("bob@ross.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(1L, "bob@ross.com")).thenReturn("fake-token");

        var result = service.login(request);

        assertEquals("fake-token", result);
    }

    @Test
    void loginWithWrongPassword() {
        var request = new LoginRequest("bob@ross.com", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> service.login(request));
    }

    @Test
    void loginWithUnknownEmail() {
        var request = new LoginRequest("nobody@nowhere.com", "password123");
        when(authenticationManager.authenticate(any())).thenThrow(new UsernameNotFoundException("nobody@nowhere.com"));

        assertThrows(UnauthorizedException.class, () -> service.login(request));
    }

}