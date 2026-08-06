package io.everyonecodes.project_module.artworks;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Setter
@Getter
public class ArtworkFilter {
    private String keyword;

    private Double minPrice;
    private Double maxPrice;

    private List<String> cities;
    private List<String> postcodes;

    private List<Long> categoryIds;
    private List<Long> mediumIds;
    private List<Long> supportIds;

    private Double minWidth;
    private Double maxWidth;
    private Double minHeight;
    private Double maxHeight;
    private Double minDepth;
    private Double maxDepth;

    private Boolean framed;

    private Double minFrameWidth;
    private Double maxFrameWidth;
    private Double minFrameHeight;
    private Double maxFrameHeight;
    private Double minFrameDepth;
    private Double maxFrameDepth;

    private Integer minYear;
    private Integer maxYear;

    private Boolean reserved;
}
