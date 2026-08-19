package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
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
    @PostMapping("/user/{userId}/favorite-artist/{artistId}")
    UserResponse saveFavoriteArtist(@PathVariable Long userId,
                                    @PathVariable Long artistId,
                                    @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        throwForbiddenIfNotOwned(authPrincipal, userId);
        return service.saveFavoriteArtist(userId, artistId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{userId}/favorite-artist/{artistId}")
    void deleteFavoriteArtist(@PathVariable Long userId,
                              @PathVariable Long artistId,
                              @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        throwForbiddenIfNotOwned(authPrincipal, userId);
        service.deleteFavoriteArtist(userId, artistId);
    }

    @GetMapping("/user/{userId}/favorite-artist")
    List<UserResponse> listFavoriteArtists(@PathVariable Long userId, @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        throwForbiddenIfNotOwned(authPrincipal, userId);
        return service.listFavoriteArtists(userId);
    }

    private void throwForbiddenIfNotOwned(AuthPrincipal principal, Long userId) {
        if (!principal.id().equals(userId)) {
            throw new ForbiddenException(ErrorMessages.NOT_PROFILE_OWNER);
        }
    }
}
