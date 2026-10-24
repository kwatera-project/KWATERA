package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ConversationLifecyclePolicyTest {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Warsaw");

  @Mock private BusinessDateProvider businessDateProvider;

  private ConversationLifecyclePolicy policy;

  @BeforeEach
  void setUp() {
    policy = new ConversationLifecyclePolicy(businessDateProvider, 90);
    lenient()
        .when(businessDateProvider.toLocalDate(any(Instant.class)))
        .thenAnswer(
            invocation ->
                invocation.getArgument(0, Instant.class).atZone(BUSINESS_ZONE).toLocalDate());
  }

  @Test
  void shouldAllowPendingAndConfirmedReservations() {
    policy.requireAvailable(reservation(ReservationStatus.PENDING), null);
    policy.requireAvailable(reservation(ReservationStatus.CONFIRMED), null);
  }

  @Test
  void shouldRejectBlockedReservation() {
    assertThatThrownBy(() -> policy.requireAvailable(reservation(ReservationStatus.BLOCKED), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("409 CONFLICT");
  }

  @Test
  void shouldIncludeCompletedConversationOnRetentionBoundary() {
    Reservation reservation = reservation(ReservationStatus.COMPLETED);
    reservation.setEndDate(LocalDate.of(2026, 1, 1));
    when(businessDateProvider.today()).thenReturn(LocalDate.of(2026, 4, 1));

    policy.requireAvailable(reservation, null);

    assertThat(policy.retentionExpiresOn(reservation, null)).contains(LocalDate.of(2026, 4, 1));
  }

  @Test
  void shouldRejectCompletedConversationAfterRetentionBoundary() {
    Reservation reservation = reservation(ReservationStatus.COMPLETED);
    reservation.setEndDate(LocalDate.of(2026, 1, 1));
    when(businessDateProvider.today()).thenReturn(LocalDate.of(2026, 4, 2));

    assertThatThrownBy(() -> policy.requireAvailable(reservation, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("retention period has expired");
  }

  @Test
  void shouldExtendCancelledRetentionFromLastMessage() {
    Reservation reservation = reservation(ReservationStatus.CANCELLED);
    reservation.setCancelledAt(Instant.parse("2026-01-01T12:00:00Z"));
    Conversation conversation = new Conversation();
    conversation.setLastMessageAt(Instant.parse("2026-01-10T12:00:00Z"));

    assertThat(policy.retentionExpiresOn(reservation, conversation))
        .contains(LocalDate.of(2026, 4, 10));
  }

  @Test
  void shouldRejectLegacyCancelledReservationWithoutReliableCancellationDate() {
    assertThatThrownBy(
            () -> policy.requireAvailable(reservation(ReservationStatus.CANCELLED), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("lifecycle date is unavailable");
  }

  private Reservation reservation(ReservationStatus status) {
    Reservation reservation = new Reservation();
    reservation.setStatus(status);
    return reservation;
  }
}
