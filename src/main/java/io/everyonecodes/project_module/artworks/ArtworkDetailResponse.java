package io.everyonecodes.project_module.artworks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ArtworkDetailResponse {

    private Long id;
    private String title;
    private String description;
    private double price;
    private int year;
    private String city;
    private String postcode;

    private double width;
    private double height;
    private Double depth;

    private boolean framed;
    private Double frameWidth;
    private Double frameHeight;
    private Double frameDepth;

    private Long artistId;
    private String artistName;
    private String aboutArtist;

    private Long categoryId;
    private Long categoryCode;
    private String categoryName;

    private Long mediumId;
    private String mediumName;

    private Long supportId;
    private String supportName;

    private boolean reserved;
    private OffsetDateTime createdAt;

    private List<String> imageUrls;
}
