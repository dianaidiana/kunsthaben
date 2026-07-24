package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.users.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Artwork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User artist;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String title;

    @Column(columnDefinition = "NUMERIC(12,2)", nullable = false)
    private double price;

    @Column(nullable = false)
    private int year;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String city;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String postcode;

    @Embedded
    private Dimensions dimensions;

    @Embedded
    private Frame frame;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Category category;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Media media;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Support support;

    @Column(updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime deletedAt;

    @Column
    private OffsetDateTime soldAt;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private boolean reserved;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ArtworkImage> images = new ArrayList<>();
}
