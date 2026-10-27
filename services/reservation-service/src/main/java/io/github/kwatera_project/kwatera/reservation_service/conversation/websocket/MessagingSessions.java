package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationService;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Owns identity for a transport session. Native headers can never replace it after CONNECT. */
@Component
public class MessagingSessions implements ChannelInterceptor {
  private static final String UUID_PATH =
      "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";
  private static final Pattern SEND =
      Pattern.compile("/app/conversations/" + UUID_PATH + "/messages");
  private static final Pattern SUBSCRIBE =
      Pattern.compile("/user/queue/conversations/" + UUID_PATH + "/messages");
  private final Clock clock;

  @Autowired
  public MessagingSessions(JwtService jwt, ConversationService conversations) {
    this(jwt, conversations, Clock.systemUTC());
  }

  MessagingSessions(JwtService jwt, ConversationService conversations, Clock clock) {
    this.jwt = jwt;
    this.conversations = conversations;
    this.clock = clock;
  }

  private final JwtService jwt;
  private final ConversationService conversations;
  private final ConcurrentHashMap<String, SessionIdentity> sessions = new ConcurrentHashMap<>();

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor headers = requireHeaders(message);
    List<String> credentials = extractAndRemoveCredentials(headers);
    StompCommand command = headers.getCommand();
    if (command == StompCommand.CONNECT) {
      connect(headers, credentials);
      return message;
    }
    if (isHousekeeping(headers)) return message;
    SessionIdentity identity = requireSession(headers);
    return switch (command) {
      case SEND -> {
        validateSend(headers);
        yield message;
      }
      case SUBSCRIBE -> {
        authorizeSubscription(headers, identity);
        yield message;
      }
      case null, default -> throw new MessagingFailure("INVALID_DESTINATION");
    };
  }

  private StompHeaderAccessor requireHeaders(Message<?> message) {
    StompHeaderAccessor headers =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (headers == null) throw new MessagingFailure("INVALID_DESTINATION");
    return headers;
  }

  private List<String> extractAndRemoveCredentials(StompHeaderAccessor headers) {
    List<String> credentials = headers.getNativeHeader("Authorization");
    // Spring logs rejected frames. Remove credentials before any operation can throw.
    if (credentials != null) headers.removeNativeHeader("Authorization");
    return credentials;
  }

  private void connect(StompHeaderAccessor headers, List<String> credentials) {
    SessionIdentity identity = authenticate(credentials);
    if (sessions.putIfAbsent(headers.getSessionId(), identity) != null)
      throw new MessagingFailure("UNAUTHENTICATED");
    headers.setUser(identity.authentication());
  }

  private boolean isHousekeeping(StompHeaderAccessor headers) {
    StompCommand command = headers.getCommand();
    // Housekeeping cannot carry application data or create subscriptions.
    return command == StompCommand.DISCONNECT
        || command == StompCommand.UNSUBSCRIBE
        || (command == null && headers.getMessageType() == SimpMessageType.HEARTBEAT);
  }

  private SessionIdentity requireSession(StompHeaderAccessor headers) {
    SessionIdentity identity = sessions.get(headers.getSessionId());
    if (identity == null || !identity.expiresAt().isAfter(clock.instant()))
      throw new MessagingFailure("UNAUTHENTICATED");
    headers.setUser(identity.authentication());
    return identity;
  }

  private void validateSend(StompHeaderAccessor headers) {
    requireDestination(SEND, headers.getDestination());
    if (headers.getContentType() == null
        || !MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(headers.getContentType())) {
      throw new MessagingFailure("INVALID_MESSAGE");
    }
  }

  private void authorizeSubscription(StompHeaderAccessor headers, SessionIdentity identity) {
    if (!"/user/queue/messaging-errors".equals(headers.getDestination())) {
      UUID conversationId = requireDestination(SUBSCRIBE, headers.getDestination());
      conversations.get(conversationId, UUID.fromString(identity.authentication().getName()));
    }
  }

  private SessionIdentity authenticate(List<String> values) {
    try {
      if (values == null || values.size() != 1 || !values.getFirst().startsWith("Bearer "))
        throw new IllegalArgumentException();
      var claims = jwt.extractAllClaims(values.getFirst().substring(7));
      Instant expiration = claims.getExpiration().toInstant();
      if (!expiration.isAfter(clock.instant())) throw new IllegalArgumentException();
      String userId = UUID.fromString(claims.get("userId", String.class)).toString();
      var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
      authentication.setDetails(userId);
      return new SessionIdentity(authentication, expiration);
    } catch (RuntimeException _) {
      throw new MessagingFailure("UNAUTHENTICATED");
    }
  }

  private UUID requireDestination(Pattern pattern, String destination) {
    var matcher = pattern.matcher(destination == null ? "" : destination);
    if (!matcher.matches()) throw new MessagingFailure("INVALID_DESTINATION");
    return UUID.fromString(matcher.group(1));
  }

  /** Applied at the final outbound channel, including deliveries to already subscribed sessions. */
  public boolean isAuthenticated(String sessionId) {
    SessionIdentity identity = sessionId == null ? null : sessions.get(sessionId);
    return identity != null && identity.expiresAt().isAfter(clock.instant());
  }

  public ChannelInterceptor outbound() {
    return new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var headers = StompHeaderAccessor.wrap(message);
        if (headers.getMessageType() != SimpMessageType.MESSAGE) return message;
        return isAuthenticated(headers.getSessionId()) ? message : null;
      }
    };
  }

  @EventListener
  public void disconnected(SessionDisconnectEvent event) {
    sessions.remove(event.getSessionId());
  }

  private record SessionIdentity(
      UsernamePasswordAuthenticationToken authentication, Instant expiresAt) {}
}
