package io.github.kwatera_project.kwatera.billing_service.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.FailedTransactionCommand;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.TransactionStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

  @Mock private PaymentTransactionRepository repository;

  @InjectMocks private PaymentTransactionService service;

  @Test
  void shouldSaveSuccessTransaction() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(repository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.saveSuccessTransaction(
        settlementId,
        unitId,
        SettlementItemType.ACCOMMODATION,
        "desc",
        BigDecimal.valueOf(2),
        BigDecimal.valueOf(100),
        "stripe-session-1");

    verify(repository)
        .save(
            argThat(
                tx ->
                    tx.getSettlementId().equals(settlementId)
                        && tx.getUnitId().equals(unitId)
                        && tx.getStatus() == TransactionStatus.SUCCESS
                        && tx.getType() == SettlementItemType.ACCOMMODATION
                        && tx.getAmount().equals(BigDecimal.valueOf(200))
                        && tx.getStripeSessionId().equals("stripe-session-1")
                        && tx.getFailureReason() == null));
  }

  @Test
  void shouldSaveFailedTransaction() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(repository.save(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

    FailedTransactionCommand command =
        new FailedTransactionCommand(
            settlementId,
            unitId,
            SettlementItemType.DEPOSIT,
            "desc",
            BigDecimal.valueOf(3),
            BigDecimal.valueOf(50),
            "stripe-session-2",
            "Card declined");

    service.saveFailedTransaction(command);

    verify(repository)
        .save(
            argThat(
                tx ->
                    tx.getSettlementId().equals(settlementId)
                        && tx.getUnitId().equals(unitId)
                        && tx.getStatus() == TransactionStatus.FAILED
                        && tx.getType() == SettlementItemType.DEPOSIT
                        && tx.getAmount().equals(BigDecimal.valueOf(150))
                        && tx.getStripeSessionId().equals("stripe-session-2")
                        && tx.getFailureReason().equals("Card declined")));
  }
}
