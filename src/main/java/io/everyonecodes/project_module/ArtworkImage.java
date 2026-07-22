package io.everyonecodes.project_module;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        columnNames = {"artwork_id", "sort_order"}
)

)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Artwork artwork;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
