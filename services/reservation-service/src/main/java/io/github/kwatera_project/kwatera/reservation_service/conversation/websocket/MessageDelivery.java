package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageDelivery {
  private final SimpMessagingTemplate messaging;

  @TransactionalEventListener
  public void afterCommit(MessageCreated event) {
    Stream.of(event.guestUserId(), event.ownerUserId())
        .distinct()
        .forEach(
            user -> {
              try {
                messaging.convertAndSendToUser(
                    user.toString(),
                    "/queue/conversations/" + event.message().conversationId() + "/messages",
                    event.message());
              } catch (RuntimeException failure) {
                // The transaction has committed. History is the recovery mechanism; never log
                // content.
                log.warn("Live delivery failed for persisted message {}", event.message().id());
              }
            });
  }
}
