package io.github.kwatera_project.kwatera.billing_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Table(name = "media_reading_upload_attempts")
public class MediaReadingUploadAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "media_reading_id", nullable = false)
  private UUID mediaReadingId;

  @Column(name = "meter_image", columnDefinition = "bytea")
  private byte[] meterImage;

  @Column(name = "ocr_value", length = 50)
  private String ocrValue;

  @Column(name = "confidence_score", precision = 5, scale = 4)
  private BigDecimal confidenceScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ReadingStatus status;

  @Column(name = "attempted_at", nullable = false, updatable = false)
  @CreatedDate
  private Instant attemptedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "reading_type", nullable = false, length = 20)
  private ReadingType readingType; // INITIAL / FINAL
}
