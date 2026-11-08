package io.github.kwatera_project.kwatera.review_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "review_summaries")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ReviewSummary {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "property_id")
  private UUID propertyId;

  @Column(name = "unit_id")
  private UUID unitId;

  @Column(name = "ai_summary", nullable = false, columnDefinition = "TEXT")
  private String aiSummary;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "review_ids", nullable = false)
  private List<UUID> reviewIds = new ArrayList<>();

  public List<UUID> getReviewIds() {
    return List.copyOf(reviewIds);
  }

  public void setReviewIds(List<UUID> reviewIds) {
    this.reviewIds = new ArrayList<>(List.copyOf(reviewIds));
  }

  @Column(name = "input_fingerprint", nullable = false)
  private String inputFingerprint;

  @Column(name = "generation_timestamp", nullable = false)
  private Instant generationTimestamp;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdAt;

  @Column(name = "updated_at")
  @LastModifiedDate
  private Instant updatedAt;
}
