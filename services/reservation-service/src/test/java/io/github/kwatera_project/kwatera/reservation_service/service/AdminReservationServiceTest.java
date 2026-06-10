package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminReservationServiceTest {

  @Mock private ReservationRepository reservationRepository;

  @Mock private ReservationStatusHistoryRepository statusHistoryRepository;

  @Mock private ReservationStatusValidator statusValidator;

  @Mock private RestTemplate restTemplate;

  @Mock private EmailNotificationService emailNotificationService;

  @Mock private SystemEventService systemEventService;

  @InjectMocks private AdminReservationService adminReservationService;

  @BeforeEach
  void setUp() {
    // Dependencies are injected via @InjectMocks.
  }

  @Test
  void shouldReturnAllReservations_whenUserIsAdmin() {
    Reservation reservation = createReservation();
    when(reservationRepository.findAll()).thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(UUID.randomUUID(), null, true);

    assertEquals(1, result.size());
    verify(reservationRepository).findAll();
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
  }

  @Test
  void shouldUpdateStatus_whenAdminChangesAnyReservation() {
    UUID resId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Reservation reservation = createReservation();
    reservation.setId(resId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));

    ReservationOverviewDto result =
        adminReservationService.updateReservationStatus(
            resId, ReservationStatus.CONFIRMED, adminId, true);

    assertEquals(ReservationStatus.CONFIRMED, result.status());
    verify(statusValidator)
        .validateTransition(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
    verify(reservationRepository).save(reservation);
    verify(statusHistoryRepository).save(any(ReservationStatusHistory.class));
  }

  @Test
  void shouldLogReservationStatusChangedEvent_whenAdminChangesReservationStatus() {
    UUID resId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Reservation reservation = createReservation();
    reservation.setId(resId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));

    adminReservationService.updateReservationStatus(
        resId, ReservationStatus.CONFIRMED, adminId, true);

    verify(systemEventService)
        .logSafely(
            eq(SystemEventType.RESERVATION_STATUS_CHANGED),
            eq(adminId),
            eq(SystemEventService.ENTITY_TYPE_RESERVATION),
            eq(resId),
            argThat(
                details ->
                    details.contains("oldStatus=PENDING")
                        && details.contains("newStatus=CONFIRMED")));
  }

  @Test
  void shouldUpdateStatus_whenOwnerChangesOwnReservation() {
    UUID resId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Reservation reservation = createReservation();
    reservation.setId(resId);
    reservation.setUnitId(unitId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(new UUID[] {unitId});

    ReservationOverviewDto result =
        adminReservationService.updateReservationStatus(
            resId, ReservationStatus.CONFIRMED, ownerId, false);

    assertEquals(ReservationStatus.CONFIRMED, result.status());
    verify(reservationRepository).save(reservation);
  }

  @Test
  void shouldThrowForbidden_whenOwnerChangesForeignReservation() {
    UUID resId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    Reservation reservation = createReservation();
    reservation.setId(resId);
    reservation.setUnitId(UUID.randomUUID()); // different unit

    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(new UUID[] {unitId});

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                adminReservationService.updateReservationStatus(
                    resId, ReservationStatus.CONFIRMED, ownerId, false));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void shouldThrowNotFound_whenReservationDoesNotExist() {
    UUID resId = UUID.randomUUID();
    when(reservationRepository.findById(resId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                adminReservationService.updateReservationStatus(
                    resId, ReservationStatus.CONFIRMED, UUID.randomUUID(), true));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void shouldThrowBadRequest_whenTransitionIsInvalid() {
    UUID resId = UUID.randomUUID();
    Reservation reservation = createReservation();
    reservation.setStatus(ReservationStatus.CANCELLED);

    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));
    doThrow(new IllegalStateException("Invalid transition"))
        .when(statusValidator)
        .validateTransition(ReservationStatus.CANCELLED, ReservationStatus.CONFIRMED);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                adminReservationService.updateReservationStatus(
                    resId, ReservationStatus.CONFIRMED, UUID.randomUUID(), true));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowBadRequest_whenNewStatusIsNull() {
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                adminReservationService.updateReservationStatus(
                    UUID.randomUUID(), null, UUID.randomUUID(), true));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldReturnOwnerReservations_whenUserIsOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID[] unitIds = {unitId};
    Reservation reservation = createReservation();

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/ids/" + ownerId, UUID[].class))
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

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(unitIds);
    when(reservationRepository.findByUnitIdInAndStatus(
            List.of(unitId), ReservationStatus.CONFIRMED))
        .thenReturn(List.of(reservation));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(
            ownerId, ReservationStatus.CONFIRMED, false);

    assertEquals(1, result.size());
  }

  @Test
  void shouldReturnEmptyList_whenOwnerHasNoUnits() {
    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(new UUID[0]);
    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(UUID.randomUUID(), null, false);
    assertEquals(0, result.size());
  }

  @Test
  void shouldHandlePropertyServiceErrorInVerifyOwnerAccess() {
    UUID resId = UUID.randomUUID();
    Reservation reservation = createReservation();
    when(reservationRepository.findById(resId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class)))
        .thenThrow(new RuntimeException("API Down"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                adminReservationService.updateReservationStatus(
                    resId, ReservationStatus.CONFIRMED, UUID.randomUUID(), false));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    assertTrue(ex.getReason().contains("Unable to verify ownership"));
  }

  @Test
  void shouldHandleUnitNameFetchError() {
    Reservation reservation = createReservation();
    when(reservationRepository.findAll()).thenReturn(List.of(reservation));
    when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("API Down"));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(UUID.randomUUID(), null, true);
    assertEquals(1, result.size());
    assertEquals(
        "Room " + reservation.getUnitId().toString().substring(0, 8), result.get(0).unitName());
  }

  @Test
  void shouldHandlePropertyServiceErrorInOverview() {
    UUID ownerId = UUID.randomUUID();
    when(restTemplate.getForObject(anyString(), eq(UUID[].class)))
        .thenThrow(new RuntimeException("Connection error"));

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(ownerId, null, false);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldHandleNullUnitIdsArrayFromPropertyService() {
    UUID ownerId = UUID.randomUUID();
    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(null);

    List<ReservationOverviewDto> result =
        adminReservationService.getReservationsOverview(ownerId, null, false);

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnTrue_whenUnitHasPendingOrConfirmedReservations() {
    UUID unitId = UUID.randomUUID();

    when(reservationRepository.existsByUnitIdAndStatusIn(
            eq(unitId), eq(List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))))
        .thenReturn(true);

    boolean result = adminReservationService.hasReservationsForUnit(unitId);

    assertTrue(result);

    verify(reservationRepository)
        .existsByUnitIdAndStatusIn(
            unitId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
  }

  @Test
  void shouldReturnFalse_whenUnitHasNoPendingOrConfirmedReservations() {
    UUID unitId = UUID.randomUUID();

    when(reservationRepository.existsByUnitIdAndStatusIn(
            eq(unitId), eq(List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))))
        .thenReturn(false);

    boolean result = adminReservationService.hasReservationsForUnit(unitId);

    assertFalse(result);

    verify(reservationRepository)
        .existsByUnitIdAndStatusIn(
            unitId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
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
