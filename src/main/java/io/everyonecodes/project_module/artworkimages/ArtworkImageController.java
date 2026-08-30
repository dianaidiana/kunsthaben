package io.everyonecodes.project_module.artworkimages;

import io.everyonecodes.project_module.auth.AuthPrincipal;
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
    @PostMapping(path = "/artwork/{artworkId}/images", consumes = "multipart/form-data")
    ArtworkImage addImage(@PathVariable Long artworkId,
                          @AuthenticationPrincipal AuthPrincipal authPrincipal,
                          @RequestParam("file") MultipartFile file) {
        return service.addImage(authPrincipal.id(), artworkId, file);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/artwork/{artworkId}/images/reorder")
    void reorderImages(@PathVariable Long artworkId,
                       @AuthenticationPrincipal AuthPrincipal authPrincipal,
                       @RequestBody List<Long> imageIdsInNewOrder) {
        service.reorderImages(authPrincipal.id(), artworkId, imageIdsInNewOrder);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/artwork/{artworkId}/images/{imageId}")
    void deleteImage(@PathVariable Long artworkId,
                     @PathVariable Long imageId,
                     @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        service.deleteImage(authPrincipal.id(), artworkId, imageId);
    }
}
