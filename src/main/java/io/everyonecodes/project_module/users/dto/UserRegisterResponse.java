package io.everyonecodes.project_module.users.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class UserRegisterResponse {

    private UserResponse user;

    private String token;
}