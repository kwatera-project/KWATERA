package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.billing_service.dto.FailedTransactionCommand;
import io.github.kwatera_project.kwatera.billing_service.model.PaymentTransaction;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.TransactionStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

  @Mock private PaymentTransactionRepository repository;

  @InjectMocks private PaymentTransactionService service;

  @Test
  void shouldCreateProcessingTransactionWhenNotExists() {

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.empty());

    boolean result =
        service.createProcessingIfNotExists(
            "evt_1",
            UUID.randomUUID(),
            UUID.randomUUID(),
            SettlementItemType.ACCOMMODATION,
            "desc",
            BigDecimal.ONE,
            BigDecimal.TEN,
            "sess_1");

    assertTrue(result);
    verify(repository, times(1)).save(any(PaymentTransaction.class));
  }

  @Test
  void shouldNotCreateWhenEventAlreadyExists() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    boolean result =
        service.createProcessingIfNotExists(
            "evt_1",
            UUID.randomUUID(),
            UUID.randomUUID(),
            SettlementItemType.ACCOMMODATION,
            "desc",
            BigDecimal.ONE,
            BigDecimal.TEN,
            "sess_1");

    assertFalse(result);
    verify(repository, never()).save(any());
  }

  @Test
  void shouldNotOverrideSuccessStatus() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStatus(TransactionStatus.SUCCESS);

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    service.markFailedIfAllowed("evt_1", "error");

    assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
  }

  @Test
  void shouldMarkTransactionAsSuccess_whenProcessing() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");
    tx.setStatus(TransactionStatus.PROCESSING);

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    service.markSuccessIfAllowed("evt_1");

    assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
    verify(repository).save(tx);
  }

  @Test
  void shouldNotChange_whenAlreadySuccess() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");
    tx.setStatus(TransactionStatus.SUCCESS);

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    service.markSuccessIfAllowed("evt_1");

    verify(repository, never()).save(any());
    assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
  }

  @Test
  void shouldConvertFailedToSuccess() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");
    tx.setStatus(TransactionStatus.FAILED);

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    service.markSuccessIfAllowed("evt_1");

    assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
    verify(repository).save(tx);
  }

  @Test
  void shouldThrowWhenNotFound() {

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, () -> service.markSuccessIfAllowed("evt_1"));
  }

  @Test
  void shouldCreateNewFailedTransaction_whenNotExists() {

    FailedTransactionCommand command =
        new FailedTransactionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            SettlementItemType.ACCOMMODATION,
            "desc",
            BigDecimal.ONE,
            BigDecimal.TEN,
            "sess_1",
            "payment failed");

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.empty());

    when(repository.save(any(PaymentTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.markFailed("evt_1", command);

    verify(repository)
        .save(
            argThat(
                tx ->
                    tx.getStripeEventId().equals("evt_1")
                        && tx.getStatus() == TransactionStatus.FAILED
                        && tx.getFailureReason().equals("payment failed")));
  }

  @Test
  void shouldUpdateExistingTransactionToFailed_whenNotSuccess() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");
    tx.setStatus(TransactionStatus.PROCESSING);

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    FailedTransactionCommand command =
        new FailedTransactionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            SettlementItemType.ACCOMMODATION,
            "desc",
            BigDecimal.ONE,
            BigDecimal.TEN,
            "sess_1",
            "error");

    service.markFailed("evt_1", command);

    verify(repository).save(tx);

    assertEquals(TransactionStatus.FAILED, tx.getStatus());
    assertEquals("error", tx.getFailureReason());
  }

  @Test
  void shouldNotOverrideSuccessStatus_whenTransactionIsSuccess() {

    PaymentTransaction tx = new PaymentTransaction();
    tx.setStripeEventId("evt_1");
    tx.setStatus(TransactionStatus.SUCCESS);

    FailedTransactionCommand command =
        new FailedTransactionCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            SettlementItemType.ACCOMMODATION,
            "desc",
            BigDecimal.ONE,
            BigDecimal.TEN,
            "sess_1",
            "ignored error");

    when(repository.findByStripeEventId("evt_1")).thenReturn(Optional.of(tx));

    service.markFailed("evt_1", command);

    assertEquals(TransactionStatus.SUCCESS, tx.getStatus());
    assertNull(tx.getFailureReason());

    verify(repository, never()).save(tx);
  }
}
