package io.github.kwatera_project.kwatera.billing_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@WebMvcTest(AdminDashboardBillingController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardBillingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SettlementRepository settlementRepository;

  @MockitoBean private RestTemplate restTemplate;

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnBillingMetrics() throws Exception {
    UUID resId1 = UUID.randomUUID();
    UUID resId2 = UUID.randomUUID();

    ResponseEntity<AdminDashboardBillingController.ReservationOverviewRecord[]> mockResponseEntity =
        mock(ResponseEntity.class);
    AdminDashboardBillingController.ReservationOverviewRecord[] mockReservations =
        new AdminDashboardBillingController.ReservationOverviewRecord[] {
          new AdminDashboardBillingController.ReservationOverviewRecord(resId1),
          new AdminDashboardBillingController.ReservationOverviewRecord(resId2)
        };
    when(mockResponseEntity.getBody()).thenReturn(mockReservations);
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/admin/reservations"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)))
        .thenReturn((ResponseEntity) mockResponseEntity);

    when(settlementRepository.sumRevenueForReservationsInPeriod(
            any(), any(Instant.class), any(Instant.class)))
        .thenReturn(new BigDecimal("1250.00"));
    when(settlementRepository.sumUnpaidBalanceForReservationsInPeriod(
            any(), any(Instant.class), any(Instant.class)))
        .thenReturn(new BigDecimal("450.00"));
    when(settlementRepository.countByStatusForReservationsInPeriod(
            any(), eq(SettlementStatus.PAID), any(Instant.class), any(Instant.class)))
        .thenReturn(5L);
    when(settlementRepository.countUnpaidSettlementsForReservationsInPeriod(
            any(), any(Instant.class), any(Instant.class)))
        .thenReturn(2L);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing")
                .header("Authorization", "Bearer mock-token")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revenueFromSettlements").value(1250.00))
        .andExpect(jsonPath("$.unpaidBalance").value(450.00))
        .andExpect(jsonPath("$.paidSettlementsCount").value(5))
        .andExpect(jsonPath("$.unpaidSettlementsCount").value(2));
  }

  @Test
  void shouldFail_whenAuthorizationHeaderMissing() throws Exception {
    mockMvc.perform(get("/api/v1/admin/dashboard/billing")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenAuthorizationHeaderDoesNotStartWithBearer() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing").header("Authorization", "Basic credentials"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFail_whenRestTemplateThrowsException() throws Exception {
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/admin/reservations"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)))
        .thenThrow(new RuntimeException("Connection refused"));

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing").header("Authorization", "Bearer mock-token"))
        .andExpect(status().isBadGateway());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnZeroMetrics_whenNoReservationsFound() throws Exception {
    ResponseEntity<AdminDashboardBillingController.ReservationOverviewRecord[]> mockResponseEntity =
        mock(ResponseEntity.class);
    when(mockResponseEntity.getBody()).thenReturn(null);
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/admin/reservations"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)))
        .thenReturn((ResponseEntity) mockResponseEntity);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing").header("Authorization", "Bearer mock-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revenueFromSettlements").value(0))
        .andExpect(jsonPath("$.unpaidBalance").value(0))
        .andExpect(jsonPath("$.paidSettlementsCount").value(0))
        .andExpect(jsonPath("$.unpaidSettlementsCount").value(0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFail_whenStartDateAfterEndDate() throws Exception {
    UUID resId = UUID.randomUUID();
    ResponseEntity<AdminDashboardBillingController.ReservationOverviewRecord[]> mockResponseEntity =
        mock(ResponseEntity.class);
    AdminDashboardBillingController.ReservationOverviewRecord[] mockReservations =
        new AdminDashboardBillingController.ReservationOverviewRecord[] {
          new AdminDashboardBillingController.ReservationOverviewRecord(resId)
        };
    when(mockResponseEntity.getBody()).thenReturn(mockReservations);
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/admin/reservations"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)))
        .thenReturn((ResponseEntity) mockResponseEntity);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing")
                .header("Authorization", "Bearer mock-token")
                .param("startDate", "2026-05-31")
                .param("endDate", "2026-05-01"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnMetricsWithDefaultDates_whenDatesNotProvided() throws Exception {
    UUID resId = UUID.randomUUID();
    ResponseEntity<AdminDashboardBillingController.ReservationOverviewRecord[]> mockResponseEntity =
        mock(ResponseEntity.class);
    AdminDashboardBillingController.ReservationOverviewRecord[] mockReservations =
        new AdminDashboardBillingController.ReservationOverviewRecord[] {
          new AdminDashboardBillingController.ReservationOverviewRecord(resId)
        };
    when(mockResponseEntity.getBody()).thenReturn(mockReservations);
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/admin/reservations"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(Class.class)))
        .thenReturn((ResponseEntity) mockResponseEntity);

    when(settlementRepository.sumRevenueForReservationsInPeriod(any(), any(), any()))
        .thenReturn(BigDecimal.ZERO);
    when(settlementRepository.sumUnpaidBalanceForReservationsInPeriod(any(), any(), any()))
        .thenReturn(BigDecimal.ZERO);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/billing").header("Authorization", "Bearer mock-token"))
        .andExpect(status().isOk());
  }
}
