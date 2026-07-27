package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    @DeleteMapping("/user/{id}")
    void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
