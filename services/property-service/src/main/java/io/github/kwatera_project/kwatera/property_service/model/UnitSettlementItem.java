package io.github.kwatera_project.kwatera.property_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unit_settlement_items")
@Getter
@Setter
@NoArgsConstructor
public class UnitSettlementItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(name = "settlement_item_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private SettlementItemType settlementItemType;

  @Column(name = "price_per_unit", nullable = false)
  private BigDecimal pricePerUnit;

  @Column(name = "measurement_unit")
  @Enumerated(EnumType.STRING)
  private MeasurementUnit measurementUnit;

  @Column(name = "billing_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private BillingType billingType;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
