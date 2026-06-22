package io.github.kwatera_project.kwatera.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Read-only newsletter property projection.
 *
 * <p>This entity maps the {@code properties} table owned by property-service. Auth-service has
 * SELECT-only access to this table for the purpose of building personalised newsletter
 * recommendations. Do not use this entity for any write operations; all property domain logic lives
 * in property-service.
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
public class Property {

  @Id private UUID id;

  @Column(nullable = false)
  private String title;

  private String description;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String country;
}
