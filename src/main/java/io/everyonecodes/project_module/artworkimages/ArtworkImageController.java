package io.everyonecodes.project_module.artworkimages;

import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ArtworkImageController {

    private final ArtworkImageService service;

    public ArtworkImageController(ArtworkImageService service) {
        this.service = service;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/user/{artistId}/artwork/{artworkId}/images", consumes = "multipart/form-data")
    ArtworkImage addImage(@PathVariable Long artistId,
                          @PathVariable Long artworkId,
                          @AuthenticationPrincipal AuthPrincipal authPrincipal,
                          @RequestParam("file") MultipartFile file) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        return service.addImage(artistId, artworkId, file);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/user/{artistId}/artwork/{artworkId}/images/reorder")
    void reorderImages(@PathVariable Long artistId,
                       @PathVariable Long artworkId,
                       @AuthenticationPrincipal AuthPrincipal authPrincipal,
                       @RequestBody List<Long> imageIdsInNewOrder) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        service.reorderImages(artistId, artworkId, imageIdsInNewOrder);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{artistId}/artwork/{artworkId}/images/{imageId}")
    void deleteImage(@PathVariable Long artistId,
                     @PathVariable Long artworkId,
                     @PathVariable Long imageId,
                     @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        service.deleteImage(artistId, artworkId, imageId);
    }

    private void throwForbiddenIfNotOwned(AuthPrincipal principal, Long artistId) {
        if (!principal.id().equals(artistId)) {
            throw new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER);
        }
    }
}
