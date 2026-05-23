package io.github.kwatera_project.kwatera.billing_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Getter
@Setter
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

  @Id @GeneratedValue private UUID id;

  @Column(name = "settlement_id", nullable = false)
  private UUID settlementId;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TransactionStatus status;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private SettlementItemType type;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private BigDecimal quantity;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "stripe_session_id", nullable = false, unique = true)
  private String stripeSessionId;

  @Column private String failureReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt = Instant.now();

  @Column(name = "stripe_event_id", nullable = false, unique = true)
  private String stripeEventId;
}
