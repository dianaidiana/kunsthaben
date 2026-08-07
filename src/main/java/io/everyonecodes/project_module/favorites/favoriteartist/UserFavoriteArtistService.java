package io.everyonecodes.project_module.favorites.favoriteartist;

import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ConflictException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.UserRepository;
import io.everyonecodes.project_module.users.UserResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFavoriteArtistService {

    private final UserFavoriteArtistRepository repository;

    private final UserRepository userRepository;

    public UserFavoriteArtistService(UserFavoriteArtistRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public UserResponse saveFavoriteArtist(Long userId, Long artistId) {
        if (userId.equals(artistId)) {
            throw new BadRequestException(ErrorMessages.SELF_FOLLOW);
        }

        var user = userRepository.findById(userId)
                                 .orElseThrow(() -> new NotFoundException(ErrorMessages.USER_NOT_FOUND));
        var artist = userRepository.findById(artistId)
                                   .orElseThrow(() -> new NotFoundException(ErrorMessages.ARTIST_NOT_FOUND));

        var followExists = repository.existsByUserIdAndArtistId(userId, artistId);
        if (followExists) {
            throw new ConflictException(ErrorMessages.DUPLICATE_FOLLOW);
        }

        repository.save(new UserFavoriteArtist(user, artist));
        return UserResponse.from(artist);
    }

    public void deleteFavoriteArtist(Long userId, Long artistId) {
        var id = new UserFavoriteArtistId(userId, artistId);
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }

    public List<UserResponse> listFavoriteArtists(Long userId) {
        List<UserFavoriteArtist> userFavoriteArtists = repository.findByUserId(userId);
        return userFavoriteArtists.stream()
                                  .map(fav -> UserResponse.from(fav.getArtist()))
                                  .toList();
    }
}
