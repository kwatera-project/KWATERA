package io.github.kwatera_project.kwatera.auth_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
