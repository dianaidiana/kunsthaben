package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.auth.JwtService;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserRegisterResponse;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user/register")
    UserRegisterResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
        var user = userService.register(userRequest);
        var token = jwtService.generateToken(user.getId(), user.getEmail());
        return new UserRegisterResponse(user, token);
    }

    @GetMapping("/user/{id}")
    UserResponse getById(@PathVariable Long id) {
        return userService.getById(id)
                          .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
    }

    @PutMapping("/user/{id}")
    UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest updateRequest) {
        return userService.update(id, updateRequest);
    }

    @PutMapping(path = "/user/{id}/avatar", consumes = "multipart/form-data")
    UserResponse updateAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return userService.updateAvatar(id, file);
    }

    @PutMapping(path = "/user/{id}/banner", consumes = "multipart/form-data")
    UserResponse updateBanner(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return userService.updateBanner(id, file);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}/avatar")
    void deleteAvatar(@PathVariable Long id) {
        userService.deleteAvatar(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}/banner")
    void deleteBanner(@PathVariable Long id) {
        userService.deleteBanner(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}")
    void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
