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

    public static ArtworkCardResponse from(Artwork artwork) {
        var category = artwork.getCategory();
        var media = artwork.getMedia();
        var support = artwork.getSupport();
        var images = artwork.getImages();
        var imageUrl = images.isEmpty() ? null : images.getFirst().getUrl();

        return new ArtworkCardResponse(
                artwork.getId(),
                imageUrl,
                artwork.getTitle(),
                artwork.getArtist().getName(),
                artwork.getYear(),
                category != null ? category.getCode() : null,
                category != null ? category.getName() : null,
                media != null ? media.getName() : null,
                support != null ? support.getName() : null,
                artwork.getPrice(),
                artwork.isReserved(),
                artwork.getCreatedAt()
        );
    }
}