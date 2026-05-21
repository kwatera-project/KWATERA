package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock UserRepository userRepository;

  @Mock PasswordEncoder passwordEncoder;

  @InjectMocks UserService userService;

  @Test
  void shouldReturnUserByEmail() {
    User user = new User();
    user.setEmail("test@mail.com");

    when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

    User result = userService.getUserByEmail("test@mail.com");

    assertThat(result.getEmail()).isEqualTo("test@mail.com");
  }

  @Test
  void shouldThrow404WhenUserNotFound() {
    when(userRepository.findByEmail("missing@mail.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserByEmail("missing@mail.com"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException e = (ResponseStatusException) ex;
              assertThat(e.getStatusCode().value()).isEqualTo(404);
            });
  }

  @Test
  void shouldRegisterUserSuccessfully() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

    when(passwordEncoder.encode("pass")).thenReturn("encoded-pass");

    userService.register("john", "john@mail.com", Role.OWNER, "pass", "John", "Doe");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());

    User saved = captor.getValue();

    assertThat(saved.getUsername()).isEqualTo("john");
    assertThat(saved.getEmail()).isEqualTo("john@mail.com");
    assertThat(saved.getPassword()).isEqualTo("encoded-pass");
    assertThat(saved.getRole()).isEqualTo(Role.OWNER);
    assertThat(saved.getFirstName()).isEqualTo("John");
    assertThat(saved.getLastName()).isEqualTo("Doe");
  }

  @Test
  void shouldSetGuestRoleWhenRoleIsNull() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

    when(passwordEncoder.encode(any())).thenReturn("encoded");

    userService.register("john", "john@mail.com", null, "pass", "John", "Doe");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());

    assertThat(captor.getValue().getRole()).isEqualTo(Role.GUEST);
    assertThat(captor.getValue().getFirstName()).isEqualTo("John");
    assertThat(captor.getValue().getLastName()).isEqualTo("Doe");
  }

  @Test
  void shouldThrow409WhenUsernameExists() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.register("john", "john@mail.com", Role.GUEST, "pass", "John", "Doe"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException e = (ResponseStatusException) ex;
              assertThat(e.getStatusCode().value()).isEqualTo(409);
            });

    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldRejectAdminRole() {
    when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.register("john", "john@mail.com", Role.ADMIN, "pass", "John", "Doe"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException e = (ResponseStatusException) ex;
              assertThat(e.getStatusCode().value()).isEqualTo(403);
            });

    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldUpdateUserProfileSuccessfully() {
    User user = new User();
    user.setEmail("john@mail.com");
    user.setFirstName("OldFirst");
    user.setLastName("OldLast");

    when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateProfile("john@mail.com", "NewFirst", "NewLast");

    assertThat(updated.getFirstName()).isEqualTo("NewFirst");
    assertThat(updated.getLastName()).isEqualTo("NewLast");
    verify(userRepository).findByEmail("john@mail.com");
    verify(userRepository).save(user);
  }
}
