package io.github.kwatera_project.kwatera.reservation_service.conversation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.client.PropertyOwnershipClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.ConversationRepository;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.MessageRepository;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationService;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.MessageService;
import io.github.kwatera_project.kwatera.reservation_service.event.SettlementEventListener;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import io.github.kwatera_project.kwatera.reservation_service.service.EmailNotificationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.cloud.discovery.enabled=false",
      "eureka.client.enabled=false",
      "spring.cloud.config.enabled=false",
      "spring.kafka.bootstrap-servers=localhost:19999",
      "spring.kafka.listener.auto-startup=false",
      "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
      "spring.mail.host=localhost",
      "spring.mail.port=1025",
      "management.health.mail.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false",
      "kwatera.business-zone=Europe/Warsaw"
    })
@AutoConfigureMockMvc
class StompMessagingIntegrationTest {

  private static final UUID GUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID UNIT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private ConversationService conversationService;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private MockMvc mockMvc;

  @MockitoBean private PropertyOwnershipClient propertyOwnershipClient;
  @MockitoBean private RestOperations restOperations;
  @MockitoBean private NbpExchangeRateClient nbpExchangeRateClient;
  @MockitoBean private EmailNotificationService emailNotificationService;
  @MockitoBean private SystemEventService systemEventService;
  @MockitoBean private JavaMailSender javaMailSender;
  @MockitoBean private BusinessDateProvider businessDateProvider;
  @MockitoBean private ConsumerFactory<String, byte[]> consumerFactory;

  @MockitoBean
  private ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory;

  @MockitoBean private SettlementEventListener settlementEventListener;

  @Autowired private MessageService messages;

  @Autowired private MessageRepository messageRepository;

  @Autowired private JdbcTemplate jdbc;
  private Reservation reservation;

