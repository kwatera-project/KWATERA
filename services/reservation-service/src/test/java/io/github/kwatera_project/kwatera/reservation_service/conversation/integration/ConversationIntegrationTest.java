package io.github.kwatera_project.kwatera.reservation_service.conversation.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.client.PropertyOwnershipClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.ConversationResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.ConversationRepository;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationService;
import io.github.kwatera_project.kwatera.reservation_service.event.SettlementEventListener;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.service.AdminReservationService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
class ConversationIntegrationTest {

  private static final UUID GUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID UNIT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private ConversationService conversationService;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private MockMvc mockMvc;
  @Autowired private AdminReservationService adminReservationService;

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

  private Reservation reservation;

  @BeforeEach
  void setUp() {
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

  @Test
  void ownerShouldCreateAndGuestShouldReceiveTheSameConversation() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(OWNER_ID.toString()))
        .andExpect(jsonPath("$.guestUserId").value(GUEST_ID.toString()));

    ConversationResponse created = conversationService.list(GUEST_ID).getFirst();

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.id().toString()));

    assertThat(conversationRepository.findAll()).hasSize(1);
  }

  @Test
  void concurrentCreationShouldRemainIdempotent() throws Exception {
    CountDownLatch start = new CountDownLatch(1);

    CompletableFuture<ConversationResponse> first = createAfter(start);
    CompletableFuture<ConversationResponse> second = createAfter(start);
    start.countDown();

    ConversationResponse firstResponse = first.get(10, TimeUnit.SECONDS);
    ConversationResponse secondResponse = second.get(10, TimeUnit.SECONDS);

    assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
    assertThat(conversationRepository.findAll()).hasSize(1);
  }

  @Test
  void shouldPreventIdorAndRequireAuthentication() throws Exception {
    ConversationResponse conversation =
        conversationService.getOrCreate(reservation.getId(), GUEST_ID);
    UUID unrelatedId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/conversations/" + conversation.id()))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            get("/api/v1/conversations/" + conversation.id())
                .with(authentication(buildAuthentication(unrelatedId, "ROLE_GUEST"))))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/conversations/" + conversation.id())
                .with(authentication(buildAuthentication(unrelatedId, "ROLE_ADMIN"))))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/conversations/" + conversation.id())
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(conversation.id().toString()));
  }

  @Test
  void shouldListConversationForBothSnapshottedParticipantsOnly() throws Exception {
    ConversationResponse conversation =
        conversationService.getOrCreate(reservation.getId(), GUEST_ID);

    mockMvc
        .perform(
            get("/api/v1/conversations")
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(conversation.id().toString()));

    mockMvc
        .perform(
            get("/api/v1/conversations")
                .with(authentication(buildAuthentication(UUID.randomUUID(), "ROLE_GUEST"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void shouldReturn404ForMissingReservationAndConversation() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/reservations/" + UUID.randomUUID() + "/conversation")
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/v1/conversations/" + UUID.randomUUID())
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectBlockedReservationForItsOwner() throws Exception {
    reservation.setStatus(ReservationStatus.BLOCKED);
    reservationRepository.saveAndFlush(reservation);

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldAllowCompletedAndCancelledReservationsWithinRetention() throws Exception {
    reservation.setStatus(ReservationStatus.COMPLETED);
    reservationRepository.saveAndFlush(reservation);

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isOk());

    conversationRepository.deleteAll();
    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setCancelledAt(Instant.parse("2026-11-15T12:00:00Z"));
    reservationRepository.saveAndFlush(reservation);

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(OWNER_ID, "ROLE_OWNER"))))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRejectConversationAfterRetentionExpired() throws Exception {
    reservation.setStatus(ReservationStatus.COMPLETED);
    reservation.setEndDate(LocalDate.of(2026, 1, 1));
    reservationRepository.saveAndFlush(reservation);

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isConflict());

    assertThat(conversationRepository.findAll()).isEmpty();
  }

  @Test
  void shouldReportPropertyServiceOutageAs503() throws Exception {
    when(propertyOwnershipClient.getOwnerIdForUnit(UNIT_ID))
        .thenThrow(
            new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Property service is unavailable"));

    mockMvc
        .perform(
            put("/api/v1/reservations/" + reservation.getId() + "/conversation")
                .with(authentication(buildAuthentication(GUEST_ID, "ROLE_GUEST"))))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void endedReservationConversationShouldBlockUnitDeletionGuard() {
    conversationService.getOrCreate(reservation.getId(), GUEST_ID);
    reservation.setStatus(ReservationStatus.COMPLETED);
    reservationRepository.saveAndFlush(reservation);

    assertThat(adminReservationService.hasReservationsForUnit(UNIT_ID)).isTrue();
  }

  @Test
  void openApiShouldPublishAllConversationEndpoints() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
        .andExpect(jsonPath("$.paths['/api/v1/conversations'].get.responses['200']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/conversations'].get.responses['401']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/conversations'].get.security[0].bearerAuth").exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/conversations/{conversationId}'].get.responses['409']")
                .exists())
        .andExpect(
            jsonPath("$.paths['/api/v1/conversations/{conversationId}'].get.security[0].bearerAuth")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/reservations/{reservationId}/conversation'].put.responses['502']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/reservations/{reservationId}/conversation'].put.responses['503']")
                .exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/reservations/{reservationId}/conversation'].put.security[0].bearerAuth")
                .exists());
  }

  private CompletableFuture<ConversationResponse> createAfter(CountDownLatch start) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            start.await();
            return conversationService.getOrCreate(reservation.getId(), GUEST_ID);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
          }
        });
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
