package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.auth.AuthService;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserRegisterResponse;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user/register")
    UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
        var user = userService.register(userRequest);
        var token = authService.issueToken(user.getId(), user.getEmail());
        return new UserRegisterResponse(user, token);
    }

    @GetMapping("/user/{id}")
    UserResponse getById(@PathVariable Long id) {
        return userService.getById(id)
                          .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @PutMapping("/user/{id}")
    UserResponse update(@PathVariable Long id,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        @Valid @RequestBody UserUpdateRequest updateRequest) {
        throwForbiddenIfNotOwned(principal, id);
        return userService.update(id, updateRequest);
    }

    @PutMapping(path = "/user/{id}/avatar", consumes = "multipart/form-data")
    UserResponse updateAvatar(@PathVariable Long id,
                              @AuthenticationPrincipal AuthPrincipal principal,
                              @RequestParam("file") MultipartFile file) {
        throwForbiddenIfNotOwned(principal, id);
        return userService.updateAvatar(id, file);
    }

    @PutMapping(path = "/user/{id}/banner", consumes = "multipart/form-data")
    UserResponse updateBanner(@PathVariable Long id,
                              @AuthenticationPrincipal AuthPrincipal principal,
                              @RequestParam("file") MultipartFile file) {
        throwForbiddenIfNotOwned(principal, id);
        return userService.updateBanner(id, file);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}/avatar")
    void deleteAvatar(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        throwForbiddenIfNotOwned(principal, id);
        userService.deleteAvatar(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}/banner")
    void deleteBanner(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        throwForbiddenIfNotOwned(principal, id);
        userService.deleteBanner(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}")
    void delete(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        throwForbiddenIfNotOwned(principal, id);
        userService.delete(id);
    }

    private void throwForbiddenIfNotOwned(AuthPrincipal principal, Long id) {
        if (!principal.id().equals(id)) {
            throw new ForbiddenException(ErrorMessages.NOT_PROFILE_OWNER);
        }
    }
}
