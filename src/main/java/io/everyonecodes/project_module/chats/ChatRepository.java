package io.everyonecodes.project_module.chats;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByArtworkIdAndBuyerId(Long artworkId, Long buyerId);

    @Query("SELECT c FROM Chat c WHERE c.buyer.id = :userId OR c.artwork.artist.id = :userId")
    Slice<Chat> findByParticipantId(@Param("userId") Long userId, Pageable pageable);
}
