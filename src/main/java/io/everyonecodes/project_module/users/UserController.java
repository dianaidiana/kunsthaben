package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.dto.UserRegisterRequest;
import io.everyonecodes.project_module.users.dto.UserResponse;
import io.everyonecodes.project_module.users.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user/register")
    UserResponse register(@Valid @RequestBody UserRegisterRequest userRequest) {
        return userService.register(userRequest);
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

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{id}")
    void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
