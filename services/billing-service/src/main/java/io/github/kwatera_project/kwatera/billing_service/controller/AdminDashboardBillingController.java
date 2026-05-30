package io.github.kwatera_project.kwatera.billing_service.controller;

import io.github.kwatera_project.kwatera.billing_service.dto.BillingMetricsDto;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardBillingController {

  private final SettlementRepository settlementRepository;
  private final RestTemplate restTemplate;

  record ReservationOverviewRecord(UUID id) {}

  @GetMapping("/billing")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  public BillingMetricsDto getBillingMetrics(
      @RequestParam(name = "startDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(name = "endDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      HttpServletRequest request) {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", authHeader);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    String url = "http://reservation-service/api/v1/admin/reservations";
    ResponseEntity<ReservationOverviewRecord[]> response;
    try {
      response =
          restTemplate.exchange(url, HttpMethod.GET, entity, ReservationOverviewRecord[].class);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Cannot fetch reservations: " + e.getMessage());
    }

    ReservationOverviewRecord[] reservations = response.getBody();
    if (reservations == null || reservations.length == 0) {
      return new BillingMetricsDto(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
    }

    List<UUID> reservationIds =
        java.util.Arrays.stream(reservations).map(ReservationOverviewRecord::id).toList();

    LocalDate startLocal = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
    LocalDate endLocal =
        (endDate != null)
            ? endDate
            : LocalDate.now().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

    if (startLocal.isAfter(endLocal)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
    }

    Instant start = startLocal.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    Instant end = endLocal.atTime(23, 59, 59, 999999999).toInstant(java.time.ZoneOffset.UTC);

    BigDecimal revenue =
        settlementRepository.sumRevenueForReservationsInPeriod(reservationIds, start, end);
    BigDecimal unpaid =
        settlementRepository.sumUnpaidBalanceForReservationsInPeriod(reservationIds, start, end);
    long paidCount =
        settlementRepository.countByStatusForReservationsInPeriod(
            reservationIds,
            io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.PAID,
            start,
            end);
    long unpaidCount =
        settlementRepository.countUnpaidSettlementsForReservationsInPeriod(
            reservationIds, start, end);

    return new BillingMetricsDto(revenue, unpaid, paidCount, unpaidCount);
  }
}
