package io.github.kwatera_project.kwatera.auth_service.service;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public User getUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "The user account has been deleted or inactivated"));
  }

  public void register(String username, String email, Role role, String password, String firstName, String lastName) {
    if (userRepository.findByUsername(username).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
    }

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setFirstName(firstName);
    user.setLastName(lastName);

    if (role == null) {
      user.setRole(Role.GUEST);
    } else if (role == Role.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign ADMIN role");
    } else {
      user.setRole(role);
    }

    userRepository.save(user);
  }

  public User updateProfile(String email, String firstName, String lastName) {
    User user = getUserByEmail(email);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    return userRepository.save(user);
  }
}
