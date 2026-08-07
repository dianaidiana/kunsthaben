package io.everyonecodes.project_module.artworks.dto;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ArtworkCreateRequest {
    @NotBlank(message = ErrorMessages.TITLE_REQUIRED)
    private String title;

    @Positive(message = ErrorMessages.PRICE_INVALID)
    private double price;

    @Positive(message = ErrorMessages.YEAR_INVALID)
    private int year;

    @NotBlank(message = ErrorMessages.DESCRIPTION_REQUIRED)
    private String description;

    @NotBlank(message = ErrorMessages.CITY_REQUIRED)
    private String city;

    @NotBlank(message = ErrorMessages.POSTCODE_REQUIRED)
    private String postcode;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private double width;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private double height;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private Double depth;

    private boolean framed;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private Double frameWidth;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private Double frameHeight;

    @Positive(message = ErrorMessages.DIMENSIONS_INVALID)
    private Double frameDepth;

    @NotNull(message = ErrorMessages.CATEGORY_REQUIRED)
    private Long categoryId;

    private Long mediumId;
    private Long supportId;
}
