package io.everyonecodes.project_module.chats;

import io.everyonecodes.project_module.artworks.Artwork;
import io.everyonecodes.project_module.artworks.ArtworkService;
import io.everyonecodes.project_module.artworks.dimensions.Dimensions;
import io.everyonecodes.project_module.artworks.dimensions.Frame;
import io.everyonecodes.project_module.chats.dto.ChatSummaryResponse;
import io.everyonecodes.project_module.chats.dto.MessageRequest;
import io.everyonecodes.project_module.chats.dto.MessageResponse;
import io.everyonecodes.project_module.classification.category.Category;
import io.everyonecodes.project_module.classification.enums.CategoryEnum;
import io.everyonecodes.project_module.classification.enums.MediaEnum;
import io.everyonecodes.project_module.classification.enums.SupportEnum;
import io.everyonecodes.project_module.classification.media.Media;
import io.everyonecodes.project_module.classification.support.Support;
import io.everyonecodes.project_module.exceptions.BadRequestException;
import io.everyonecodes.project_module.exceptions.ErrorMessages;
import io.everyonecodes.project_module.exceptions.ForbiddenException;
import io.everyonecodes.project_module.exceptions.NotFoundException;
import io.everyonecodes.project_module.users.User;
import io.everyonecodes.project_module.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    ChatService service;

    @Mock
    ChatRepository chatRepository;

    @Mock
    MessageRepository messageRepository;

    @Mock
    UserService userService;

    @Mock
    ArtworkService artworkService;

    @BeforeEach
    void setup() {
        service = new ChatService(
                chatRepository,
                messageRepository,
                userService,
                artworkService
        );
    }

    private final OffsetDateTime createdAt = OffsetDateTime.parse("2024-01-01T09:15:30Z");
    private final User artist = new User(1L, "Bob Ross", "bob@ross.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);
    private final User buyer = new User(2L, "Diana", "test@test.com", "hashhashhash", null, null, "Vienna", "1020", null, createdAt);

    private final Category category = new Category(1L, CategoryEnum.PAINTING.getName(), CategoryEnum.PAINTING.getCode());
    private final Media media = new Media(1L, MediaEnum.OIL.getName(), MediaEnum.OIL.getCode(), category);
    private final Support support = new Support(1L, SupportEnum.CANVAS.getName(), SupportEnum.CANVAS.getCode(), category);

    private final Artwork artwork = new Artwork(
            1L, artist,
            "happy accident", 100.0, 2026, "this is a beautiful painting",
            "Vienna", "1020",
            Dimensions.of(30, 40, null),
            Frame.of(false, null, null, null),
            category, media, support,
            createdAt, null,
            false, false,
            new ArrayList<>()
    );


    @Test
    void startChatSuccessfully() {
        when(userService.fetchUser(2L)).thenReturn(buyer);
        when(artworkService.fetchArtwork(1L)).thenReturn(artwork);
        when(chatRepository.findByArtworkIdAndBuyerId(1L, 2L)).thenReturn(Optional.empty());
        when(chatRepository.save(any())).thenAnswer(invocation -> {
            Chat savedChat = invocation.getArgument(0);
            savedChat.setId(1L);
            savedChat.setCreatedAt(createdAt);
            return savedChat;
        });
        var expectedChat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message savedMessage = invocation.getArgument(0);
            savedMessage.setId(1L);
            savedMessage.setCreatedAt(createdAt);
            return savedMessage;
        });
        var expectedMessage = new Message(1L, expectedChat, buyer, "Hello!", false, createdAt);

        var result = service.startChat(1L, 2L, new MessageRequest("Hello!"));
        var chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(chatCaptor.capture());
        var savedChat = chatCaptor.getValue();
        var messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        var savedMessage = messageCaptor.getValue();

        assertEquals(expectedChat, savedChat);
        assertEquals(expectedMessage, savedMessage);
        assertEquals(ChatSummaryResponse.from(expectedChat, expectedMessage, buyer.getId()), result);
    }

    @Test
    void startChatUnexistentUser() {
        when(userService.fetchUser(2L)).thenThrow(new NotFoundException(ErrorMessages.USER_NOT_FOUND));
        assertThrows(NotFoundException.class, () ->
                service.startChat(1L, 2L, new MessageRequest("Hello!"))
        );
    }

    @Test
    void startChatUnexistentArtwork() {
        when(artworkService.fetchArtwork(1L)).thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));
        assertThrows(NotFoundException.class, () -> service.startChat(1L, 2L, new MessageRequest("Hello!")));
    }

    @Test
    void startChatWithYourself() {
        when(userService.fetchUser(1L)).thenReturn(artist);
        when(artworkService.fetchArtwork(1L)).thenReturn(artwork);
        assertThrows(BadRequestException.class, () -> service.startChat(1L, 1L, new MessageRequest("Hello!")));
    }

    @Test
    void startChatThatAlreadyExists() {
        var messageRequest = new MessageRequest("Hello!");
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(userService.fetchUser(2L)).thenReturn(buyer);
        when(artworkService.fetchArtwork(1L)).thenReturn(artwork);
        when(chatRepository.findByArtworkIdAndBuyerId(1L, 2L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message savedMessage = invocation.getArgument(0);
            savedMessage.setId(1L);
            savedMessage.setCreatedAt(createdAt);
            return savedMessage;
        });

        var result = service.startChat(1L, 2L, messageRequest);
        var messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        var savedMessage = messageCaptor.getValue();
        verify(chatRepository, never()).save(any());

        var expected = ChatSummaryResponse.from(chat, savedMessage, buyer.getId());
        assertEquals(buyer, savedMessage.getSender());
        assertEquals("Hello!", savedMessage.getContent());
        assertEquals(chat, savedMessage.getChat());
        assertEquals(expected, result);
    }

    @Test
    void getAllChatsByParticipantWhenParticipantIsBuyer() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        var lastMessage = new Message(1L, chat, buyer, "Hello!", false, createdAt);

        when(chatRepository.findByParticipantId(eq(2L), any())).thenReturn(new SliceImpl<>(List.of(chat)));
        when(messageRepository.findFirstByChatIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(lastMessage));

        var result = service.getAllChatsByParticipant(2L, PageRequest.of(0, 20));

        var expected = ChatSummaryResponse.from(chat, lastMessage, 2L);
        assertEquals(List.of(expected), result.getContent());
    }

    @Test
    void getAllChatsByParticipantWhenParticipantIsArtist() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        var lastMessage = new Message(1L, chat, buyer, "Hello!", false, createdAt);

        when(chatRepository.findByParticipantId(eq(1L), any())).thenReturn(new SliceImpl<>(List.of(chat)));
        when(messageRepository.findFirstByChatIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(lastMessage));

        var result = service.getAllChatsByParticipant(1L, PageRequest.of(0, 20));

        var expected = ChatSummaryResponse.from(chat, lastMessage, 1L);
        assertEquals(List.of(expected), result.getContent());
    }

    @Test
    void getAllChatsByParticipantWhenNoneExist() {
        when(chatRepository.findByParticipantId(eq(2L), any())).thenReturn(new SliceImpl<>(List.of()));

        var result = service.getAllChatsByParticipant(2L, PageRequest.of(0, 20));

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getMessagesAsBuyer() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        var message = new Message(1L, chat, buyer, "Hello!", false, createdAt);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(new SliceImpl<>(List.of(message)));

        var result = service.getMessages(1L, 2L, PageRequest.of(0, 20));

        assertEquals(List.of(MessageResponse.from(message)), result.getContent());
    }

    @Test
    void getMessagesAsArtist() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        var message = new Message(1L, chat, buyer, "Hello!", false, createdAt);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderByCreatedAtDesc(eq(1L), any())).thenReturn(new SliceImpl<>(List.of(message)));

        var result = service.getMessages(1L, 1L, PageRequest.of(0, 20));

        assertEquals(List.of(MessageResponse.from(message)), result.getContent());
    }

    @Test
    void getMessagesUnexistentChat() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getMessages(1L, 2L, PageRequest.of(0, 20)));
    }

    @Test
    void getMessagesNotParticipant() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(ForbiddenException.class, () -> service.getMessages(1L, 99L, PageRequest.of(0, 20)));
    }

    @Test
    void sendMessageAsBuyer() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message savedMessage = invocation.getArgument(0);
            savedMessage.setId(1L);
            savedMessage.setCreatedAt(createdAt);
            return savedMessage;
        });

        var result = service.sendMessage(1L, 2L, new MessageRequest("Hi there"));

        var messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        var savedMessage = messageCaptor.getValue();

        assertEquals(buyer, savedMessage.getSender());
        assertEquals("Hi there", savedMessage.getContent());
        assertEquals(chat, savedMessage.getChat());
        assertEquals(new MessageResponse(1L, buyer.getId(), "Hi there", false, createdAt), result);
    }

    @Test
    void sendMessageAsArtist() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message savedMessage = invocation.getArgument(0);
            savedMessage.setId(1L);
            savedMessage.setCreatedAt(createdAt);
            return savedMessage;
        });

        var result = service.sendMessage(1L, 1L, new MessageRequest("Hi there"));

        var messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        var savedMessage = messageCaptor.getValue();

        assertEquals(artist, savedMessage.getSender());
        assertEquals("Hi there", savedMessage.getContent());
        assertEquals(chat, savedMessage.getChat());
        assertEquals(new MessageResponse(1L, artist.getId(), "Hi there", false, createdAt), result);
    }

    @Test
    void sendMessageUnexistentChat() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.sendMessage(1L, 2L, new MessageRequest("Hi there")));
    }

    @Test
    void sendMessageNotParticipant() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(ForbiddenException.class, () -> service.sendMessage(1L, 99L, new MessageRequest("Hi there")));
        verify(messageRepository, never()).save(any());
    }

    @Test
    void markReadAsBuyer() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        service.markRead(1L, 2L);

        verify(messageRepository).markAllAsRead(1L, 2L);
    }

    @Test
    void markReadAsArtist() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        service.markRead(1L, 1L);

        verify(messageRepository).markAllAsRead(1L, 1L);
    }

    @Test
    void markReadUnexistentChat() {
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.markRead(1L, 2L));
        verify(messageRepository, never()).markAllAsRead(anyLong(), anyLong());
    }

    @Test
    void markReadNotParticipant() {
        var chat = new Chat(1L, artwork, buyer, createdAt, createdAt, true);
        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(ForbiddenException.class, () -> service.markRead(1L, 99L));
        verify(messageRepository, never()).markAllAsRead(anyLong(), anyLong());
    }

}