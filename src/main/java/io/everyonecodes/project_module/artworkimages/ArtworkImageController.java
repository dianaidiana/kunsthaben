package io.everyonecodes.project_module.artworkimages;

import io.everyonecodes.project_module.artworkimages.dto.ArtworkImageRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ArtworkImageController {

    private final ArtworkImageService service;

    public ArtworkImageController(ArtworkImageService service) {
        this.service = service;
    }

    @PostMapping("/user/{artistId}/artwork/{artworkId}/images")
    ArtworkImage addImage(@PathVariable Long artistId, @PathVariable Long artworkId, @Valid @RequestBody ArtworkImageRequest request) {
        return service.addImage(artistId, artworkId, request);
    }

    @PutMapping("/user/{artistId}/artwork/{artworkId}/images/reorder")
    void reorderImages(@PathVariable Long artistId, @PathVariable Long artworkId, @RequestBody List<Long> imageIdsInNewOrder) {
        service.reorderImages(artistId, artworkId, imageIdsInNewOrder);
    }

    @DeleteMapping("/user/{artistId}/artwork/{artworkId}/images/{imageId}")
    void deleteImage(@PathVariable Long artistId, @PathVariable Long artworkId, @PathVariable Long imageId) {
        service.deleteImage(artistId, artworkId, imageId);
    }


}
