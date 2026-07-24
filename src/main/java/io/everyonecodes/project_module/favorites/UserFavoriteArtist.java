package io.everyonecodes.project_module.favorites;

import io.everyonecodes.project_module.users.User;
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
@Table(check = @CheckConstraint(constraint = "user_id <> artist_id"))
public class UserFavoriteArtist {

    @EmbeddedId
    private UserFavoriteArtistId id;

    @ManyToOne
    @MapsId("userId") // links the Object (User) to the ID in the composite key.
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @MapsId("artistId")
    @JoinColumn(name = "artist_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User artist;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public UserFavoriteArtist(User user, User artist) {
        this.id = new UserFavoriteArtistId(user.getId(), artist.getId());
        this.user = user;
        this.artist = artist;
    }
}
