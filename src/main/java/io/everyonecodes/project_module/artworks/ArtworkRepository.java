package io.everyonecodes.project_module.artworks;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository extends JpaRepository<Artwork, Long>, JpaSpecificationExecutor<Artwork> {
    Optional<Artwork> findByIdAndDeletedAtIsNull(Long id);

    Slice<Artwork> findAllByDeletedAtIsNullAndSold(boolean sold, Pageable pageable);

    Slice<Artwork> findAllByArtistIdAndDeletedAtIsNullAndSold(Long artistId, boolean sold, Pageable pageable);

    @Query(value = "SELECT id FROM artwork WHERE search_vector @@ websearch_to_tsquery('english', :keywords)",
            nativeQuery = true)
    List<Long> findIdsMatchingKeywords(@Param("keywords") String keywords);
}
