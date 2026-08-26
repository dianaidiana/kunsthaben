package io.everyonecodes.project_module.chats.dto;

import io.everyonecodes.project_module.exceptions.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SendMessageRequest {

    @NotBlank(message = ErrorMessages.CONTENT_REQUIRED)
    @Size(max = 1000, message = ErrorMessages.MAX_CHARACTERS)
    private String content;
}
