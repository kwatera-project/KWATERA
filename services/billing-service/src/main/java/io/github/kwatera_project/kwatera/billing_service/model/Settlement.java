package io.github.kwatera_project.kwatera.billing_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Settlement {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "reservation_id", nullable = false, unique = true)
  private UUID reservationId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private SettlementStatus status;

  @Column(name = "accommodation_amount", nullable = false)
  private BigDecimal accommodationAmount;

  @Column(name = "utilities_amount", nullable = false)
  private BigDecimal utilitiesAmount;

  @Column(name = "deposit_amount", nullable = false)
  private BigDecimal depositAmount;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "amount_paid", nullable = false)
  private BigDecimal amountPaid;

  @Column(name = "balance_due", nullable = false)
  private BigDecimal balanceDue;

  @Column(name = "issued_at")
  private Instant issuedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "invoice_requested", nullable = false)
  private boolean invoiceRequested;

  @Column(name = "invoice_pdf_path")
  private String invoicePdfPath;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "tax_id")
  private String taxId;

  @Column(name = "company_address")
  private String companyAddress;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @LastModifiedDate
  private Instant updatedAt;
}
