package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.dto.FailedTransactionCommand;
import io.github.kwatera_project.kwatera.billing_service.model.PaymentTransaction;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.TransactionStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

  private final PaymentTransactionRepository repository;

  @Transactional
  public boolean createProcessingIfNotExists(
      String stripeEventId,
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String stripeSessionId) {

    if (repository.findByStripeEventId(stripeEventId).isPresent()) {
      return false;
    }

    PaymentTransaction tx = new PaymentTransaction();

    tx.setStripeEventId(stripeEventId);
    tx.setSettlementId(settlementId);
    tx.setUnitId(unitId);
    tx.setStatus(TransactionStatus.PROCESSING);
    tx.setType(type);
    tx.setDescription(description);
    tx.setQuantity(quantity);
    tx.setUnitPrice(unitPrice);
    tx.setAmount(quantity.multiply(unitPrice));
    tx.setStripeSessionId(stripeSessionId);
    tx.setFailureReason(null);

    repository.save(tx);
    return true;
  }

  @Transactional
  public void markSuccessIfAllowed(String stripeEventId) {

    PaymentTransaction tx =
        repository
            .findByStripeEventId(stripeEventId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

    if (tx.getStatus() == TransactionStatus.SUCCESS) {
      return; // idempotency guard
    }

    tx.setStatus(TransactionStatus.SUCCESS);
    repository.save(tx);
  }

  @Transactional
  public void markFailedIfAllowed(String stripeEventId, String reason) {

    PaymentTransaction tx =
        repository
            .findByStripeEventId(stripeEventId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

    if (tx.getStatus() == TransactionStatus.FAILED || tx.getStatus() == TransactionStatus.SUCCESS) {
      return; // idempotency guard
    }

    tx.setStatus(TransactionStatus.FAILED);
    tx.setFailureReason(reason);

    repository.save(tx);
  }

  @Transactional
  public void markFailed(String stripeEventId, FailedTransactionCommand command) {

    Optional<PaymentTransaction> existing = repository.findByStripeEventId(stripeEventId);

    if (existing.isPresent()) {

      PaymentTransaction tx = existing.get();

      if (tx.getStatus() == TransactionStatus.SUCCESS) {
        return;
      }

      tx.setStatus(TransactionStatus.FAILED);
      tx.setFailureReason(command.failureReason());

      repository.save(tx);
      return;
    }

    PaymentTransaction newTx = new PaymentTransaction();

    newTx.setStripeEventId(stripeEventId);
    newTx.setSettlementId(command.settlementId());
    newTx.setUnitId(command.unitId());
    newTx.setType(command.type());
    newTx.setDescription(command.description());
    newTx.setQuantity(command.quantity());
    newTx.setUnitPrice(command.unitPrice());
    newTx.setAmount(command.quantity().multiply(command.unitPrice()));
    newTx.setStripeSessionId(command.stripeSessionId());
    newTx.setStatus(TransactionStatus.FAILED);
    newTx.setFailureReason(command.failureReason());

    repository.save(newTx);
  }
}
