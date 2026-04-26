package io.github.kwatera_project.kwatera.auth_service.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity // Mapping to a table in the DB.
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(
    AuditingEntityListener.class) // Enables auto-completion of createdAt, updatedAt fields.
public class User implements UserDetails {
  /// A class representing the user table in the database.
  @Id // Master key.
  @GeneratedValue(strategy = GenerationType.UUID) // Automatically generates UUID.
  private UUID id;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING) // Saved in the database as text.
  private Role role;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreatedDate // The field is set automatically when the record is created and cannot be changed
  // later.
  private Instant createdAt;

  @Column(name = "updated_at")
  @LastModifiedDate // Field updated automatically whenever a record changes.
  private Instant updatedAt;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
