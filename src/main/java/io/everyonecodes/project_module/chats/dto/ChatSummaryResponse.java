package io.everyonecodes.project_module.chats.dto;

import io.everyonecodes.project_module.chats.Chat;
import io.everyonecodes.project_module.chats.Message;
import io.everyonecodes.project_module.users.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ChatSummaryResponse {

    private Long chatId;
    private Long artworkId;
    private String title;
    private String thumbnailUrl;
    private UserResponse otherParticipant;
    private String lastMessage;
    private OffsetDateTime lastMessageSentAt;
    private boolean messageRead;
    private OffsetDateTime createdAt;

    public static ChatSummaryResponse from(Chat chat, Message lastMessage, Long viewerId) {
        var artwork = chat.getArtwork();
        var images = artwork.getImages();
        var buyer = chat.getBuyer();
        var seller = chat.getSeller();

        return new ChatSummaryResponse(
                chat.getId(),
                artwork.getId(),
                artwork.getTitle(),
                images.isEmpty() ? null : images.getFirst().getUrl(),
                UserResponse.from(buyer.getId().equals(viewerId) ? seller : buyer),
                lastMessage.getContent(),
                lastMessage.getCreatedAt(),
                lastMessage.isRead(),
                chat.getCreatedAt()
        );
    }

}