  @BeforeEach
  void setUp() {
    messageRepository.deleteAll();
    conversationRepository.deleteAll();
    reservationRepository.deleteAll();
    reservation = reservationRepository.saveAndFlush(reservation());
    when(propertyOwnershipClient.getOwnerIdForUnit(UNIT_ID)).thenReturn(OWNER_ID);
    when(businessDateProvider.today()).thenReturn(LocalDate.of(2026, 12, 1));
    when(businessDateProvider.toLocalDate(org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenAnswer(
            invocation ->
                invocation
                    .getArgument(0, Instant.class)
                    .atZone(ZoneId.of("Europe/Warsaw"))
                    .toLocalDate());
  }

  @Value("${local.server.port}")
  private int port;

  @Autowired private ObjectMapper json;
  @Autowired private PlatformTransactionManager transactions;
  private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=";
  private final List<StompSession> connected = new ArrayList<>();
  private final List<WebSocketStompClient> clients = new ArrayList<>();
  private final java.util.Map<StompSession, CompletableFuture<StompHeaders>> protocolErrors =
      new java.util.IdentityHashMap<>();
  private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

  @AfterEach
  void cleanUp() {
    // assertAll runs every cleanup step in order, aggregating failures instead of aborting early.
    assertAll(
        () ->
            assertAll(
                connected.stream()
                    .<Executable>map(
                        session ->
                            () -> {
                              if (session.isConnected()) session.disconnect();
                            })),
        () -> assertAll(clients.stream().<Executable>map(client -> client::stop)),
        scheduler::shutdown,
        messageRepository::deleteAll);
  }

  @Test
  void participantsExchangePersistedMessagesAndRecoverThroughRest() throws Exception {
    UUID id = conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
    var guest = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
    var owner = connect(token(OWNER_ID, Instant.now().plusSeconds(600)));
    var stranger = connect(token(UUID.randomUUID(), Instant.now().plusSeconds(600)));
    var guestQueue = subscribe(guest, destination(id));
    var ownerQueue = subscribe(owner, destination(id));
    var strangerErrors = subscribe(stranger, "/user/queue/messaging-errors");
    for (var sender : List.of(guest, owner)) {
      send(sender, id, "{\"content\":\"  hello  \"}");
      var a = json.readValue(take(guestQueue), MessageResponse.class);
      var b = json.readValue(take(ownerQueue), MessageResponse.class);
      assertThat(a).isEqualTo(b);
      assertThat(a.senderUserId()).isEqualTo(sender == guest ? GUEST_ID : OWNER_ID);
      assertThat(a.content()).isEqualTo("hello");
      var stored = messageRepository.findById(a.id()).orElseThrow();
      assertThat(stored.getCreatedAt()).isEqualTo(a.createdAt());
      assertThat(stored.getSenderUserId()).isEqualTo(a.senderUserId());
      assertThat(stored.getContent()).isEqualTo(a.content());
      assertThat(stored.getConversation().getId()).isEqualTo(id);
    }
    assertThat(messageRepository.count()).isEqualTo(2);
    assertThat(strangerErrors).isEmpty();
    mockMvc
        .perform(get(path(id)).with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(path(id))
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST")))
                .contentType("application/json")
                .content("{\"content\":\"from REST\"}"))
        .andExpect(status().isCreated());
    assertThat(json.readValue(take(guestQueue), MessageResponse.class).content())
        .isEqualTo("from REST");
    take(ownerQueue);
    guest.disconnect();
    messages.send(id, OWNER_ID, "while disconnected");
    var reconnected = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
    subscribe(reconnected, destination(id));
    mockMvc
        .perform(get(path(id)).with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].content").value("while disconnected"));
  }

  @Test
  void recoverableErrorsArePrivateAndRollbackDoesNotDeliver() throws Exception {
    UUID id = conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
    var guest = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
    var owner = connect(token(OWNER_ID, Instant.now().plusSeconds(600)));
    var live = subscribe(guest, destination(id));
    var errors = subscribe(guest, "/user/queue/messaging-errors");
    var ownerLive = subscribe(owner, destination(id));
    send(guest, id, "{\"content\":\"  \"}");
    assertThat(json.readTree(take(errors)).get("code").asString()).isEqualTo("INVALID_MESSAGE");
    send(guest, id, "{\"content\":\"hello\",\"senderUserId\":\"" + OWNER_ID + "\"}");
    take(errors);
    send(guest, UUID.randomUUID(), "{\"content\":\"secret\"}");
    assertThat(json.readTree(take(errors)).get("code").asString())
        .isEqualTo("CONVERSATION_NOT_FOUND");
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              messages.send(id, GUEST_ID, "rolled back");
              status.setRollbackOnly();
            });
    reservation.setStatus(ReservationStatus.BLOCKED);
    reservationRepository.saveAndFlush(reservation);
    send(guest, id, "{\"content\":\"blocked\"}");
    assertThat(json.readTree(take(errors)).get("code").asString())
        .isEqualTo("CONVERSATION_UNAVAILABLE");
    assertThat(messageRepository.count()).isZero();
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt()).isNull();
    assertThat(live.poll(300, TimeUnit.MILLISECONDS)).isNull();
    assertThat(ownerLive).isEmpty();
  }

  @Test
  void connectRejectsBadCredentials() {
    for (String invalid :
        List.of(
            "",
            "garbage",
            token(GUEST_ID, Instant.now().minusSeconds(30)),
            Jwts.builder()
                .claim("userId", GUEST_ID.toString())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(
                    Keys.hmacShaKeyFor(
                        "different-secret-key-at-least-32-bytes-long"
                            .getBytes(StandardCharsets.UTF_8)))
                .compact())) {
      assertThatThrownBy(() -> connect(invalid)).isInstanceOf(Exception.class);
    }
    assertThat(messageRepository.count()).isZero();
  }

  @Test
  void unrelatedSubscriptionsAndBrokerSendsCloseWithSanitizedError() throws Exception {
    UUID id = conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
    var unrelated = connect(token(UUID.randomUUID(), Instant.now().plusSeconds(600)));
    unrelated.subscribe(destination(id), handler(new LinkedBlockingQueue<>()));
    assertThat(protocolErrors.get(unrelated).get(5, TimeUnit.SECONDS).getFirst("message"))
        .isEqualTo("CONVERSATION_NOT_FOUND");
    for (String destination :
        List.of(
            "/queue/x",
            "/topic/x",
            "/user/" + OWNER_ID + "/queue/x",
            "/user/queue/conversations/*/messages",
            "/user/queue/conversations/bad/messages",
            "/unknown")) {
      var session = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
      session.subscribe(destination, handler(new LinkedBlockingQueue<>()));
      assertThat(protocolErrors.get(session).get(5, TimeUnit.SECONDS).getFirst("message"))
          .isEqualTo("INVALID_DESTINATION");
    }
    var session = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
    session.send("/user/" + OWNER_ID + "/queue/x", new byte[0]);
    assertThat(protocolErrors.get(session).get(5, TimeUnit.SECONDS).getFirst("message"))
        .isEqualTo("INVALID_DESTINATION");
    assertThat(messageRepository.count()).isZero();
  }

  @Test
  void unauthorizedSendDoesNotPersistOrExposeLifecycle() throws Exception {
    UUID id = conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
    reservation.setStatus(ReservationStatus.BLOCKED);
    reservationRepository.saveAndFlush(reservation);
    var unrelated = connect(token(UUID.randomUUID(), Instant.now().plusSeconds(600)));
    var errors = subscribe(unrelated, "/user/queue/messaging-errors");
    send(unrelated, id, "{\"content\":\"secret\"}");
    assertThat(json.readTree(take(errors)).get("code").asString())
        .isEqualTo("CONVERSATION_NOT_FOUND");
    assertThat(messageRepository.count()).isZero();
  }

  @Test
  void foreignOriginCannotUpgrade() {
    var client = new WebSocketStompClient(new StandardWebSocketClient());
    clients.add(client);
    var headers = new org.springframework.web.socket.WebSocketHttpHeaders();
    headers.setOrigin("https://untrusted.example");
    assertThatThrownBy(
            () ->
                client
                    .connectAsync(
                        "ws://localhost:" + port + "/ws/conversations",
                        headers,
                        new StompHeaders(),
                        new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS))
        .isInstanceOf(Exception.class);
  }

  @Test
  void applicationFramesBeforeConnectAreRejected() throws Exception {
    for (String frame :
        List.of(
            "SEND\ndestination:/app/conversations/" + UUID.randomUUID() + "/messages\n\n{}\u0000",
            "SUBSCRIBE\nid:one\ndestination:/user/queue/messaging-errors\n\n\u0000")) {
      var received = new CompletableFuture<String>();
      var client = new StandardWebSocketClient();
      var socket =
          client
              .execute(
                  new org.springframework.web.socket.handler.TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(
                        org.springframework.web.socket.WebSocketSession session,
                        org.springframework.web.socket.TextMessage message) {
                      received.complete(message.getPayload());
                    }
                  },
                  "ws://localhost:" + port + "/ws/conversations")
              .get(5, TimeUnit.SECONDS);
      try {
        socket.sendMessage(new org.springframework.web.socket.TextMessage(frame));
        assertThat(received.get(5, TimeUnit.SECONDS))
            .startsWith("ERROR")
            .contains("UNAUTHENTICATED");
      } finally {
        socket.close();
      }
    }
    assertThat(messageRepository.count()).isZero();
  }

  @Test
  void escapedMaximumLengthContentIsAcceptedAndOverlongContentIsRejected() throws Exception {
    UUID id = conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
    var guest = connect(token(GUEST_ID, Instant.now().plusSeconds(600)));
    var live = subscribe(guest, destination(id));
    var errors = subscribe(guest, "/user/queue/messaging-errors");
    String escapedCharacter = "\\" + "u4e2d";
    send(guest, id, "{\"content\":\"" + escapedCharacter.repeat(4000) + "\"}");
    assertThat(json.readValue(take(live), MessageResponse.class).content())
        .isEqualTo("\u4e2d".repeat(4000));
    send(guest, id, "{\"content\":\"" + "x".repeat(4001) + "\"}");
    assertThat(json.readTree(take(errors)).get("code").asString()).isEqualTo("INVALID_MESSAGE");
    assertThat(messageRepository.count()).isEqualTo(1);
  }

  private String token(UUID user, Instant expiry) {
    return Jwts.builder()
        .subject("same-display-name")
        .claim("userId", user.toString())
        .claim("role", List.of("ROLE_GUEST"))
        .expiration(Date.from(expiry))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  private StompSession connect(String token) throws Exception {
    if (!scheduler.isRunning()) scheduler.initialize();
    var client = new WebSocketStompClient(new StandardWebSocketClient());
    var converter =
        new ByteArrayMessageConverter() {
          @Override
          protected boolean supportsMimeType(org.springframework.messaging.MessageHeaders headers) {
            return true;
          }
        };
    client.setMessageConverter(converter);
    client.setTaskScheduler(scheduler);
    client.setDefaultHeartbeat(new long[] {0, 0});
    clients.add(client);
    var headers = new StompHeaders();
    if (!token.isEmpty()) headers.set("Authorization", "Bearer " + token);
    var protocolError = new CompletableFuture<StompHeaders>();
    StompSession session =
        client
            .connectAsync(
                "ws://localhost:" + port + "/ws/conversations",
                new org.springframework.web.socket.WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                  @Override
                  public Type getPayloadType(StompHeaders h) {
                    return byte[].class;
                  }

                  @Override
                  public void handleFrame(StompHeaders h, Object payload) {
                    protocolError.complete(h);
                  }
                })
            .get(5, TimeUnit.SECONDS);
    protocolErrors.put(session, protocolError);
    session.setAutoReceipt(true);
    connected.add(session);
    return session;
  }

  private StompFrameHandler handler(BlockingQueue<byte[]> queue) {
    return new StompFrameHandler() {
      public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
      }

      public void handleFrame(StompHeaders headers, Object body) {
        queue.add((byte[]) body);
      }
    };
  }

  private BlockingQueue<byte[]> subscribe(StompSession session, String destination)
      throws Exception {
    var queue = new LinkedBlockingQueue<byte[]>();
    var receipt = new CompletableFuture<Void>();
    var subscription = session.subscribe(destination, handler(queue));
    subscription.addReceiptTask(() -> receipt.complete(null));
    subscription.addReceiptLostTask(
        () -> receipt.completeExceptionally(new IllegalStateException("Receipt lost")));
    receipt.get(5, TimeUnit.SECONDS);
    return queue;
  }

  private void send(StompSession session, UUID id, String payload) {
    var headers = new StompHeaders();
    headers.setDestination("/app/conversations/" + id + "/messages");
    headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
    session.send(headers, payload.getBytes(StandardCharsets.UTF_8));
  }

  private byte[] take(BlockingQueue<byte[]> queue) throws Exception {
    byte[] value = queue.poll(5, TimeUnit.SECONDS);
    assertThat(value).as("live frame").isNotNull();
    return value;
  }

  private String destination(UUID id) {
    return "/user/queue/conversations/" + id + "/messages";
  }

  private String path(UUID id) {
    return "/api/v1/conversations/" + id + "/messages";
  }

  private Authentication buildAuthentication(UUID userId, String role) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "user@kwatera.test", null, List.of(new SimpleGrantedAuthority(role)));
    authentication.setDetails(userId.toString());
    return authentication;
  }

  private Reservation reservation() {
    Reservation value = new Reservation();
    value.setUserId(GUEST_ID);
    value.setUnitId(UNIT_ID);
    value.setStartDate(LocalDate.of(2026, 11, 1));
    value.setEndDate(LocalDate.of(2026, 11, 4));
    value.setStatus(ReservationStatus.PENDING);
    value.setPricePerNightSnapshot(new BigDecimal("200.00"));
    value.setTotalPrice(new BigDecimal("600.00"));
    value.setPaymentCurrency("PLN");
    value.setPaymentExchangeRate(BigDecimal.ONE);
    return value;
  }
}
