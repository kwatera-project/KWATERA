package io.github.kwatera_project.kwatera.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest
class ApiGatewayApplicationTests {

  @MockBean private ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  void contextLoads() {}
}
