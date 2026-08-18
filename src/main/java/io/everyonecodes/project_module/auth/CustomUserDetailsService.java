package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));

        // org.springframework.security.core.userdetails.User is Spring's own
        // ready-made implementation of UserDetail interface, built via a fluent builder
        // I write the whole snippet to avoid collision with this application's User class
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities("ROLE_USER") // there are no roles in this app, but this is mandatory
                .build();
    }
}
