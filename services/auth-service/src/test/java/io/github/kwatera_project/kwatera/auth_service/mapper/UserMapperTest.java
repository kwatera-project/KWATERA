package io.github.kwatera_project.kwatera.auth_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileDto;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private final UserMapper userMapper = new UserMapper();

  @Test
  void shouldMapUserToUserProfileDto() {
    UUID id = UUID.randomUUID();
    User user = new User();
    user.setId(id);
    user.setUsername("john");
    user.setEmail("john@example.com");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setRole(Role.GUEST);

    UserProfileDto dto = userMapper.toUserProfileDto(user);

    assertThat(dto).isNotNull();
    assertThat(dto.username()).isEqualTo("john");
    assertThat(dto.firstName()).isEqualTo("John");
    assertThat(dto.lastName()).isEqualTo("Doe");
    assertThat(dto.email()).isEqualTo("john@example.com");
    assertThat(dto.role()).isEqualTo(Role.GUEST);
  }
}
