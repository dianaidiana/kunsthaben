package io.everyonecodes.project_module.artworkimages;

import io.everyonecodes.project_module.artworkimages.dto.ArtworkImageRequest;
import io.everyonecodes.project_module.artworks.ArtworkService;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArtworkImageService {

    private final ArtworkImageRepository repository;
    private final ArtworkService artworkService;

    public static final int MAX_IMAGES_PER_ARTWORK = 10;

    public ArtworkImageService(ArtworkImageRepository repository, ArtworkService artworkService) {
        this.repository = repository;
        this.artworkService = artworkService;
    }

    @Transactional
    public ArtworkImage addImage(Long artistId, Long artworkId, ArtworkImageRequest request) {
        var artwork = artworkService.fetchOwnedArtwork(artistId, artworkId);
        var currentCount = repository.countByArtworkId(artworkId);
        if (currentCount >= MAX_IMAGES_PER_ARTWORK) {
            throw new BadRequestException(ErrorMessages.TOO_MANY_IMAGES);
        }

        return repository.save(new ArtworkImage(null, artwork, request.getUrl(), currentCount));
    }

    @Transactional
    public void reorderImages(Long artistId, Long artworkId, List<Long> imageIdsInNewOrder) {
        artworkService.fetchOwnedArtwork(artistId, artworkId);

        // get fresh list of current images, to avoid using old cache memory
        var currentImages = repository.findArtworkImageByArtworkId(artworkId);
        var currentIds = currentImages
                .stream()
                .map(ArtworkImage::getId)
                .collect(Collectors.toSet());

        var isSameSet = new HashSet<>(imageIdsInNewOrder).equals(currentIds);
        var hasSameLength = imageIdsInNewOrder.size() == currentImages.size();
        if (!isSameSet || !hasSameLength) {
            throw new BadRequestException(ErrorMessages.IMAGES_DO_NOT_MATCH);
        }

        // I move them all to a new invalid index (MAX * 2 + i) to avoid unique constraints collisions
        for (int i = 0; i < imageIdsInNewOrder.size(); i++) {
            var image = repository.getReferenceById(imageIdsInNewOrder.get(i));
            image.setSortOrder(i + MAX_IMAGES_PER_ARTWORK * 2);
        }

        // then I save the new order
        for (int i = 0; i < imageIdsInNewOrder.size(); i++) {
            var image = repository.getReferenceById(imageIdsInNewOrder.get(i));
            image.setSortOrder(i);
        }
    }

    @Transactional
    public void deleteImage(Long artistId, Long artworkId, Long imageId) {
        artworkService.fetchOwnedArtwork(artistId, artworkId);
        var image = repository.findById(imageId)
                              .filter(img -> img.getArtwork().getId().equals(artworkId))
                              .orElseThrow(() -> new NotFoundException(ErrorMessages.IMAGE_NOT_FOUND));
        var remainingIds = repository.findArtworkImageByArtworkId(artworkId).stream()
                                     .sorted(Comparator.comparing(ArtworkImage::getSortOrder))
                                     .map(ArtworkImage::getId)
                                     .filter(id -> !id.equals(imageId))
                                     .toList();
        repository.delete(image);
        reorderImages(artistId, artworkId, remainingIds);
    }
}
