package io.everyonecodes.project_module.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
        Spring Security lazily assembles a default AuthenticationManager the first time one is needed,
        using whatever UserDetailsService bean and PasswordEncoder bean it finds in
        your application context, and wiring them together into a DaoAuthenticationProvider automatically.
        AuthenticationConfiguration is the Spring Security object that does this assembly;
        calling .getAuthenticationManager() on it and exposing the result as your own @Bean
        is just what makes that manager available to be injected elsewhere
    */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }
}