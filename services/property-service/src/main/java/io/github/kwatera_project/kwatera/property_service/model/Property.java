package io.github.kwatera_project.kwatera.property_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Property {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  private String description;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

  @Column(nullable = false)
  private String location;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate // The field is set automatically when the record is created and cannot be changed
  // later.
  private Instant createdAt;

  @Column(name = "updated_at")
  @LastModifiedDate // Field updated automatically whenever a record changes.
  private Instant updatedAt;
}
