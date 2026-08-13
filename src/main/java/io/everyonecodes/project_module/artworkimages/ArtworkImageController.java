package io.everyonecodes.project_module.artworkimages;

import org.springframework.http.HttpStatus;
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
    ArtworkImage addImage(@PathVariable Long artistId, @PathVariable Long artworkId, @RequestParam("file") MultipartFile file) {
        return service.addImage(artistId, artworkId, file);
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
