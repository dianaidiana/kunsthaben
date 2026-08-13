package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.users.dto.UserResponse;
import org.springframework.http.HttpStatus;
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
    UserResponse saveFavoriteArtist(@PathVariable Long userId, @PathVariable Long artistId) {
        return service.saveFavoriteArtist(userId, artistId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/user/{userId}/favorite-artist/{artistId}")
    void deleteFavoriteArtist(@PathVariable Long userId, @PathVariable Long artistId) {
        service.deleteFavoriteArtist(userId, artistId);
    }

    @GetMapping("/user/{userId}/favorite-artist")
    List<UserResponse> listFavoriteArtists(@PathVariable Long userId) {
        return service.listFavoriteArtists(userId);
    }
}
