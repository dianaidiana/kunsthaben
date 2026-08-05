package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.category.CategoryRepository;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.media.MediaRepository;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.classification.support.SupportRepository;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ArtworkService {

    private final ArtworkRepository repository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MediaRepository mediaRepository;
    private final SupportRepository supportRepository;

    public ArtworkService(ArtworkRepository repository, UserRepository userRepository, CategoryRepository categoryRepository, MediaRepository mediaRepository, SupportRepository supportRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.mediaRepository = mediaRepository;
        this.supportRepository = supportRepository;
    }

    public Optional<ArtworkDetailResponse> getDetailById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                         .map(ArtworkDetailResponse::from);
    }

    public List<ArtworkCardResponse> getAllCards() {
        return repository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                         .stream()
                         .map(ArtworkCardResponse::from)
                         .toList();
    }

    public List<ArtworkCardResponse> getAllCardsByArtistIdNewestFirst(Long artistId) {
        return repository.findAllByArtistIdAndDeletedAtIsNullOrderByCreatedAtDesc(artistId)
                         .stream()
                         .map(ArtworkCardResponse::from)
                         .toList();
    }

    public ArtworkDetailResponse create(Long artistId, ArtworkCreateRequest request) {
        var artist = userRepository.findById(artistId)
                                   .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
        var category = fetchCategory(request.getCategoryId());
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
        var artwork = fetchOwnedArtwork(artistId, artworkId);
        var category = fetchCategory(request.getCategoryId());
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
        var artwork = fetchOwnedArtwork(artistId, artworkId);
        artwork.setDeletedAt(OffsetDateTime.now());
    }

    @Transactional
    public ArtworkDetailResponse markReserved(Long artistId, Long artworkId, boolean reserved) {
        var artwork = fetchOwnedArtwork(artistId, artworkId);
        artwork.setReserved(reserved);
        return ArtworkDetailResponse.from(artwork);
    }

    @Transactional
    public ArtworkDetailResponse markSold(Long artistId, Long artworkId, boolean sold) {
        var artwork = fetchOwnedArtwork(artistId, artworkId);
        artwork.setSold(sold);
        return ArtworkDetailResponse.from(artwork);
    }

    private Artwork fetchOwnedArtwork(Long artistId, Long artworkId) {
        var artwork = repository.findByIdAndDeletedAtIsNull(artworkId)
                                .orElseThrow(() -> new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));

        if (!artwork.getArtist().getId().equals(artistId)) {
            throw new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER);
        }
        return artwork;
    }

    private Category fetchCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                                 .orElseThrow(() -> new NotFoundException(ErrorMessages.CATEGORY_NOT_FOUND));
    }

    private Media fetchMedium(Long mediumId) {
        if (mediumId == null) {
            return null;
        }
        return mediaRepository.findById(mediumId)
                              .orElseThrow(() -> new NotFoundException(ErrorMessages.MEDIA_NOT_FOUND));
    }

    private Support fetchSupport(Long supportId) {
        if (supportId == null) {
            return null;
        }
        return supportRepository.findById(supportId)
                                .orElseThrow(() -> new NotFoundException(ErrorMessages.SUPPORT_NOT_FOUND));
    }
}



