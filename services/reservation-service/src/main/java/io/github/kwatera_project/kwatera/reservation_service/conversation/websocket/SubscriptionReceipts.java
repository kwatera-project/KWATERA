package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.stereotype.Component;

/** The simple broker has no receipts; acknowledge only subscriptions it has actually installed. */
@Component
public class SubscriptionReceipts implements ExecutorChannelInterceptor {
  private final ObjectProvider<MessageChannel> outbound;

  public SubscriptionReceipts(
      @Qualifier("clientOutboundChannel") ObjectProvider<MessageChannel> outbound) {
    this.outbound = outbound;
  }

  @Override
  public void afterMessageHandled(
      Message<?> message, MessageChannel channel, MessageHandler handler, Exception failure) {
    var request = StompHeaderAccessor.wrap(message);
    String receipt = request.getReceipt();
    if (failure == null
        && handler instanceof SimpleBrokerMessageHandler
        && request.getCommand() == StompCommand.SUBSCRIBE
        && receipt != null
        && receipt.length() <= 256) {
      var response = StompHeaderAccessor.create(StompCommand.RECEIPT);
      response.setSessionId(request.getSessionId());
      response.setReceiptId(receipt);
      outbound
          .getObject()
          .send(MessageBuilder.createMessage(new byte[0], response.getMessageHeaders()));
    }
  }
}
