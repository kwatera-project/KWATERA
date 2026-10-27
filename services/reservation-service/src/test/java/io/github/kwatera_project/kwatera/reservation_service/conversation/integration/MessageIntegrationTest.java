package io.github.kwatera_project.kwatera.reservation_service.conversation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
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
class MessageIntegrationTest {

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

  @AfterEach
  void cleanMessages() {
    messageRepository.deleteAll();
  }

  @ParameterizedTest
  @ValueSource(strings = {"guest", "owner"})
  void participantsCanSendAndReadAcrossTransactions(String participant) throws Exception {
    UUID user = participant.equals("guest") ? GUEST_ID : OWNER_ID;
    UUID conversation = conversationId();
    Instant before = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(path(conversation))
                .with(authentication(buildAuthentication(user, "ROLE_GUEST")))
                .contentType("application/json")
                .content(
                    "{\"content\":\"  Hello\\n  there  \",\"senderUserId\":\""
                        + UUID.randomUUID()
                        + "\",\"createdAt\":\"2000-01-01T00:00:00Z\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.senderUserId").value(user.toString()))
        .andExpect(jsonPath("$.conversationId").value(conversation.toString()))
        .andExpect(jsonPath("$.content").value("Hello\n  there"));
    var stored = messageRepository.findAll().getFirst();
    assertThat(stored.getCreatedAt()).isBetween(before, Instant.now());
    assertThat(conversationRepository.findById(conversation).orElseThrow().getLastMessageAt())
        .isEqualTo(stored.getCreatedAt());
    mockMvc
        .perform(
            get(path(conversation))
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(stored.getId().toString()))
        .andExpect(jsonPath("$.nextCursor").isEmpty())
        .andExpect(jsonPath("$.hasMore").value(false));
  }

  @Test
  void emptyHistoryAndDefaultAndMaximumBounds() {
    UUID id = conversationId();
    var empty = messages.history(id, GUEST_ID, null, null);
    assertThat(empty.items()).isEmpty();
    assertThat(empty.nextCursor()).isNull();
    assertThat(empty.hasMore()).isFalse();
    for (int i = 0; i < 101; i++) {
      messages.send(id, GUEST_ID, "message " + i);
    }
    var first = messages.history(id, OWNER_ID, null, null);
    assertThat(first.items()).hasSize(50);
    assertThat(first.hasMore()).isTrue();
    assertThat(first.nextCursor()).isNotNull();
    var maximum = messages.history(id, GUEST_ID, "100", null);
    assertThat(maximum.items()).hasSize(100);
    assertThat(maximum.hasMore()).isTrue();
    var last = messages.history(id, GUEST_ID, "100", maximum.nextCursor());
    assertThat(last.items()).hasSize(1);
    assertThat(last.hasMore()).isFalse();
    assertThat(last.nextCursor()).isNull();
  }

  @Test
  void keysetTraversalHandlesTiesIsolationAndConcurrentInsertion() {
    UUID id = conversationId();
    Instant timestamp = Instant.parse("2026-10-27T12:00:00.123456Z");
    List<UUID> orderedIds =
        List.of(
            UUID.fromString("ffffffff-0000-0000-0000-000000000001"),
            UUID.fromString("80000000-0000-0000-0000-000000000001"),
            UUID.fromString("00000000-0000-0000-0000-000000000001"));
    for (UUID messageId : orderedIds) {
      insertMessage(id, messageId, timestamp);
    }
    UUID other = otherConversation();
    messages.send(other, GUEST_ID, "isolated");
    var first = messages.history(id, GUEST_ID, "1", null);
    assertThat(first.items())
        .extracting(value -> value.id())
        .containsExactly(orderedIds.getFirst());
    insertMessage(id, UUID.randomUUID(), timestamp.plusSeconds(1));
    var second = messages.history(id, GUEST_ID, "1", first.nextCursor());
    var third = messages.history(id, GUEST_ID, "1", second.nextCursor());
    assertThat(second.items()).extracting(value -> value.id()).containsExactly(orderedIds.get(1));
    assertThat(third.items()).extracting(value -> value.id()).containsExactly(orderedIds.get(2));
    assertThat(third.hasMore()).isFalse();
    assertThat(third.nextCursor()).isNull();
    var newest = messages.history(id, GUEST_ID, "100", null);
    assertThat(newest.items().getFirst().createdAt()).isEqualTo(timestamp.plusSeconds(1));
    assertThat(newest.items()).hasSize(4);
    assertStatus(() -> messages.history(other, GUEST_ID, "1", first.nextCursor()), 400);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "101", "abc", "1.5", "2147483648", ""})
  void invalidLimitsAreRejectedAfterAccess(String limit) throws Exception {
    UUID id = conversationId();
    mockMvc
        .perform(
            get(path(id))
                .param("limit", limit)
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isBadRequest());
    assertStatus(() -> messages.history(id, UUID.randomUUID(), limit, "bad"), 404);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "bad", "@@@", "MQ", "a|b|c", "===="})
  void malformedCursorsAreRejected(String cursor) throws Exception {
    UUID id = conversationId();
    mockMvc
        .perform(
            get(path(id))
                .param("cursor", cursor)
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t", "\u2003"})
  void serviceRejectsInvalidContentWithoutChangingMetadata(String content) {
    UUID id = conversationId();
    assertStatus(() -> messages.send(id, GUEST_ID, content), 400);
    assertThat(messageRepository.count()).isZero();
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt()).isNull();
  }

  @Test
  void normalizedLengthIsValidatedAtBothBoundaries() throws Exception {
    UUID id = conversationId();
    assertStatus(() -> messages.send(id, GUEST_ID, "a".repeat(4001)), 400);
    for (String body :
        List.of(
            "{}",
            "{\"content\":null}",
            "{\"content\":\"\"}",
            "{\"content\":\"  \\" + "n\"}",
            "{\"content\":\"" + "a".repeat(4001) + "\"}",
            "{")) {
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(path(id))
                  .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST")))
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    }
    assertThat(messageRepository.count()).isZero();
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt()).isNull();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(path(id))
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST")))
                .contentType("application/json")
                .content("{\"content\":\"  " + "a".repeat(4000) + "  \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("a".repeat(4000)));
    assertThat(messages.send(id, OWNER_ID, "\u2003" + "b".repeat(4000) + "\u2003").content())
        .hasSize(4000);
  }

  @Test
  void outsidersAndAnonymousCannotReadOrWriteAndMissingIsIndistinguishable() throws Exception {
    UUID id = conversationId();
    for (UUID target : List.of(id, UUID.randomUUID())) {
      mockMvc.perform(get(path(target))).andExpect(status().isUnauthorized());
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(path(target))
                  .contentType("application/json")
                  .content("{\"content\":\"hello\"}"))
          .andExpect(status().isUnauthorized());
      var outsider = buildAuthentication(UUID.randomUUID(), "ROLE_ADMIN");
      mockMvc
          .perform(get(path(target)).param("cursor", "bad").with(authentication(outsider)))
          .andExpect(status().isNotFound());
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(path(target))
                  .with(authentication(outsider))
                  .contentType("application/json")
                  .content("{\"content\":\"hello\"}"))
          .andExpect(status().isNotFound());
      assertStatus(() -> messages.send(target, UUID.randomUUID(), "hello"), 404);
      assertStatus(() -> messages.history(target, UUID.randomUUID(), "0", "bad"), 404);
    }
    assertStatus(() -> messages.send(id, null, "hello"), 401);
    assertStatus(() -> messages.history(id, null, null, null), 401);
    for (String badId : List.of("bad", "123")) {
      mockMvc
          .perform(
              get(path(badId)).with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
          .andExpect(status().isBadRequest());
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(path(badId))
                  .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST")))
                  .contentType("application/json")
                  .content("{\"content\":\"hello\"}"))
          .andExpect(status().isBadRequest());
    }
    assertThat(messageRepository.count()).isZero();
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"BLOCKED", "COMPLETED", "CANCELLED"})
  void unavailableLifecycleRejectsBothOperationsBeforeChangingRetention(ReservationStatus state)
      throws Exception {
    UUID id = conversationId();
    reservation.setStatus(state);
    reservation.setEndDate(LocalDate.of(2026, 1, 1));
    reservation.setCancelledAt(Instant.parse("2026-01-01T12:00:00Z"));
    reservationRepository.saveAndFlush(reservation);
    assertStatus(() -> messages.send(id, GUEST_ID, "cannot revive"), 409);
    assertStatus(() -> messages.history(id, OWNER_ID, null, null), 409);
    assertStatus(() -> messages.history(id, UUID.randomUUID(), "0", "bad"), 404);
    mockMvc
        .perform(get(path(id)).with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isConflict());
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(path(id))
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER")))
                .contentType("application/json")
                .content("{\"content\":\"hello\"}"))
        .andExpect(status().isConflict());
    assertThat(messageRepository.count()).isZero();
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt()).isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = ReservationStatus.class,
      names = {"COMPLETED", "CANCELLED"})
  void retentionBoundaryAndLaterMessageAnchorArePreserved(ReservationStatus state) {
    UUID id = conversationId();
    LocalDate anchor = LocalDate.now(java.time.ZoneOffset.UTC).plusDays(30);
    reservation.setStatus(state);
    reservation.setEndDate(anchor);
    reservation.setCancelledAt(anchor.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC));
    reservationRepository.saveAndFlush(reservation);
    when(businessDateProvider.today()).thenReturn(anchor.plusDays(90));
    messages.history(id, GUEST_ID, null, null);
    messages.send(id, OWNER_ID, "boundary permitted");
    when(businessDateProvider.today()).thenReturn(anchor.plusDays(91));
    assertStatus(() -> messages.history(id, GUEST_ID, null, null), 409);
    var conversation = conversationRepository.findById(id).orElseThrow();
    conversation.setLastMessageAt(
        anchor.plusDays(20).atTime(12, 0).toInstant(java.time.ZoneOffset.UTC));
    conversationRepository.saveAndFlush(conversation);
    messages.history(id, GUEST_ID, null, null);
  }

  @Test
  void confirmedReservationAllowsMessaging() {
    UUID id = conversationId();
    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservationRepository.saveAndFlush(reservation);
    messages.send(id, GUEST_ID, "confirmed");
    assertThat(messages.history(id, OWNER_ID, null, null).items()).hasSize(1);
  }

  @Test
  void concurrentSendsPersistBothAndKeepTheMaximumTimestamp() throws Exception {
    UUID id = conversationId();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    var first = sendAfter(ready, start, id, GUEST_ID);
    var second = sendAfter(ready, start, id, OWNER_ID);
    assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    var a = first.get(10, TimeUnit.SECONDS);
    var b = second.get(10, TimeUnit.SECONDS);
    assertThat(messageRepository.count()).isEqualTo(2);
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt())
        .isEqualTo(a.createdAt().isAfter(b.createdAt()) ? a.createdAt() : b.createdAt());
    // A backwards wall-clock step cannot decrease the retention anchor either.
    var conversation = conversationRepository.findById(id).orElseThrow();
    Instant future = Instant.parse("2099-01-01T00:00:00Z");
    conversation.setLastMessageAt(future);
    conversationRepository.saveAndFlush(conversation);
    messages.send(id, GUEST_ID, "clock moved back");
    assertThat(conversationRepository.findById(id).orElseThrow().getLastMessageAt())
        .isEqualTo(future);
  }

  @Test
  void openApiIncludesMessagingSchemasSecurityAndResponses() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/conversations/{conversationId}/messages'].post.responses['201']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/conversations/{conversationId}/messages'].get.responses['200']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/conversations/{conversationId}/messages'].post.security[0].bearerAuth")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/conversations/{conversationId}/messages'].get.responses['409']")
                .exists())
        .andExpect(
            jsonPath("$.components.schemas.SendMessageRequest.properties.content.maxLength")
                .value(4000))
        .andExpect(
            jsonPath("$.components.schemas.MessageHistoryResponse.properties.items").exists());
  }

  private CompletableFuture<MessageResponse> sendAfter(
      CountDownLatch ready, CountDownLatch start, UUID id, UUID user) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
              throw new IllegalStateException("Timed out waiting for send barrier");
            }
            return messages.send(id, user, "concurrent");
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
          }
        });
  }

  private UUID conversationId() {
    return conversationService.getOrCreate(reservation.getId(), GUEST_ID).id();
  }

  private UUID otherConversation() {
    Reservation other = reservationRepository.saveAndFlush(reservation());
    return conversationService.getOrCreate(other.getId(), GUEST_ID).id();
  }

  private String path(Object id) {
    return "/api/v1/conversations/" + id + "/messages";
  }

  private void insertMessage(UUID conversation, UUID id, Instant timestamp) {
    jdbc.update(
        "INSERT INTO conversation_messages (id, conversation_id, sender_user_id, content, created_at) VALUES (?, ?, ?, ?, ?)",
        id,
        conversation,
        GUEST_ID,
        "tie",
        java.time.OffsetDateTime.ofInstant(timestamp, java.time.ZoneOffset.UTC));
  }

  private void assertStatus(Runnable operation, int status) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
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
