package io.everyonecodes.project_module.artworks.dimensions;

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
public class Frame {

    @Column(nullable = false)
    private boolean framed;

    @Column(name = "dim_frame_width", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_width > 0)")
    private Double width;

    @Column(name = "dim_frame_height", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_height > 0)")
    private Double height;

    @Column(name = "dim_frame_depth", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_depth > 0)")
    private Double depth;

    public static Frame of(boolean framed, Double width, Double height, Double depth) {
        if (!framed) {
            return new Frame(false, null, null, null);
        }
        if (width == null || height == null || width <= 0 || height <= 0 || (depth != null && depth <= 0)) {
            throw new IllegalArgumentException(ErrorMessages.FRAME_DIMENSIONS_INVALID);
        }
        return new Frame(true, width, height, depth);
    }
}
