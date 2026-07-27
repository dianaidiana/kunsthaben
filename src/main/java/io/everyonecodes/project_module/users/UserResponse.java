package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
public class UserResponse {

    private Long id;

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @Email(message = ErrorMessages.EMAIL_INVALID)
    private String email;

    @URL(message = ErrorMessages.BANNER_URL_INVALID)
    private String bannerUrl;

    @URL(message = ErrorMessages.AVATAR_URL_INVALID)
    private String avatarUrl;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    private String about;

    private OffsetDateTime createdAt;
}
