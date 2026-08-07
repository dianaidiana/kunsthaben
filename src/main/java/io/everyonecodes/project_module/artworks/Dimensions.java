package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Dimensions {

    @Column(name = "dim_width", columnDefinition = "NUMERIC(8,2) CHECK (dim_width > 0)", nullable = false)
    private double width;

    @Column(name = "dim_height", columnDefinition = "NUMERIC(8,2) CHECK (dim_height > 0)", nullable = false)
    private double height;

    @Column(name = "dim_depth", columnDefinition = "NUMERIC(8,2) CHECK (dim_depth > 0)")
    private Double depth;

    public static Dimensions of(double width, double height, Double depth) {
        if (width <= 0 || height <= 0 || (depth != null && depth <= 0)) {
            throw new IllegalArgumentException(ErrorMessages.DIMENSIONS_INVALID);
        }
        return new Dimensions(width, height, depth);
    }
}