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
                category, medium, support, null, null, null, false, new ArrayList<>());

        if (request.getImageUrls() != null) {
            var sortOrder = 0;
            for (var url : request.getImageUrls()) {
                artwork.getImages().add(new ArtworkImage(null, artwork, url, sortOrder++));
            }
        }

        var savedArtwork = repository.save(artwork);
        return toDetailResponse(savedArtwork);
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

    private ArtworkDetailResponse toDetailResponse(Artwork artwork) {
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
                artwork.isReserved(),
                artwork.getCreatedAt(),
                artwork.getImages().stream().map(ArtworkImage::getUrl).toList()
        );
    }
}



