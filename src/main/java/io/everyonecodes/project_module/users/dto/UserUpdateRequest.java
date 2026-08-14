package io.everyonecodes.project_module.users.dto;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class UserUpdateRequest {

//    No password update for now
//    @NotBlank(message = ErrorMessages.PASSWORD_REQUIRED)
//    private String password;

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    private String about;
}
