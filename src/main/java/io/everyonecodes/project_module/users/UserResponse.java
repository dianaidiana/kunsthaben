package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
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

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                user.getBannerUrl(), user.getAvatarUrl(), user.getCity(),
                user.getPostcode(), user.getAbout(), user.getCreatedAt());
    }
}
