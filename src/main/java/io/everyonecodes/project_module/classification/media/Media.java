package io.everyonecodes.project_module.classification.media;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;

    @NotBlank(message = ErrorMessages.CODE_REQUIRED)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
