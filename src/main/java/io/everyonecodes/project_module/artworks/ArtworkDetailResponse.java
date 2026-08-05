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
    private String categoryCode;
    private String categoryName;

    private Long mediumId;
    private String mediumName;

    private Long supportId;
    private String supportName;

    private boolean sold;
    private boolean reserved;
    private OffsetDateTime createdAt;

    private List<String> imageUrls;

    public static ArtworkDetailResponse from(Artwork artwork) {
        var dimensions = artwork.getDimensions();
        var frame = artwork.getFrame();
        var artist = artwork.getArtist();
        var category = artwork.getCategory();
        var medium = artwork.getMedia();
        var support = artwork.getSupport();

        return new ArtworkDetailResponse(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getDescription(),
                artwork.getPrice(),
                artwork.getYear(),
                artwork.getCity(),
                artwork.getPostcode(),
                dimensions.getX(),
                dimensions.getY(),
                dimensions.getZ(),
                frame.isFramed(),
                frame.getDimX(),
                frame.getDimY(),
                frame.getDimZ(),
                artist.getId(),
                artist.getName(),
                artist.getAbout(),
                category != null ? category.getId() : null,
                category != null ? category.getCode() : null,
                category != null ? category.getName() : null,
                medium != null ? medium.getId() : null,
                medium != null ? medium.getName() : null,
                support != null ? support.getId() : null,
                support != null ? support.getName() : null,
                artwork.isSold(),
                artwork.isReserved(),
                artwork.getCreatedAt(),
                artwork.getImages().stream().map(ArtworkImage::getUrl).toList()
        );
    }
}
