package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.dto.FailedTransactionCommand;
import io.github.kwatera_project.kwatera.billing_service.model.PaymentTransaction;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.TransactionStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

  private final PaymentTransactionRepository repository;

  public void saveSuccessTransaction(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String stripeSessionId) {

    PaymentTransaction tx = new PaymentTransaction();

    tx.setSettlementId(settlementId);
    tx.setUnitId(unitId);
    tx.setStatus(TransactionStatus.SUCCESS);
    tx.setType(type);
    tx.setDescription(description);
    tx.setQuantity(quantity);
    tx.setUnitPrice(unitPrice);
    tx.setAmount(quantity.multiply(unitPrice));
    tx.setStripeSessionId(stripeSessionId);
    tx.setFailureReason(null);

    repository.save(tx);
  }

  public void saveFailedTransaction(FailedTransactionCommand cmd) {

    PaymentTransaction tx = new PaymentTransaction();

    tx.setSettlementId(cmd.settlementId());
    tx.setUnitId(cmd.unitId());
    tx.setStatus(TransactionStatus.FAILED);
    tx.setType(cmd.type());
    tx.setDescription(cmd.description());
    tx.setQuantity(cmd.quantity());
    tx.setUnitPrice(cmd.unitPrice());
    tx.setAmount(cmd.quantity().multiply(cmd.unitPrice()));
    tx.setStripeSessionId(cmd.stripeSessionId());
    tx.setFailureReason(cmd.failureReason());

    repository.save(tx);
  }
}
