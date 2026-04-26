package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AdminReservationServiceTest {

  @Mock private ReservationRepository reservationRepository;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private AdminReservationService adminReservationService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(adminReservationService, "restTemplate", restTemplate);
  }

  @Test
  void shouldReturnAllReservations_whenUserIsAdmin() {
    Reservation reservation = createReservation();
    when(reservationRepository.findAll()).thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(UUID.randomUUID(), null, true);

    assertEquals(1, result.size());
    verify(reservationRepository).findAll();
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldReturnFilteredReservations_whenUserIsAdminAndStatusProvided() {
    Reservation reservation = createReservation();
    when(reservationRepository.findByStatus(ReservationStatus.CONFIRMED))
        .thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(
            UUID.randomUUID(), ReservationStatus.CONFIRMED, true);

    assertEquals(1, result.size());
    verify(reservationRepository).findByStatus(ReservationStatus.CONFIRMED);
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldReturnOwnerReservations_whenUserIsOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID[] unitIds = {unitId};
    Reservation reservation = createReservation();

    when(restTemplate.getForObject(
            "http://property-service:8083/api/properties/units/ids/" + ownerId, UUID[].class))
        .thenReturn(unitIds);
    when(reservationRepository.findByUnitIdIn(List.of(unitId))).thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(ownerId, null, false);

    assertEquals(1, result.size());
    verify(reservationRepository).findByUnitIdIn(List.of(unitId));
  }

  @Test
  void shouldReturnFilteredOwnerReservations_whenUserIsOwnerAndStatusProvided() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID[] unitIds = {unitId};
    Reservation reservation = createReservation();

    when(restTemplate.getForObject(
            "http://property-service:8083/api/properties/units/ids/" + ownerId, UUID[].class))
        .thenReturn(unitIds);
    when(reservationRepository.findByUnitIdInAndStatus(
            List.of(unitId), ReservationStatus.CONFIRMED))
        .thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(
            ownerId, ReservationStatus.CONFIRMED, false);

    assertEquals(1, result.size());
    verify(reservationRepository)
        .findByUnitIdInAndStatus(List.of(unitId), ReservationStatus.CONFIRMED);
  }

  @Test
  void shouldReturnEmptyList_whenOwnerHasNoUnits() {
    UUID ownerId = UUID.randomUUID();

    when(restTemplate.getForObject(
            "http://property-service:8083/api/properties/units/ids/" + ownerId, UUID[].class))
        .thenReturn(new UUID[0]);

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(ownerId, null, false);

    assertEquals(0, result.size());
    verifyNoInteractions(reservationRepository);
  }

  @Test
  void shouldReturnEmptyList_whenPropertyServiceThrowsException() {
    UUID ownerId = UUID.randomUUID();

    when(restTemplate.getForObject(anyString(), eq(UUID[].class)))
        .thenThrow(new RuntimeException("Service unavailable"));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(ownerId, null, false);

    assertEquals(0, result.size());
    verifyNoInteractions(reservationRepository);
  }

  private Reservation createReservation() {
    Reservation reservation = new Reservation();
    reservation.setId(UUID.randomUUID());
    reservation.setUserId(UUID.randomUUID());
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(1));
    reservation.setStatus(ReservationStatus.CONFIRMED);
    return reservation;
  }
}
