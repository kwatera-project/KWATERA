package io.github.kwatera_project.kwatera.reservation_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigTest {

  @Test
  void shouldConfigureFiniteConnectionAndReadTimeouts() {
    RestTemplate restTemplate =
        new RestTemplateConfig()
            .restTemplate(new ObjectMapper(), Duration.ofSeconds(2), Duration.ofSeconds(5));

    assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
    assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "connectTimeout"))
        .isEqualTo(2_000);
    assertThat(ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "readTimeout"))
        .isEqualTo(5_000);
  }
}
