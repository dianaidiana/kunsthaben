package io.everyonecodes.project_module.chats;

import io.everyonecodes.project_module.artworks.Artwork;
import io.everyonecodes.project_module.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Entity
@Table(
        check = @CheckConstraint(constraint = "buyer_id <> seller_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"artwork_id", "buyer_id"})
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "artwork_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Artwork artwork;

    @ManyToOne
    @JoinColumn(name = "buyer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User seller;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private boolean active;
}