package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ArtworkOwnershipService {

    private final ArtworkRepository repository;

    public ArtworkOwnershipService(ArtworkRepository repository) {
        this.repository = repository;
    }

    public Artwork fetchOwnedArtwork(Long artistId, Long artworkId) {
        var artwork = repository.findByIdAndDeletedAtIsNull(artworkId)
                                .orElseThrow(() -> new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));

        if (!artwork.getArtist().getId().equals(artistId)) {
            throw new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER);
        }
        return artwork;
    }
}
