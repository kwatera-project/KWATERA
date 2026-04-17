package io.github.kwatera_project.kwatera.auth_service.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class RegisteredClientRepositoryTest {

  SecurityConfig config = new SecurityConfig();

  @Test
  void shouldContainGatewayClient() {
    RegisteredClientRepository repo = config.registeredClientRepository();

    var client = repo.findByClientId("gateway-client");

    assertThat(client).isNotNull();
    assertThat(client.getClientId()).isEqualTo("gateway-client");
    assertThat(client.getScopes()).contains("openid", "profile", "email");
  }
}
