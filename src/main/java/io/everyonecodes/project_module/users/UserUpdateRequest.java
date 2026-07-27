package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@AllArgsConstructor
@Getter
@Setter
public class UserUpdateRequest {

//    No password update for now
//    @NotBlank(message = ErrorMessages.PASSWORD_REQUIRED)
//    private String password;

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @URL(message = ErrorMessages.BANNER_URL_INVALID)
    private String bannerUrl;

    @URL(message = ErrorMessages.AVATAR_URL_INVALID)
    private String avatarUrl;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    private String about;
}
