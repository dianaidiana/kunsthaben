package io.everyonecodes.project_module.chats;

import io.everyonecodes.project_module.artworks.Artwork;
import io.everyonecodes.project_module.artworks.dimensions.Dimensions;
import io.everyonecodes.project_module.artworks.dimensions.Frame;
import io.everyonecodes.project_module.auth.JwtService;
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
import io.everyonecodes.project_module.testsupport.AuthTestSupport;
import io.everyonecodes.project_module.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ChatControllerTest {

    @MockitoBean
    ChatService service;

    @Autowired
    RestTestClient client;

    @Autowired
    JwtService jwtService;

    @Value("${app.auth.cookie-name}")
    String cookieName;

    AuthTestSupport auth;

    @BeforeEach
    void setUpAuth() {
        auth = new AuthTestSupport(client, jwtService, cookieName);
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
        var messageRequest = new MessageRequest("Hello!");
        var chat = new Chat(1L, artwork, buyer, createdAt, true);
        var message = new Message(1L, chat, buyer, messageRequest.getContent(), false, createdAt);
        var expected = ChatSummaryResponse.from(chat, message, buyer.getId());
        when(service.startChat(artwork.getId(), buyer.getId(), messageRequest)).thenReturn(expected);

        ChatSummaryResponse response = auth.authenticated(
                                                   client.post()
                                                         .uri("/artwork/1/chat")
                                                         .contentType(MediaType.APPLICATION_JSON)
                                                         .body(messageRequest), 2L, "test@test.com")
                                           .exchange()
                                           .expectStatus().isCreated()
                                           .expectBody(ChatSummaryResponse.class)
                                           .returnResult()
                                           .getResponseBody();

        assertEquals(expected, response);
    }

    @Test
    void startChatWithYourself() {
        var messageRequest = new MessageRequest("Hello!");
        when(service.startChat(1L, 1L, messageRequest))
                .thenThrow(new BadRequestException(ErrorMessages.CANNOT_CHAT_WITH_SELF));

        auth.authenticated(
                client.post().uri("/artwork/1/chat").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                1L, "bob@ross.com")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void startChatUnexistentArtwork() {
        var messageRequest = new MessageRequest("Hello!");
        when(service.startChat(1L, 2L, messageRequest))
                .thenThrow(new NotFoundException(ErrorMessages.ARTWORK_NOT_FOUND));

        auth.authenticated(
                client.post().uri("/artwork/1/chat").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                2L, "test@test.com")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void getAllChatsByParticipantSuccessfully() {
        var chat = new Chat(1L, artwork, buyer, createdAt, true);
        var message = new Message(1L, chat, buyer, "Hello!", false, createdAt);
        when(service.getAllChatsByParticipant(eq(2L), any()))
                .thenReturn(new SliceImpl<>(List.of(ChatSummaryResponse.from(chat, message, buyer.getId()))));

        auth.authenticated(client.get().uri("/chat"), 2L, "test@test.com")
            .exchange()
            .expectStatus().isOk();

        verify(service).getAllChatsByParticipant(eq(2L), any());
    }

    @Test
    void getMessagesSuccessfully() {
        when(service.getMessages(eq(1L), eq(2L), any())).thenReturn(new SliceImpl<>(List.of()));

        auth.authenticated(client.get().uri("/chat/1/message"), 2L, "test@test.com")
            .exchange()
            .expectStatus().isOk();

        verify(service).getMessages(eq(1L), eq(2L), any());
    }

    @Test
    void getMessagesUnexistentChat() {
        when(service.getMessages(eq(1L), eq(2L), any()))
                .thenThrow(new NotFoundException(ErrorMessages.CHAT_NOT_FOUND));

        auth.authenticated(client.get().uri("/chat/1/message"), 2L, "test@test.com")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void getMessagesNotParticipant() {
        when(service.getMessages(eq(1L), eq(99L), any()))
                .thenThrow(new ForbiddenException(ErrorMessages.NOT_CHAT_PARTICIPANT));

        auth.authenticated(client.get().uri("/chat/1/message"), 99L, "someone-else@example.com")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void sendMessageSuccessfully() {
        var messageRequest = new MessageRequest("Hi there");
        var expected = new MessageResponse(1L, buyer.getId(), "Hi there", false, createdAt);
        when(service.sendMessage(1L, 2L, messageRequest)).thenReturn(expected);

        MessageResponse response = auth.authenticated(
                                                client.post().uri("/chat/1/message").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                                                2L, "test@test.com")
                                        .exchange()
                                        .expectStatus().isCreated()
                                        .expectBody(MessageResponse.class)
                                        .returnResult()
                                        .getResponseBody();

        assertEquals(expected, response);
    }

    @Test
    void sendMessageNotParticipant() {
        var messageRequest = new MessageRequest("Hi there");
        when(service.sendMessage(1L, 99L, messageRequest))
                .thenThrow(new ForbiddenException(ErrorMessages.NOT_CHAT_PARTICIPANT));

        auth.authenticated(
                client.post().uri("/chat/1/message").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                99L, "someone-else@example.com")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void sendMessageUnexistentChat() {
        var messageRequest = new MessageRequest("Hi there");
        when(service.sendMessage(1L, 2L, messageRequest))
                .thenThrow(new NotFoundException(ErrorMessages.CHAT_NOT_FOUND));

        auth.authenticated(
                client.post().uri("/chat/1/message").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                2L, "test@test.com")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void sendMessageBlankContent() {
        var messageRequest = new MessageRequest("");

        auth.authenticated(
                client.post().uri("/chat/1/message").contentType(MediaType.APPLICATION_JSON).body(messageRequest),
                2L, "test@test.com")
            .exchange()
            .expectStatus().isBadRequest();

        verify(service, never()).sendMessage(any(), any(), any());
    }

    @Test
    void markAsReadSuccessfully() {
        auth.authenticated(client.patch().uri("/chat/1/message/read"), 2L, "test@test.com")
            .exchange()
            .expectStatus().isNoContent();

        verify(service).markRead(1L, 2L);
    }

    @Test
    void markAsReadUnexistentChat() {
        doThrow(new NotFoundException(ErrorMessages.CHAT_NOT_FOUND)).when(service).markRead(1L, 2L);

        auth.authenticated(client.patch().uri("/chat/1/message/read"), 2L, "test@test.com")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void markAsReadNotParticipant() {
        doThrow(new ForbiddenException(ErrorMessages.NOT_CHAT_PARTICIPANT)).when(service).markRead(1L, 99L);

        auth.authenticated(client.patch().uri("/chat/1/message/read"), 99L, "someone-else@example.com")
            .exchange()
            .expectStatus().isForbidden();
    }

}