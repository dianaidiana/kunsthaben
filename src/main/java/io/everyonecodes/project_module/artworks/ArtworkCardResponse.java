package io.everyonecodes.project_module.artworks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ArtworkCardResponse {

    private Long id;
    private String imageUrl;
    private String title;
    private String artistName;
    private int year;
    private String categoryCode;
    private String categoryName;
    private String mediaName;
    private String supportName;
    private double price;
    private boolean reserved;
    private OffsetDateTime createdAt;
}