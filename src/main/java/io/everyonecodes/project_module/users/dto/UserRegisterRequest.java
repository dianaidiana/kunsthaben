package io.everyonecodes.project_module.users.dto;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class UserRegisterRequest {

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private final String name;

    @Email(message = ErrorMessages.EMAIL_INVALID)
    private final String email;

    @NotBlank(message = ErrorMessages.PASSWORD_REQUIRED)
    private String password;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;
}
