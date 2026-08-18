package io.everyonecodes.project_module.auth;

import io.everyonecodes.project_module.auth.dto.AuthResponse;
import io.everyonecodes.project_module.auth.dto.LoginRequest;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.UnauthorizedException;
import io.everyonecodes.project_module.users.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            /*
            this single call is where CustomUserDetailsService and the PasswordEncoder bean get used
            internally, the DaoAuthenticationProvider calls loadUserByUsername(email),
            then checks the raw password against the returned hash. If the email doesn't exist, it throws
            `UsernameNotFoundException`; if the password doesn't match, it throws `BadCredentialsException`.
            */
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new UnauthorizedException(ErrorMessages.INVALID_CREDENTIALS);
        }

        var user = userRepository.findByEmail(request.getEmail())
                                 .orElseThrow(() -> new UnauthorizedException(ErrorMessages.INVALID_CREDENTIALS));
        return new AuthResponse(jwtService.generateToken(user));
    }
}
