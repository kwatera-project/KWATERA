package io.github.kwatera_project.kwatera.auth_service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoTest {

  @Test
  void testUserProfileDto() {
    UUID id = UUID.randomUUID();
    UserProfileDto dto =
        new UserProfileDto(id, "username", "John", "Doe", "john@example.com", Role.GUEST);

    assertThat(dto.id()).isEqualTo(id);
    assertThat(dto.username()).isEqualTo("username");
    assertThat(dto.firstName()).isEqualTo("John");
    assertThat(dto.lastName()).isEqualTo("Doe");
    assertThat(dto.email()).isEqualTo("john@example.com");
    assertThat(dto.role()).isEqualTo(Role.GUEST);
  }

  @Test
  void testUserProfileUpdateDto() {
    UserProfileUpdateDto dto = new UserProfileUpdateDto("John", "Doe");

    assertThat(dto.firstName()).isEqualTo("John");
    assertThat(dto.lastName()).isEqualTo("Doe");
  }
}
