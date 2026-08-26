package io.everyonecodes.project_module.chats.dto;

import io.everyonecodes.project_module.chats.Message;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class MessageResponse {

    private Long id;
    private Long senderId;
    private String content;
    private boolean read;
    private OffsetDateTime createdAt;

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getContent(),
                message.isRead(),
                message.getCreatedAt()
        );
    }
}
