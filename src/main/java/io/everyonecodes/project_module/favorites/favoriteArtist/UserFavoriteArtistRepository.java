package io.everyonecodes.project_module.favorites.favoriteArtist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFavoriteArtistRepository extends JpaRepository<UserFavoriteArtist, UserFavoriteArtistId> {

    List<UserFavoriteArtist> findByUserId(Long userId);

    boolean existsByUserIdAndArtistId(Long userId, Long artistId);
}
