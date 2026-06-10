package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceDashboardMetricsTest {

  @Mock private ReservationRepository reservationRepository;
  @Mock private RestTemplate restTemplate;
  @Mock private ObjectFactory<RestOperations> restOperationsFactory;
  @Mock private NbpExchangeRateClient nbpExchangeRateClient;
  @Mock private BusinessDateProvider businessDateProvider;

  @InjectMocks private ReservationService reservationService;

  private UUID ownerId;
  private UUID unitId;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    unitId = UUID.randomUUID();
    lenient().when(restOperationsFactory.getObject()).thenReturn(restTemplate);
    when(businessDateProvider.today()).thenReturn(LocalDate.of(2026, 5, 15));
  }

  @Test
  void shouldThrowBadRequest_whenStartDateAfterEndDate() {
    LocalDate start = LocalDate.of(2026, 5, 31);
    LocalDate end = LocalDate.of(2026, 5, 1);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.getDashboardReservationMetrics(start, end, ownerId, false));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Start date must be before or equal to end date", exception.getReason());
  }

  @Test
  void shouldReturnEmptyMetrics_whenNoUnitsFound() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 31);

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(null);

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(0L, metrics.getTotalReservations());
    assertEquals(0.0, metrics.getOccupancyRate());
    assertEquals(0L, metrics.getOccupiedDays());
  }

  @Test
  void shouldReturnMetrics_whenNoReservationsFound() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 31);
    UUID[] units = new UUID[] {unitId};

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(0L);
    when(reservationRepository.findActiveReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(0L, metrics.getTotalReservations());
    assertEquals(0.0, metrics.getOccupancyRate());
    assertEquals(0L, metrics.getOccupiedDays());
  }

  @Test
  void shouldCalculateOccupancyRateCorrectly_whenAdminQuery() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 10);
    UUID[] units = new UUID[] {UUID.randomUUID(), UUID.randomUUID()};

    Reservation r = new Reservation();
    r.setStartDate(LocalDate.of(2026, 5, 2));
    r.setEndDate(LocalDate.of(2026, 5, 7));

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRange(any(), any())).thenReturn(1L);
    when(reservationRepository.findActiveReservationsInDateRange(any(), any()))
        .thenReturn(List.of(r));

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, null, true);

    assertEquals(1L, metrics.getTotalReservations());
    assertEquals(27.78, metrics.getOccupancyRate());
    assertEquals(5L, metrics.getOccupiedDays());
  }

  @Test
  void shouldCalculateOccupancyRateCorrectly_whenOwnerQuery() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 10);
    UUID[] units = new UUID[] {unitId};

    Reservation r1 = new Reservation();
    r1.setStartDate(LocalDate.of(2026, 4, 25));
    r1.setEndDate(LocalDate.of(2026, 5, 4));

    Reservation r2 = new Reservation();
    r2.setStartDate(LocalDate.of(2026, 5, 8));
    r2.setEndDate(LocalDate.of(2026, 5, 15));

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(2L);
    when(reservationRepository.findActiveReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(List.of(r1, r2));

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(2L, metrics.getTotalReservations());
    assertEquals(55.56, metrics.getOccupancyRate());
    assertEquals(5L, metrics.getOccupiedDays());
  }

  @Test
  void shouldLimitOccupancyTo100Percent_whenReservationsExceedDays() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 5);
    UUID[] units = new UUID[] {unitId};

    Reservation r = new Reservation();
    r.setStartDate(LocalDate.of(2026, 5, 1));
    r.setEndDate(LocalDate.of(2026, 5, 10));

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(1L);
    when(reservationRepository.findActiveReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(List.of(r));

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(1L, metrics.getTotalReservations());
    assertEquals(100.0, metrics.getOccupancyRate());
    assertEquals(4L, metrics.getOccupiedDays());
  }

  @Test
  void shouldFetchDefaultDates_whenDatesAreNull() {
    UUID[] units = new UUID[] {unitId};

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(0L);
    when(reservationRepository.findActiveReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(null, null, ownerId, false);

    assertNotNull(metrics);
    assertEquals(0L, metrics.getTotalReservations());
  }

  @Test
  void shouldSafeguardTotalDays_whenStartEqualsEnd() {
    LocalDate start = LocalDate.of(2026, 5, 1);
    LocalDate end = LocalDate.of(2026, 5, 1);
    UUID[] units = new UUID[] {unitId};

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(units);
    when(reservationRepository.countReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(0L);
    when(reservationRepository.findActiveReservationsInDateRangeForUnits(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    ReservationMetricsDto metrics =
        reservationService.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(0L, metrics.getTotalReservations());
    assertEquals(0.0, metrics.getOccupancyRate());
  }
}
