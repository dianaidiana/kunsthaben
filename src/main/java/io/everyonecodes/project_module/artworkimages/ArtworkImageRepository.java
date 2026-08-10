package io.everyonecodes.project_module.artworkimages;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtworkImageRepository extends JpaRepository<ArtworkImage, Long> {

    List<ArtworkImage> findArtworkImageByArtworkId(Long artworkId);

    int countByArtworkId(Long artworkId);
}
