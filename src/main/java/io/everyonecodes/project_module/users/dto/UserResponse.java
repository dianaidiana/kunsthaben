package io.everyonecodes.project_module.users.dto;

import io.everyonecodes.project_module.users.User;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
