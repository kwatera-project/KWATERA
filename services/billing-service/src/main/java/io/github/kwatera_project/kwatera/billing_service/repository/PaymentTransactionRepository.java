package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.PaymentTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
  Optional<PaymentTransaction> findByStripeSessionId(String sessionId);

  Optional<PaymentTransaction> findByStripeEventId(String eventId);

  java.util.List<PaymentTransaction> findBySettlementId(UUID settlementId);
}
