package io.everyonecodes.project_module.artworkimages.dto;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ArtworkImageRequest {

    @NotBlank(message = ErrorMessages.URL_REQUIRED)
    @URL(message = ErrorMessages.IMAGE_URL_INVALID)
    private String url;

}
