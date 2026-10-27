package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public final class ConversationWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final MessagingSessions sessions;
  private final String[] origins;
  private final SubscriptionReceipts receipts;

  public ConversationWebSocketConfig(
      MessagingSessions sessions,
      SubscriptionReceipts receipts,
      @Value("${kwatera.messaging.allowed-origins:http://localhost:5173,http://localhost:5174}")
          String origins) {
    this.sessions = sessions;
    this.receipts = receipts;
    this.origins = Arrays.stream(origins.split(",", -1)).map(String::strip).toArray(String[]::new);
    if (this.origins.length == 0
        || Arrays.stream(this.origins)
            .anyMatch(origin -> origin.isBlank() || origin.contains("*"))) {
      throw new IllegalArgumentException("Explicit WebSocket origins required");
    }
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.setErrorHandler(
        new StompSubProtocolErrorHandler() {
          @Override
          public Message<byte[]> handleClientMessageProcessingError(
              Message<byte[]> clientMessage, Throwable failure) {
            var error =
                clientMessage != null
                        && !sessions.isAuthenticated(
                            org.springframework.messaging.simp.SimpMessageHeaderAccessor
                                .getSessionId(clientMessage.getHeaders()))
                    ? new MessagingError("UNAUTHENTICATED", "Valid credentials required; reconnect")
                    : MessagingError.from(failure);
            var headers = StompHeaderAccessor.create(StompCommand.ERROR);
            headers.setMessage(error.code());
            headers.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
            // Only fixed application codes/messages are included, never exception text or frame
            // headers.
            byte[] body =
                ("{\"code\":\"" + error.code() + "\",\"message\":\"" + error.message() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return MessageBuilder.createMessage(body, headers.getMessageHeaders());
          }
        });
    registry.addEndpoint("/ws/conversations").setAllowedOrigins(origins);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix("/user");
    registry.enableSimpleBroker("/queue");
    registry.configureBrokerChannel().interceptors(receipts);
    registry.setPreservePublishOrder(true);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(sessions);
    registration.taskExecutor().corePoolSize(4).maxPoolSize(8).queueCapacity(256);
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(sessions.outbound());
    registration.taskExecutor().corePoolSize(4).maxPoolSize(8).queueCapacity(512);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(
        handler ->
            new org.springframework.web.socket.handler.WebSocketHandlerDecorator(handler) {
              @Override
              public void afterConnectionEstablished(
                  org.springframework.web.socket.WebSocketSession session) throws Exception {
                // Bound native frames too, and allow 4000 characters even when JSON uses six-byte
                // escapes.
                session.setTextMessageSizeLimit(32768);
                session.setBinaryMessageSizeLimit(32768);
                super.afterConnectionEstablished(session);
              }
            });
    registration
        .setMessageSizeLimit(32768)
        .setSendBufferSizeLimit(131072)
        .setSendTimeLimit(10000)
        .setTimeToFirstMessage(10000);
  }
}
