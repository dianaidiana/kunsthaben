package io.everyonecodes.project_module.chats;

import io.everyonecodes.project_module.artworks.ArtworkService;
import io.everyonecodes.project_module.chats.dto.ChatSummaryResponse;
import io.everyonecodes.project_module.chats.dto.MessageResponse;
import io.everyonecodes.project_module.chats.dto.MessageRequest;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final ArtworkService artworkService;

    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository, UserService userService, ArtworkService artworkService) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.artworkService = artworkService;
    }

    @Transactional
    public ChatSummaryResponse startChat(Long artworkId, Long buyerId, MessageRequest messageRequest) {
        var buyer = userService.fetchUser(buyerId);
        var artwork = artworkService.fetchArtwork(artworkId);

        if (artwork.getArtist().getId().equals(buyerId)) {
            throw new BadRequestException(ErrorMessages.CANNOT_CHAT_WITH_SELF);
        }

        var oChat = chatRepository.findByArtworkIdAndBuyerId(artworkId, buyerId);
        if (oChat.isPresent()) {
            var chat = oChat.get();
            var newMessage = new Message(null, chat, buyer, messageRequest.getContent(), false, null);
            messageRepository.save(newMessage);
            return ChatSummaryResponse.from(chat, newMessage, buyerId);
        }

        var newChat = new Chat(null, artwork, buyer, null, true);
        var newMessage = new Message(null, newChat, buyer, messageRequest.getContent(), false, null);
        chatRepository.save(newChat);
        messageRepository.save(newMessage);

        return ChatSummaryResponse.from(newChat, newMessage, buyerId);
    }

    public Slice<ChatSummaryResponse> getAllChatsByParticipant(Long participantId, Pageable pageable) {
        var chats = chatRepository.findByParticipantId(participantId, pageable);
        return chats.map(chat ->
                ChatSummaryResponse.from(chat,
                        messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId())
                                         .orElseThrow(() -> new IllegalStateException(ErrorMessages.NO_MESSAGES)),
                        participantId)
        );
    }

    @Transactional(readOnly = true)
    public Slice<MessageResponse> getMessages(Long chatId, Long participantId, Pageable pageable) {
        fetchChatAsParticipant(chatId, participantId);
        return messageRepository.findByChatIdOrderByCreatedAtDesc(chatId, pageable).map(MessageResponse::from);
    }

    @Transactional
    public MessageResponse sendMessage(Long chatId, Long senderId, MessageRequest messageRequest) {
        var chat = fetchChatAsParticipant(chatId, senderId);
        var sender = chat.getBuyer().getId().equals(senderId) ? chat.getBuyer() : chat.getArtwork().getArtist();
        var newMessage = new Message(null, chat, sender, messageRequest.getContent(), false, null);
        messageRepository.save(newMessage);
        return MessageResponse.from(newMessage);
    }

    @Transactional
    public void markRead(Long chatId, Long readerId) {
        fetchChatAsParticipant(chatId, readerId);
        messageRepository.markAllAsRead(chatId, readerId);
    }

    private Chat fetchChatAsParticipant(Long chatId, Long participantId) {
        var chat = chatRepository.findById(chatId)
                                 .orElseThrow(() -> new NotFoundException(ErrorMessages.CHAT_NOT_FOUND));

        if (!chat.getBuyer().getId().equals(participantId) && !chat.getArtwork().getArtist().getId().equals(participantId)) {
            throw new ForbiddenException(ErrorMessages.NOT_CHAT_PARTICIPANT);
        }

        return chat;
    }
}
