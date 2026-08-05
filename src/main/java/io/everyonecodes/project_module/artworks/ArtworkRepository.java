package io.everyonecodes.project_module.artworks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByIdAndDeletedAtIsNull(Long id);

    List<Artwork> findAllByDeletedAtIsNullAndSoldOrderByCreatedAtDesc(boolean sold);
    
    List<Artwork> findAllByArtistIdAndDeletedAtIsNullAndSoldOrderByCreatedAtDesc(Long artistId, boolean sold);
}
