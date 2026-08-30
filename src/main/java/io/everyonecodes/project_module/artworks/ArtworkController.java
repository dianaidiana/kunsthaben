package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworks.dto.ArtworkCardResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkCreateRequest;
import io.everyonecodes.project_module.artworks.dto.ArtworkDetailResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkUpdateRequest;
import io.everyonecodes.project_module.artworks.filters.ArtworkFilter;
import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    Slice<ArtworkCardResponse> getAllCards(@PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getAllCards(pageable);
    }

    @GetMapping("/artwork/search")
    Slice<ArtworkCardResponse> search(
            @ModelAttribute ArtworkFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction =
                    Sort.Direction.DESC) Pageable pageable) {
        return service.search(filter, pageable);
    }

    @GetMapping("/user/{artistId}/artwork")
    Slice<ArtworkCardResponse> getUnsoldCardsByArtistId(@PathVariable Long artistId, @PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getUnsoldCardsByArtistId(artistId, pageable);
    }

    @GetMapping("/user/{artistId}/artwork/sold")
    Slice<ArtworkCardResponse> getSoldCardsByArtistId(@PathVariable Long artistId, @PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getSoldCardsByArtistId(artistId, pageable);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/artwork")
    ArtworkDetailResponse create(@AuthenticationPrincipal AuthPrincipal authPrincipal,
                                 @Valid @RequestBody ArtworkCreateRequest request) {
        return service.create(authPrincipal.id(), request);
    }

    @PutMapping("/artwork/{artworkId}")
    ArtworkDetailResponse update(@PathVariable Long artworkId,
                                 @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                 @Valid @RequestBody ArtworkUpdateRequest request) {
        return service.update(authPrincipal.id(), artworkId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/artwork/{artworkId}")
    void delete(@PathVariable Long artworkId, @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        service.delete(authPrincipal.id(), artworkId);
    }

    @PatchMapping("/artwork/{artworkId}/reserved")
    ArtworkDetailResponse markReserved(@PathVariable Long artworkId,
                                       @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                       @RequestParam boolean reserved) {
        return service.markReserved(authPrincipal.id(), artworkId, reserved);
    }

    @PatchMapping("/artwork/{artworkId}/sold")
    ArtworkDetailResponse markSold(@PathVariable Long artworkId,
                                   @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                   @RequestParam boolean sold) {
        return service.markSold(authPrincipal.id(), artworkId, sold);
    }
}
