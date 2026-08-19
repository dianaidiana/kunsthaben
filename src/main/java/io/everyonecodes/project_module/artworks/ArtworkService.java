package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworkimages.ArtworkImageService;
import io.everyonecodes.project_module.artworks.dimensions.Dimensions;
import io.everyonecodes.project_module.artworks.dimensions.Frame;
import io.everyonecodes.project_module.artworks.dto.ArtworkCardResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkCreateRequest;
import io.everyonecodes.project_module.artworks.dto.ArtworkDetailResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkUpdateRequest;
import io.everyonecodes.project_module.artworks.filters.ArtworkFilter;
import io.everyonecodes.project_module.artworks.filters.ArtworkSpecifications;
import io.everyonecodes.project_module.classification.category.CategoryService;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.media.MediaService;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.classification.support.SupportService;
import io.everyonecodes.project_module.users.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class ArtworkService {

    private final ArtworkRepository repository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final MediaService mediaService;
    private final SupportService supportService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkOwnershipService artworkOwnershipService;

    public ArtworkService(ArtworkRepository repository, UserService userService, CategoryService categoryService, MediaService mediaService, SupportService supportService, ArtworkImageService artworkImageService, ArtworkOwnershipService artworkOwnershipService) {
        this.repository = repository;
        this.userService = userService;
        this.categoryService = categoryService;
        this.mediaService = mediaService;
        this.supportService = supportService;
        this.artworkImageService = artworkImageService;
        this.artworkOwnershipService = artworkOwnershipService;
    }

    @Transactional(readOnly = true)
    public Optional<ArtworkDetailResponse> getDetailById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                         .map(ArtworkDetailResponse::from);
    }

    @Transactional(readOnly = true)
    public Slice<ArtworkCardResponse> getAllCards(Pageable pageable) {
        return repository.findAllByDeletedAtIsNullAndSold(false, pageable)
                         .map(ArtworkCardResponse::from);
    }

    @Transactional(readOnly = true)
    public Slice<ArtworkCardResponse> search(ArtworkFilter filter, Pageable pageable) {
        return repository.findBy(ArtworkSpecifications.build(filter), query -> query.slice(pageable))
                         .map(ArtworkCardResponse::from);
    }

    @Transactional(readOnly = true)
    public Slice<ArtworkCardResponse> getUnsoldCardsByArtistId(Long artistId, Pageable pageable) {
        return repository.findAllByArtistIdAndDeletedAtIsNullAndSold(artistId, false, pageable)
                         .map(ArtworkCardResponse::from);
    }

    @Transactional(readOnly = true)
    public Slice<ArtworkCardResponse> getSoldCardsByArtistId(Long artistId, Pageable pageable) {
        return repository.findAllByArtistIdAndDeletedAtIsNullAndSold(artistId, true, pageable)
                         .map(ArtworkCardResponse::from);
    }

    public ArtworkDetailResponse create(Long artistId, ArtworkCreateRequest request) {
        var artist = userService.fetchUser(artistId);
        var category = categoryService.fetchCategory(request.getCategoryId());
        var medium = fetchMedium(request.getMediumId());
        var support = fetchSupport(request.getSupportId());
        var dimensions = Dimensions.of(request.getWidth(), request.getHeight(), request.getDepth());
        var frame = Frame.of(request.isFramed(), request.getFrameWidth(), request.getFrameHeight(), request.getFrameDepth());

        var artwork = new Artwork(null, artist, request.getTitle(), request.getPrice(), request.getYear(),
                request.getDescription(), request.getCity(), request.getPostcode(), dimensions, frame,
                category, medium, support, null, null, false, false, new ArrayList<>());

        var savedArtwork = repository.save(artwork);
        return ArtworkDetailResponse.from(savedArtwork);
    }


    @Transactional
    public ArtworkDetailResponse update(Long artistId, Long artworkId, ArtworkUpdateRequest request) {
        var artwork = artworkOwnershipService.fetchOwnedArtwork(artistId, artworkId);
        var category = categoryService.fetchCategory(request.getCategoryId());
        var medium = fetchMedium(request.getMediumId());
        var support = fetchSupport(request.getSupportId());
        var dimensions = Dimensions.of(request.getWidth(), request.getHeight(), request.getDepth());
        var frame = Frame.of(request.isFramed(), request.getFrameWidth(), request.getFrameHeight(), request.getFrameDepth());

        artwork.setTitle(request.getTitle());
        artwork.setPrice(request.getPrice());
        artwork.setYear(request.getYear());
        artwork.setDescription(request.getDescription());
        artwork.setCity(request.getCity());
        artwork.setPostcode(request.getPostcode());
        artwork.setDimensions(dimensions);
        artwork.setFrame(frame);
        artwork.setCategory(category);
        artwork.setMedia(medium);
        artwork.setSupport(support);

        return ArtworkDetailResponse.from(artwork);
    }

    @Transactional
    public void delete(Long artistId, Long artworkId) {
        var artwork = artworkOwnershipService.fetchOwnedArtwork(artistId, artworkId);
        artwork.setDeletedAt(OffsetDateTime.now());
        artworkImageService.deleteAllImagesForArtwork(artworkId);
    }

    @Transactional
    public ArtworkDetailResponse markReserved(Long artistId, Long artworkId, boolean reserved) {
        var artwork = artworkOwnershipService.fetchOwnedArtwork(artistId, artworkId);
        artwork.setReserved(reserved);
        return ArtworkDetailResponse.from(artwork);
    }

    @Transactional
    public ArtworkDetailResponse markSold(Long artistId, Long artworkId, boolean sold) {
        var artwork = artworkOwnershipService.fetchOwnedArtwork(artistId, artworkId);
        artwork.setSold(sold);
        return ArtworkDetailResponse.from(artwork);
    }

    private Media fetchMedium(Long mediumId) {
        if (mediumId == null) {
            return null;
        }
        return mediaService.fetchMedia(mediumId);
    }

    private Support fetchSupport(Long supportId) {
        if (supportId == null) {
            return null;
        }
        return supportService.fetchSupport(supportId);
    }
}



