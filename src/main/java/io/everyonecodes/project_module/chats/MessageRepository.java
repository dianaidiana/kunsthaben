package io.everyonecodes.project_module.chats;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Slice<Message> findByChatIdOrderByCreatedAtDesc(Long chatId, Pageable pageable);

    Optional<Message> findFirstByChatIdOrderByCreatedAtDesc(Long chatId);

    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.chat.id = :chatId AND m.sender.id <> :readerId AND m.read = false")
    int markAllAsRead(@Param("chatId") Long chatId, @Param("readerId") Long readerId);

}
