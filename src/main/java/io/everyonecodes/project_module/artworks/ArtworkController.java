package io.everyonecodes.project_module.artworks;

import io.everyonecodes.project_module.artworks.dto.ArtworkCardResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkCreateRequest;
import io.everyonecodes.project_module.artworks.dto.ArtworkDetailResponse;
import io.everyonecodes.project_module.artworks.dto.ArtworkUpdateRequest;
import io.everyonecodes.project_module.artworks.filters.ArtworkFilter;
import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<ArtworkCardResponse> getAllCards(@PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getAllCards(pageable);
    }

    @GetMapping("/artwork/search")
    Page<ArtworkCardResponse> search(
            @ModelAttribute ArtworkFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction =
                    Sort.Direction.DESC) Pageable pageable) {
        return service.search(filter, pageable);
    }

    @GetMapping("/user/{artistId}/artwork")
    Page<ArtworkCardResponse> getUnsoldCardsByArtistId(@PathVariable Long artistId, @PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getUnsoldCardsByArtistId(artistId, pageable);
    }

    @GetMapping("/user/{artistId}/artwork/sold")
    Page<ArtworkCardResponse> getSoldCardsByArtistId(@PathVariable Long artistId, @PageableDefault(size = 20, sort = "createdAt", direction =
            Sort.Direction.DESC) Pageable pageable) {
        return service.getSoldCardsByArtistId(artistId, pageable);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user/{artistId}/artwork")
    ArtworkDetailResponse create(@PathVariable Long artistId,
                                 @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                 @Valid @RequestBody ArtworkCreateRequest request) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        return service.create(artistId, request);
    }

    @PutMapping("/user/{artistId}/artwork/{artworkId}")
    ArtworkDetailResponse update(@PathVariable Long artistId,
                                 @PathVariable Long artworkId,
                                 @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                 @Valid @RequestBody ArtworkUpdateRequest request) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        return service.update(artistId, artworkId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{artistId}/artwork/{artworkId}")
    void delete(@PathVariable Long artistId, @PathVariable Long artworkId, @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        service.delete(artistId, artworkId);
    }

    @PatchMapping("/user/{artistId}/artwork/{artworkId}/reserved")
    ArtworkDetailResponse markReserved(@PathVariable Long artistId,
                                       @PathVariable Long artworkId,
                                       @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                       @RequestParam boolean reserved) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        return service.markReserved(artistId, artworkId, reserved);
    }

    @PatchMapping("/user/{artistId}/artwork/{artworkId}/sold")
    ArtworkDetailResponse markSold(@PathVariable Long artistId,
                                   @PathVariable Long artworkId,
                                   @AuthenticationPrincipal AuthPrincipal authPrincipal,
                                   @RequestParam boolean sold) {
        throwForbiddenIfNotOwned(authPrincipal, artistId);
        return service.markSold(artistId, artworkId, sold);
    }

    private void throwForbiddenIfNotOwned(AuthPrincipal principal, Long artistId) {
        if (!principal.id().equals(artistId)) {
            throw new ForbiddenException(ErrorMessages.NOT_ARTWORK_OWNER);
        }
    }
}
