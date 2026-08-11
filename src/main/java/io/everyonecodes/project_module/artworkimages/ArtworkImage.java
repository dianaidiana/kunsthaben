package io.everyonecodes.project_module.artworkimages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.everyonecodes.project_module.artworks.Artwork;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.validator.constraints.URL;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        columnNames = {"artwork_id", "sort_order"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArtworkImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Artwork artwork;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = ErrorMessages.URL_REQUIRED)
    @URL(message = ErrorMessages.IMAGE_URL_INVALID)
    private String url;

    @Column(name = "sort_order", nullable = false)
    @PositiveOrZero(message = ErrorMessages.SORT_ORDER_INVALID)
    private int sortOrder;
}
