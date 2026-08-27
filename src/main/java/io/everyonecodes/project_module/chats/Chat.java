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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"artwork_id", "buyer_id"}),
        indexes = @Index(name = "idx_chat_buyer_id", columnList = "buyer_id")
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

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private boolean active;

    @OneToMany(mappedBy = "chat")
    @OrderBy("createdAt DESC")
    private List<Message> messages = new ArrayList<>();
}

// TODO: user can delete messages?