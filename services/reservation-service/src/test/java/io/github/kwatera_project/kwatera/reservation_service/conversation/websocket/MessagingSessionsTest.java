package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationAuthenticationService;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationService;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class MessagingSessionsTest {
  private static final String SECRET = "test-secret-with-at-least-32-bytes-for-hmac";
  private final UUID user = UUID.randomUUID();
  private final UUID conversation = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-10-27T12:00:00Z");
  private final Clock clock = mock(Clock.class);
  private final ConversationService conversations = mock(ConversationService.class);
  private final MessageChannel channel = mock(MessageChannel.class);
  private MessagingSessions sessions;

  @BeforeEach
  void setup() {
    when(clock.instant()).thenReturn(now);
    // Real JWT verification uses wall clock. Session expiry uses an injectable clock.
    sessions = new MessagingSessions(new JwtService(SECRET), conversations, clock);
  }

  private Message<byte[]> frame(StompCommand command, String destination) {
    var h = StompHeaderAccessor.create(command);
    h.setSessionId("session");
    if (command == StompCommand.SEND)
      h.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
    if (destination != null) h.setDestination(destination);
    h.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
  }

  private void connect() {
    var message = frame(StompCommand.CONNECT, null);
    var h = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    h.setNativeHeader(
        "Authorization",
        "Bearer "
            + Jwts.builder()
                .subject("display-name")
                .claim("userId", user.toString())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact());
    sessions.preSend(message, channel);
    var auth = (Authentication) h.getUser();
    assertThat(auth.getName()).isEqualTo(user.toString());
    assertThat(new ConversationAuthenticationService().requireUserId(auth)).isEqualTo(user);
  }

  @Test
  void authenticatedUserCannotBeReplacedByLaterHeadersOrSecondConnect() {
    connect();
    var send = frame(StompCommand.SEND, "/app/conversations/" + conversation + "/messages");
    var h = MessageHeaderAccessor.getAccessor(send, StompHeaderAccessor.class);
    h.setNativeHeader("Authorization", "Bearer forged");
    h.setUser(() -> "impersonated");
    sessions.preSend(send, channel);
    assertThat(h.getUser().getName()).isEqualTo(user.toString());
    assertThatThrownBy(this::connect).isInstanceOf(MessagingFailure.class);
  }

  @Test
  void noSendOrSubscribeBeforeConnect() {
    for (var command : List.of(StompCommand.SEND, StompCommand.SUBSCRIBE))
      assertThatThrownBy(
              () -> sessions.preSend(frame(command, "/user/queue/messaging-errors"), channel))
          .isInstanceOf(MessagingFailure.class);
  }

  @Test
  void subscriptionChecksExactConversationAndHidesLifecycleFromStrangers() {
    connect();
    sessions.preSend(
        frame(StompCommand.SUBSCRIBE, "/user/queue/conversations/" + conversation + "/messages"),
        channel);
    verify(conversations).get(conversation, user);
    var other = UUID.randomUUID();
    when(conversations.get(other, user))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    assertThatThrownBy(
            () ->
                sessions.preSend(
                    frame(
                        StompCommand.SUBSCRIBE, "/user/queue/conversations/" + other + "/messages"),
                    channel))
        .isInstanceOf(ResponseStatusException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/queue/x",
        "/topic/x",
        "/user/someone/queue/x",
        "/user/queue/conversations/*/messages",
        "/user/queue/conversations/**",
        "/user/queue",
        "/user/queue/conversations/bad/messages",
        "/unknown",
        "/app/conversations/x/messages"
      })
  void rejectsAllNonContractSubscriptionsAndSends(String destination) {
    connect();
    for (var command : List.of(StompCommand.SEND, StompCommand.SUBSCRIBE))
      assertThatThrownBy(() -> sessions.preSend(frame(command, destination), channel))
          .isInstanceOf(MessagingFailure.class);
    verifyNoInteractions(conversations);
  }

  @Test
  void expirationStopsInboundAndAlreadySubscribedOutboundMessages() {
    connect();
    var outboundHeaders = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    outboundHeaders.setSessionId("session");
    var outbound = MessageBuilder.createMessage(new byte[0], outboundHeaders.getMessageHeaders());
    assertThat(sessions.outbound().preSend(outbound, channel)).isNotNull();
    when(clock.instant()).thenReturn(Instant.now().plusSeconds(1200));
    assertThat(sessions.outbound().preSend(outbound, channel)).isNull();
    for (var command : List.of(StompCommand.SEND, StompCommand.SUBSCRIBE))
      assertThatThrownBy(
              () -> sessions.preSend(frame(command, "/user/queue/messaging-errors"), channel))
          .isInstanceOf(MessagingFailure.class);
    sessions.preSend(frame(StompCommand.DISCONNECT, null), channel);
  }
}
