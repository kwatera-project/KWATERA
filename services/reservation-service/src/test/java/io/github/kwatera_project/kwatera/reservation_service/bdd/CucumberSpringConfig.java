package io.github.kwatera_project.kwatera.reservation_service.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.event.SettlementEventListener;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import io.github.kwatera_project.kwatera.reservation_service.service.EmailNotificationService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestOperations;

// Connect Cucumber with Spring Boot test context.
// Full Spring context with in-memory H2 (configured via src/test/resources/application.yaml).
// External infrastructure (Eureka, Config Server, Kafka, mail, property-service, NBP API)
// is either disabled through test properties or replaced with Mockito mocks.
@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      // Disable Spring Cloud service discovery and config server
      "spring.cloud.discovery.enabled=false",
      "eureka.client.enabled=false",
      "spring.cloud.config.enabled=false",
      // Point Kafka to a non-existing broker so the consumer never connects
      "spring.kafka.bootstrap-servers=localhost:19999",
      // Provide a dummy JWT secret so JwtService can initialise
      "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
      // Mail configuration – JavaMailSender bean is mocked, but properties must resolve
      "spring.mail.host=localhost",
      "spring.mail.port=1025",
      // Business zone used by BusinessDateProvider (mocked, but the property must parse)
      "kwatera.business-zone=Europe/Warsaw",
      "kwatera.mail.from=no-reply@kwatera.test",
      "kwatera.mail.test-recipient=test@kwatera.test",
      // Keep DDL in sync with the entity model for the H2 test database
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false",
      // Disable mail health indicator
      "management.health.mail.enabled=false",
      // Prevent Kafka listener containers from auto-starting at context startup
      "spring.kafka.listener.auto-startup=false"
    })
@AutoConfigureMockMvc
public class CucumberSpringConfig {

  // RestOperations is the interface implemented by the @LoadBalanced RestTemplate
  // that ReservationService uses for property-service calls.
  @MockitoBean protected RestOperations restOperations;

  // Prevents outbound calls to the NBP exchange-rate API.
  @MockitoBean protected NbpExchangeRateClient nbpExchangeRateClient;

  // Prevents actual SMTP/mail-sender calls.
  @MockitoBean protected EmailNotificationService emailNotificationService;

  // Prevents audit event persistence from interfering with test assertions.
  @MockitoBean protected SystemEventService systemEventService;

  // Satisfies JavaMailSender autowiring inside Spring Mail autoconfiguration.
  @MockitoBean protected JavaMailSender javaMailSender;

  // Fixes the "today" date so test dates are always deterministic and in the future.
  @MockitoBean protected BusinessDateProvider businessDateProvider;

  // Mocking the Kafka ConsumerFactory prevents a real TCP connection to kafka:9092.
  @MockitoBean protected ConsumerFactory<String, byte[]> consumerFactory;

  @MockitoBean
  protected ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory;

  // Mocking SettlementEventListener replaces the @KafkaListener-annotated bean.
  @MockitoBean protected SettlementEventListener settlementEventListener;
}
