package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public UserResponse register(UserRegisterRequest userRequest) {
        var email = userRequest.getEmail();
        var oExistingUser = repository.findByEmail(email);
        if (oExistingUser.isPresent()) {
            throw new ConflictException(ErrorMessages.EMAIL_ALREADY_TAKEN);
        }

        var password = userRequest.getPassword();
        var passwordHash = encoder.encode(password);
        var newUser = new User(null, userRequest.getName(), email, passwordHash, null, null, userRequest.getCity(), userRequest.getPostcode(), null, null);
        var savedUser = repository.save(newUser);
        return new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(),
                savedUser.getBannerUrl(), savedUser.getAvatarUrl(), savedUser.getCity(),
                savedUser.getPostcode(), savedUser.getAbout(), savedUser.getCreatedAt());
    }

    public Optional<UserResponse> getById(Long id) {
        return repository.findById(id)
                         .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(),
                                 user.getBannerUrl(), user.getAvatarUrl(), user.getCity(),
                                 user.getPostcode(), user.getAbout(), user.getCreatedAt()));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        var user = repository.findById(id)
                             .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));

        user.setName(request.getName());
        user.setCity(request.getCity());
        user.setPostcode(request.getPostcode());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBannerUrl(request.getBannerUrl());
        user.setAbout(request.getAbout());

        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getBannerUrl(), user.getAvatarUrl(), user.getCity(),
                user.getPostcode(), user.getAbout(), user.getCreatedAt());
    }

    public void delete(Long id) {
        var user = repository.findById(id)
                             .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
        repository.delete(user);
    }

}
