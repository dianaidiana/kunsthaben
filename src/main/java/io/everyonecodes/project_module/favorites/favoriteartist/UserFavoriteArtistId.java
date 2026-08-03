package io.everyonecodes.project_module.favorites.favoriteartist;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class UserFavoriteArtistId implements Serializable {
    private Long userId;
    private Long artistId;
}
