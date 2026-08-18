package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    CustomUserDetailsService service;

    @Mock
    UserRepository userRepository;

    @BeforeEach
    void setup() {
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsernameWithExistentEmail() {
        var user = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, null);
        when(userRepository.findByEmail("bob@ross.com")).thenReturn(Optional.of(user));

        var userDetails = service.loadUserByUsername("bob@ross.com");

        assertEquals("bob@ross.com", userDetails.getUsername());
        assertEquals("hashhashhash", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameWithUnexistentEmail() {
        when(userRepository.findByEmail("nobody@nowhere.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody@nowhere.com"));
    }

}