package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ArtworkController {

    private final ArtworkService service;

    public ArtworkController(ArtworkService service) {
        this.service = service;
    }

    @GetMapping("/artwork/{id}")
    ArtworkDetailResponse getDetailById(@PathVariable Long id) {
        return service.getDetailById(id)
                      .orElseThrow(() -> new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));
    }

    @GetMapping("/artwork")
    List<ArtworkCardResponse> getAllCards() {
        return service.getAllCards();
    }

    @GetMapping("/user/{artistId}/artwork")
    List<ArtworkCardResponse> getAllCardsByArtistIdNewestFirst(@PathVariable Long artistId) {
        return service.getAllCardsByArtistIdNewestFirst(artistId);
    }

    @PostMapping("/user/{artistId}/artwork")
    ArtworkDetailResponse create(@PathVariable Long artistId, @Valid @RequestBody ArtworkCreateRequest request) {
        return service.create(artistId, request);
    }

    @PutMapping("/user/{artistId}/artwork/{artworkId}")
    ArtworkDetailResponse update(@PathVariable Long artistId, @PathVariable Long artworkId, @Valid @RequestBody ArtworkUpdateRequest request) {
        return service.update(artistId, artworkId, request);
    }

    @DeleteMapping("/user/{artistId}/artwork/{artworkId}")
    void delete(@PathVariable Long artistId, @PathVariable Long artworkId) {
        service.delete(artistId, artworkId);
    }

    @PatchMapping("/user/{artistId}/artwork/{artworkId}/reserved")
    ArtworkDetailResponse markReserved(@PathVariable Long artistId, @PathVariable Long artworkId, @RequestParam boolean reserved) {
        return service.markReserved(artistId, artworkId, reserved);
    }

    @PatchMapping("/user/{artistId}/artwork/{artworkId}/sold")
    ArtworkDetailResponse markSold(@PathVariable Long artistId, @PathVariable Long artworkId, @RequestParam boolean sold) {
        return service.markSold(artistId, artworkId, sold);
    }
}
