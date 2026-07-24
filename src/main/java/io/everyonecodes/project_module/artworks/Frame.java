package io.everyonecodes.project_module.artworks;

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

    @Column(name = "dim_frame_x", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_x > 0)")
    private Double dimX;

    @Column(name = "dim_frame_y", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_y > 0)")
    private Double dimY;

    @Column(name = "dim_frame_z", columnDefinition = "NUMERIC(8,2) CHECK (dim_frame_z > 0)")
    private Double dimZ;

    // factories for framed or unframed
    public static Frame framed(double dimX, double dimY, Double dimZ) {
        if (dimX <= 0 || dimY <= 0 || (dimZ != null && dimZ <= 0)) {
            throw new IllegalArgumentException("Frame dimensions must be positive");
        }
        return new Frame(true, dimX, dimY, dimZ);
    }

    public static Frame unframed() {
        return new Frame(false, null, null, null);
    }
}
