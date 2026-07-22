package io.everyonecodes.project_module;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class UserFavoriteArtwork {

    @EmbeddedId
    private UserFavoriteArtworkId id;

    @ManyToOne
    @MapsId("userId") // links the Object (User) to the ID in the composite key.
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @MapsId("artworkId")
    @JoinColumn(name = "artwork_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Artwork artwork;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public UserFavoriteArtwork(User user, Artwork artwork) {
        this.id = new UserFavoriteArtworkId(user.getId(), artwork.getId());
        this.user = user;
        this.artwork = artwork;
    }
}
