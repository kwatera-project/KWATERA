package io.github.kwatera_project.kwatera.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest
class ApiGatewayApplicationTests {

  @MockitoBean private ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  void contextLoads() {}

  @Test
  @SuppressWarnings("unchecked")
  void shouldRouteConversationApiToReservationService() throws IOException {
    Map<String, Object> configuration;
    try (var input = Files.newInputStream(Path.of("src/main/resources/application.yaml"))) {
      configuration = new Yaml().load(input);
    }

    Map<String, Object> spring = (Map<String, Object>) configuration.get("spring");
    Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
    Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
    Map<String, Object> server = (Map<String, Object>) gateway.get("server");
    Map<String, Object> webflux = (Map<String, Object>) server.get("webflux");
    List<Map<String, Object>> routes = (List<Map<String, Object>>) webflux.get("routes");

    assertThat(routes)
        .anySatisfy(
            route -> {
              assertThat(route.get("id")).isEqualTo("reservation-service-conversations");
              assertThat(route.get("uri")).isEqualTo("lb://reservation-service");
              assertThat((List<String>) route.get("predicates"))
                  .containsExactly("Path=/api/v1/conversations,/api/v1/conversations/**");
            });
  }
}
