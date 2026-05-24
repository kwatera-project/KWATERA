package io.github.kwatera_project.kwatera.billing_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Check(constraints = "final_reading >= initial_reading")
@Table(name = "media_readings")
public class MediaReading {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "settlement_item_id", nullable = false)
  private UUID settlementItemId;

  @Enumerated(EnumType.STRING)
  @Column(name = "utility_type", nullable = false, length = 50)
  private UtilityType utilityType;

  @NotNull
  @Column(name = "initial_reading", precision = 12, scale = 6, nullable = false)
  @DecimalMin(value = "0.0")
  private BigDecimal initialReading;

  @Column(name = "initial_confidence_score", precision = 12, scale = 6, nullable = false)
  private BigDecimal initialConfidenceScore;

  @Column(name = "final_reading", precision = 12, scale = 6)
  @DecimalMin(value = "0.0")
  private BigDecimal finalReading;

  @Column(name = "final_confidence_score", precision = 12, scale = 6)
  private BigDecimal finalConfidenceScore;

  @Generated(event = EventType.INSERT)
  @Column(
      name = "consumption_difference",
      precision = 12,
      scale = 6,
      insertable = false,
      updatable = false,
      columnDefinition =
          "numeric(12,6) GENERATED ALWAYS AS (final_reading - initial_reading) STORED")
  private BigDecimal consumptionDifference;

  @NotNull
  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  @DecimalMin(value = "0.0")
  private BigDecimal unitPrice;

  @Generated(event = EventType.INSERT)
  @Column(
      name = "calculated_cost",
      precision = 12,
      scale = 2,
      insertable = false,
      updatable = false,
      columnDefinition =
          "numeric(12,2) GENERATED ALWAYS AS ((final_reading - initial_reading) * unit_price) STORED")
  private BigDecimal calculatedCost; // GENERATED ALWAYS AS (...) STORED

  @Enumerated(EnumType.STRING)
  @Column(name = "reading_source", nullable = false, length = 50)
  private ReadingSource readingSource = ReadingSource.OCR;

  @Enumerated(EnumType.STRING)
  @Column(name = "reading_status", nullable = false, length = 50)
  private ReadingStatus readingStatus = ReadingStatus.PENDING;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @LastModifiedDate
  private Instant updatedAt;
}
