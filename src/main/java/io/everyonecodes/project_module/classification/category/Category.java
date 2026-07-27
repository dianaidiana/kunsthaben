package io.everyonecodes.project_module.classification.category;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = ErrorMessages.NAME_REQUIRED)
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String name;

    @NotBlank(message = ErrorMessages.CODE_REQUIRED)
    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String code;

}
