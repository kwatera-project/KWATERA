package io.github.kwatera_project.kwatera.property_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unit_images")
@Getter
@Setter
@NoArgsConstructor
public class UnitImage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "unit_id", nullable = false)
  private UUID unitId;

  @Column(nullable = false)
  private String url;

  @Column(name = "is_main", nullable = false)
  private Boolean isMain;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;
}
