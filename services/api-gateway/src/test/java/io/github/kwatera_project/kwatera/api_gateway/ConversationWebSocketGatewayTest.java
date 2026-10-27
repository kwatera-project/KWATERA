package io.github.kwatera_project.kwatera.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/** Executes the production YAML route, discovery, load balancer and WebSocket upgrade filter. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.config.location=file:src/main/resources/application.yaml",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false"
    })
class ConversationWebSocketGatewayTest {
  private static final AtomicReference<String> PATH = new AtomicReference<>();
  private static final DisposableServer BACKEND =
      HttpServer.create()
          .port(0)
          .handle(
              (request, response) -> {
                PATH.set(request.uri());
                return response.sendWebsocket((in, out) -> out.send(in.receive().retain()));
              })
          .bindNow();
  @MockitoBean private ReactiveJwtDecoder decoder;

  @Value("${local.server.port}")
  private int port;

  @DynamicPropertySource
  static void discovery(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.discovery.client.simple.instances.reservation-service[0].uri",
        () -> "http://localhost:" + BACKEND.port());
  }

  @AfterAll
  static void stopBackend() {
    BACKEND.disposeNow();
  }

  @Test
  void productionLoadBalancedRoutePreservesPathAndExchangesWebSocketFrames() {
    var received = new AtomicReference<String>();
    String frame = "CONNECT\naccept-version:1.2\n\n\u0000";
    new ReactorNettyWebSocketClient()
        .execute(
            URI.create("ws://localhost:" + port + "/ws/conversations"),
            session ->
                session
                    .send(Mono.just(session.textMessage(frame)))
                    .then(
                        session
                            .receive()
                            .next()
                            .doOnNext(message -> received.set(message.getPayloadAsText()))
                            .then()))
        .block(Duration.ofSeconds(10));
    assertThat(PATH.get()).isEqualTo("/ws/conversations");
    assertThat(received.get()).isEqualTo(frame);
  }
}
