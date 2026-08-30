package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.auth.AuthCookieService;
import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.auth.AuthService;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public UserController(UserService userService, AuthService authService, AuthCookieService authCookieService) {
        this.userService = userService;
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user/register")
    UserResponse register(@Valid @RequestBody UserRegisterRequest userRequest, HttpServletResponse response) {
        var user = userService.register(userRequest);
        var token = authService.issueToken(user.getId(), user.getEmail());
        authCookieService.attachAuthCookie(response, token);
        return user;
    }

    @GetMapping("/user/{id}")
    UserResponse getById(@PathVariable Long id) {
        return userService.getById(id)
                          .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @PutMapping("/user")
    UserResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                        @Valid @RequestBody UserUpdateRequest updateRequest) {
        return userService.update(principal.id(), updateRequest);
    }

    @PutMapping(path = "/user/avatar", consumes = "multipart/form-data")
    UserResponse updateAvatar(@AuthenticationPrincipal AuthPrincipal principal,
                              @RequestParam("file") MultipartFile file) {
        return userService.updateAvatar(principal.id(), file);
    }

    @PutMapping(path = "/user/banner", consumes = "multipart/form-data")
    UserResponse updateBanner(@AuthenticationPrincipal AuthPrincipal principal,
                              @RequestParam("file") MultipartFile file) {
        return userService.updateBanner(principal.id(), file);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/avatar")
    void deleteAvatar(@AuthenticationPrincipal AuthPrincipal principal) {
        userService.deleteAvatar(principal.id());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/banner")
    void deleteBanner(@AuthenticationPrincipal AuthPrincipal principal) {
        userService.deleteBanner(principal.id());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user")
    void delete(@AuthenticationPrincipal AuthPrincipal principal) {
        userService.delete(principal.id());
    }
}
