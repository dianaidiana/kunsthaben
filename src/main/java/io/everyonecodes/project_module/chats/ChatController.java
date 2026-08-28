package io.everyonecodes.project_module.chats;

import io.everyonecodes.project_module.auth.AuthPrincipal;
import io.everyonecodes.project_module.chats.dto.ChatSummaryResponse;
import io.everyonecodes.project_module.chats.dto.MessageRequest;
import io.everyonecodes.project_module.chats.dto.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/artwork/{artworkId}/chat")
    ChatSummaryResponse startChat(@PathVariable Long artworkId,
                                  @Valid @RequestBody MessageRequest messageRequest,
                                  @AuthenticationPrincipal AuthPrincipal authPrincipal) {
        return service.startChat(artworkId, authPrincipal.id(), messageRequest);
    }

    @GetMapping("/chat")
    Slice<ChatSummaryResponse> getAllChatsByParticipant(@AuthenticationPrincipal AuthPrincipal authPrincipal,
                                                        @PageableDefault(size = 20, sort = "createdAt", direction =
                                                                Sort.Direction.DESC) Pageable pageable) {
        return service.getAllChatsByParticipant(authPrincipal.id(), pageable);
    }

    @GetMapping("/chat/{chatId}/message")
    Slice<MessageResponse> getMessages(@AuthenticationPrincipal AuthPrincipal authPrincipal,
                                       @PathVariable Long chatId,
                                       @PageableDefault(size = 20, sort = "createdAt", direction =
                                               Sort.Direction.DESC) Pageable pageable) {
        return service.getMessages(chatId, authPrincipal.id(), pageable);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/chat/{chatId}/message")
    MessageResponse sendMessage(@AuthenticationPrincipal AuthPrincipal authPrincipal,
                                @PathVariable Long chatId,
                                @Valid @RequestBody MessageRequest messageRequest) {
        return service.sendMessage(chatId, authPrincipal.id(), messageRequest);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/chat/{chatId}/message/read")
    void markAsRead(@AuthenticationPrincipal AuthPrincipal authPrincipal,
                    @PathVariable Long chatId) {
        service.markRead(chatId, authPrincipal.id());
    }
}
