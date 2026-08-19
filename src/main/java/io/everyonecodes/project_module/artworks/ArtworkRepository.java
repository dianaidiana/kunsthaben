package io.everyonecodes.project_module.artworks;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByIdAndDeletedAtIsNull(Long id);

    Slice<Artwork> findAllByDeletedAtIsNullAndSold(boolean sold, Pageable pageable);

    Slice<Artwork> findAllByArtistIdAndDeletedAtIsNullAndSold(Long artistId, boolean sold, Pageable pageable);
}
