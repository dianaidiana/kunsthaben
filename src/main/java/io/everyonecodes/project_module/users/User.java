package io.everyonecodes.project_module.users;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    @Email(message = ErrorMessages.EMAIL_INVALID)
    private String email;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = ErrorMessages.PASSWORD_HASH_REQUIRED)
    private String passwordHash;

    @Column(columnDefinition = "TEXT")
    @URL(message = ErrorMessages.BANNER_URL_INVALID)
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    @URL(message = ErrorMessages.AVATAR_URL_INVALID)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String city;

    @Column(columnDefinition = "TEXT")
    private String postcode;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
