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

  @Test
  void testRegisterRequest() {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("john");
    request.setEmail("john@example.com");
    request.setRole(Role.OWNER);
    request.setPassword("pass");
    request.setFirstName("John");
    request.setLastName("Doe");

    assertThat(request.getUsername()).isEqualTo("john");
    assertThat(request.getEmail()).isEqualTo("john@example.com");
    assertThat(request.getRole()).isEqualTo(Role.OWNER);
    assertThat(request.getPassword()).isEqualTo("pass");
    assertThat(request.getFirstName()).isEqualTo("John");
    assertThat(request.getLastName()).isEqualTo("Doe");
  }
}
