package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.users.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserFavoriteArtistController {

    private final UserFavoriteArtistService service;

    public UserFavoriteArtistController(UserFavoriteArtistService service) {
        this.service = service;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/favorite-artist/{artistId}")
    UserResponse saveFavoriteArtist(@PathVariable Long artistId,
                                    @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        return service.saveFavoriteArtist(authPrincipal.id(), artistId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/favorite-artist/{artistId}")
    void deleteFavoriteArtist(@PathVariable Long artistId,
                              @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        service.deleteFavoriteArtist(authPrincipal.id(), artistId);
    }

    @GetMapping("/favorite-artist")
    List<UserResponse> listFavoriteArtists(@AuthenticationPrincipal AuthPrincipal authPrincipal) {
        return service.listFavoriteArtists(authPrincipal.id());
    }
}
