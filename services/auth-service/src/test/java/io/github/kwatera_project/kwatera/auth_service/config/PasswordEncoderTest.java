package io.github.kwatera_project.kwatera.auth_service.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

  SecurityConfig config = new SecurityConfig();

  @Test
  void shouldEncodeAndMatchPassword() {
    PasswordEncoder encoder = config.passwordEncoder();

    String raw = "secret";

    String encoded = encoder.encode(raw);

    assertThat(encoded).isNotEqualTo(raw);
    assertThat(encoder.matches(raw, encoded)).isTrue();
  }
}
