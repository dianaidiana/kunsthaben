package io.everyonecodes.project_module.artworks;

import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;

public class ArtworkSpecifications {

    public static Specification<Artwork> matchesKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return null;
        }
        return Arrays.stream(keywords.trim().split("\\s+"))
                     .map(ArtworkSpecifications::matchesKeyword)
                     .reduce(Specification::and)
                     .orElse(null);
    }

    private static Specification<Artwork> matchesKeyword(String word) {
        var pattern = "%" + word.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Artwork> hasMinPrice(Double minPrice) {
        if (minPrice == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"),
                minPrice);
    }

    public static Specification<Artwork> hasMaxPrice(Double maxPrice) {
        if (maxPrice == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"),
                maxPrice);
    }

    public static Specification<Artwork> hasCityIn(List<String> cities) {
        if (cities == null || cities.isEmpty()) return null;
        return (root, query, cb) -> root.get("city").in(cities);
    }

    public static Specification<Artwork> hasPostcodeIn(List<String> postcodes) {
        if (postcodes == null || postcodes.isEmpty()) return null;
        return (root, query, cb) -> root.get("postcode").in(postcodes);
    }

    public static Specification<Artwork> hasCategoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Artwork> hasMediumIn(List<Long> mediumIds) {
        if (mediumIds == null || mediumIds.isEmpty()) return null;
        return (root, query, cb) -> root.get("media").get("id").in(mediumIds);
    }

    public static Specification<Artwork> hasSupportIn(List<Long> supportIds) {
        if (supportIds == null || supportIds.isEmpty()) return null;
        return (root, query, cb) -> root.get("support").get("id").in(supportIds);
    }

    public static Specification<Artwork> hasMinWidth(Double minWidth) {
        if (minWidth == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dimensions").get("width"),
                minWidth);
    }

    public static Specification<Artwork> hasMaxWidth(Double maxWidth) {
        if (maxWidth == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dimensions").get("width"),
                maxWidth);
    }

    public static Specification<Artwork> hasMinHeight(Double minHeight) {
        if (minHeight == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dimensions").get("height"),
                minHeight);
    }

    public static Specification<Artwork> hasMaxHeight(Double maxHeight) {
        if (maxHeight == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dimensions").get("height"),
                maxHeight);
    }

    public static Specification<Artwork> hasMinDepth(Double minDepth) {
        if (minDepth == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dimensions").get("depth"),
                minDepth);
    }

    public static Specification<Artwork> hasMaxDepth(Double maxDepth) {
        if (maxDepth == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dimensions").get("depth"),
                maxDepth);
    }

    public static Specification<Artwork> isFramed(Boolean framed) {
        if (framed == null) return null;
        return (root, query, cb) -> cb.equal(root.get("frame").get("framed"),
                framed);
    }

    public static Specification<Artwork> hasMinFrameWidth(Double minFrameWidth) {
        if (minFrameWidth == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("frame").get("width"),
                minFrameWidth);
    }

    public static Specification<Artwork> hasMaxFrameWidth(Double maxFrameWidth) {
        if (maxFrameWidth == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("frame").get("width"),
                maxFrameWidth);
    }

    public static Specification<Artwork> hasMinFrameHeight(Double minFrameHeight) {
        if (minFrameHeight == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("frame").get("height"),
                minFrameHeight);
    }

    public static Specification<Artwork> hasMaxFrameHeight(Double maxFrameHeight) {
        if (maxFrameHeight == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("frame").get("height"),
                maxFrameHeight);
    }

    public static Specification<Artwork> hasMinFrameDepth(Double minFrameDepth) {
        if (minFrameDepth == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("frame").get("depth"),
                minFrameDepth);
    }

    public static Specification<Artwork> hasMaxFrameDepth(Double maxFrameDepth) {
        if (maxFrameDepth == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("frame").get("depth"),
                maxFrameDepth);
    }

    public static Specification<Artwork> hasMinYear(Integer minYear) {
        if (minYear == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("year"),
                minYear);
    }

    public static Specification<Artwork> hasMaxYear(Integer maxYear) {
        if (maxYear == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("year"),
                maxYear);
    }

    public static Specification<Artwork> hasReserved(Boolean reserved) {
        if (reserved == null) return null;
        return (root, query, cb) -> cb.equal(root.get("reserved"), reserved);
    }

    public static Specification<Artwork> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Artwork> isNotSold() {
        return (root, query, cb) -> cb.isFalse(root.get("sold"));
    }

    public static Specification<Artwork> build(ArtworkFilter filter) {
        return Specification
                .where(isNotDeleted())
                .and(isNotSold())
                .and(matchesKeywords(filter.getKeywords()))
                .and(hasMinPrice(filter.getMinPrice()))
                .and(hasMaxPrice(filter.getMaxPrice()))
                .and(hasCityIn(filter.getCities()))
                .and(hasPostcodeIn(filter.getPostcodes()))
                .and(hasCategoryIn(filter.getCategoryIds()))
                .and(hasMediumIn(filter.getMediumIds()))
                .and(hasSupportIn(filter.getSupportIds()))
                .and(hasMinWidth(filter.getMinWidth()))
                .and(hasMaxWidth(filter.getMaxWidth()))
                .and(hasMinHeight(filter.getMinHeight()))
                .and(hasMaxHeight(filter.getMaxHeight()))
                .and(hasMinDepth(filter.getMinDepth()))
                .and(hasMaxDepth(filter.getMaxDepth()))
                .and(isFramed(filter.getFramed()))
                .and(hasMinFrameWidth(filter.getMinFrameWidth()))
                .and(hasMaxFrameWidth(filter.getMaxFrameWidth()))
                .and(hasMinFrameHeight(filter.getMinFrameHeight()))
                .and(hasMaxFrameHeight(filter.getMaxFrameHeight()))
                .and(hasMinFrameDepth(filter.getMinFrameDepth()))
                .and(hasMaxFrameDepth(filter.getMaxFrameDepth()))
                .and(hasMinYear(filter.getMinYear()))
                .and(hasMaxYear(filter.getMaxYear()))
                .and(hasReserved(filter.getReserved()));
    }

}
