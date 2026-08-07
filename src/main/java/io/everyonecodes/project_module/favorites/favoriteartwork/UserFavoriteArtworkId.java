package io.everyonecodes.project_module.favorites.favoriteartwork;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class UserFavoriteArtworkId implements Serializable {
    private Long userId;
    private Long artworkId;
}
