package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatusHistory;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationStatusHistoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class Stage2ReservationFlowTest {

  @Mock private ReservationRepository reservationRepository;

  @Mock private ReservationStatusHistoryRepository statusHistoryRepository;

  @Mock private ReservationStatusValidator statusValidator;

  @Mock private RestTemplate restTemplate;

  private ReservationService reservationService;

  private AdminReservationService adminReservationService;

  @BeforeEach
  void setUp() {
    reservationService = new ReservationService(reservationRepository, restTemplate);
    adminReservationService =
        new AdminReservationService(
            reservationRepository, statusHistoryRepository, statusValidator, restTemplate);
  }

  @Test
  void shouldCompleteCriticalStage2FlowFromAvailabilityToReservationStatusUpdate() {
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate startDate = LocalDate.now().plusDays(14);
    LocalDate endDate = startDate.plusDays(4);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(startDate);
    request.setEndDate(endDate);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of());
    when(reservationRepository.save(any(Reservation.class)))
        .thenAnswer(
            invocation -> {
              Reservation reservation = invocation.getArgument(0);
              if (reservation.getId() == null) {
                reservation.setId(reservationId);
              }
              return reservation;
            });

    AvailabilityDto availability = reservationService.checkAvailability(unitId, startDate, endDate);

    Reservation createdReservation = reservationService.createReservation(guestId, request);

    when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(createdReservation));

    ReservationOverviewDto updatedReservation =
        adminReservationService.updateReservationStatus(
            reservationId, ReservationStatus.CONFIRMED, adminId, true);

    assertThat(availability.isAvailable()).isTrue();
    assertThat(createdReservation.getId()).isEqualTo(reservationId);
    assertThat(createdReservation.getUserId()).isEqualTo(guestId);
    assertThat(createdReservation.getUnitId()).isEqualTo(unitId);
    assertThat(createdReservation.getStartDate()).isEqualTo(startDate);
    assertThat(createdReservation.getEndDate()).isEqualTo(endDate);
    assertThat(updatedReservation.status()).isEqualTo(ReservationStatus.CONFIRMED);

    verify(reservationRepository, times(2)).findByUnitId(unitId);
    verify(statusValidator)
        .validateTransition(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    verify(reservationRepository, times(2)).save(any(Reservation.class));

    ArgumentCaptor<ReservationStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(ReservationStatusHistory.class);
    verify(statusHistoryRepository).save(historyCaptor.capture());

    ReservationStatusHistory history = historyCaptor.getValue();
    assertThat(history.getReservationId()).isEqualTo(reservationId);
    assertThat(history.getOldStatus()).isEqualTo(ReservationStatus.PENDING);
    assertThat(history.getNewStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    assertThat(history.getChangedBy()).isEqualTo(adminId);
    assertThat(history.getChangedAt()).isNotNull();
  }
}
