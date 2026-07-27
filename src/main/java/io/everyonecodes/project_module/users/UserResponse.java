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

    private String name;

    private String email;

    private String bannerUrl;

    private String avatarUrl;

    private String city;

    private String postcode;

    private String about;

    private OffsetDateTime createdAt;
}
