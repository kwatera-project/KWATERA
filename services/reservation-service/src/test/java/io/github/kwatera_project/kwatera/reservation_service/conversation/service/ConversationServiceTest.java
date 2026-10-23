package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.reservation_service.conversation.client.PropertyOwnershipClient;
import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.ConversationResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.ConversationRepository;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private PropertyOwnershipClient propertyOwnershipClient;
  @Mock private ConversationLifecyclePolicy lifecyclePolicy;

  private ConversationService service;

  @BeforeEach
  void setUp() {
    service =
        new ConversationService(
            conversationRepository,
            reservationRepository,
            propertyOwnershipClient,
            lifecyclePolicy,
            new ConversationParticipantAuthorizer());
  }

  @Test
  void shouldCreateConversationWithParticipantSnapshot() {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Reservation reservation =
        reservation(reservationId, guestId, unitId, ReservationStatus.PENDING);
    when(reservationRepository.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(conversationRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());
    when(propertyOwnershipClient.getOwnerIdForUnit(unitId)).thenReturn(ownerId);
    when(lifecyclePolicy.retentionExpiresOn(any(), any())).thenReturn(Optional.empty());
    when(conversationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              Conversation conversation = invocation.getArgument(0);
              conversation.setId(UUID.randomUUID());
              conversation.setCreatedAt(Instant.now());
              return conversation;
            });

    ConversationResponse response = service.getOrCreate(reservationId, guestId);

    assertThat(response.guestUserId()).isEqualTo(guestId);
    assertThat(response.ownerUserId()).isEqualTo(ownerId);
    assertThat(response.reservationId()).isEqualTo(reservationId);
    verify(lifecyclePolicy).requireAvailable(reservation, null);
  }

  @Test
  void shouldReturnExistingConversationWithoutResolvingCurrentPropertyOwner() {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID originalOwnerId = UUID.randomUUID();
    Reservation reservation =
        reservation(reservationId, guestId, UUID.randomUUID(), ReservationStatus.CONFIRMED);
    Conversation conversation = conversation(reservationId, guestId, originalOwnerId);
    when(reservationRepository.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(conversationRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(conversation));
    when(lifecyclePolicy.retentionExpiresOn(any(), any())).thenReturn(Optional.empty());

    ConversationResponse response = service.getOrCreate(reservationId, originalOwnerId);

    assertThat(response.ownerUserId()).isEqualTo(originalOwnerId);
    verify(propertyOwnershipClient, never()).getOwnerIdForUnit(reservation.getUnitId());
  }

  @Test
  void shouldHideExistingConversationFromUnrelatedUser() {
    UUID reservationId = UUID.randomUUID();
    Reservation reservation =
        reservation(
            reservationId, UUID.randomUUID(), UUID.randomUUID(), ReservationStatus.CONFIRMED);
    Conversation conversation =
        conversation(reservationId, reservation.getUserId(), UUID.randomUUID());
    when(reservationRepository.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(conversationRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(conversation));

    assertThatThrownBy(() -> service.getOrCreate(reservationId, UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
    verify(lifecyclePolicy, never()).requireAvailable(reservation, conversation);
  }

  private Reservation reservation(
      UUID reservationId, UUID guestId, UUID unitId, ReservationStatus status) {
    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);
    reservation.setUnitId(unitId);
    reservation.setStatus(status);
    return reservation;
  }

  private Conversation conversation(UUID reservationId, UUID guestId, UUID ownerId) {
    Conversation conversation = new Conversation();
    conversation.setId(UUID.randomUUID());
    conversation.setReservationId(reservationId);
    conversation.setGuestUserId(guestId);
    conversation.setOwnerUserId(ownerId);
    conversation.setCreatedAt(Instant.now());
    return conversation;
  }
}
