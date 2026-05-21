package io.github.kwatera_project.kwatera.auth_service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void testUserDetailsMethods() {
    User user = new User();
    user.setRole(Role.GUEST);

    assertThat(user.getAuthorities()).hasSize(1);
    assertThat(user.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_GUEST");
    assertThat(user.isAccountNonExpired()).isTrue();
    assertThat(user.isAccountNonLocked()).isTrue();
    assertThat(user.isCredentialsNonExpired()).isTrue();
    assertThat(user.isEnabled()).isTrue();
  }

  @Test
  void testGettersAndSetters() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    User user = new User();

    user.setId(id);
    user.setUsername("john");
    user.setEmail("john@example.com");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setPassword("pass");
    user.setRole(Role.OWNER);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getUsername()).isEqualTo("john");
    assertThat(user.getEmail()).isEqualTo("john@example.com");
    assertThat(user.getFirstName()).isEqualTo("John");
    assertThat(user.getLastName()).isEqualTo("Doe");
    assertThat(user.getPassword()).isEqualTo("pass");
    assertThat(user.getRole()).isEqualTo(Role.OWNER);
    assertThat(user.getCreatedAt()).isEqualTo(now);
    assertThat(user.getUpdatedAt()).isEqualTo(now);
  }
}
