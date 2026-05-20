package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class AdminOccupancyControllerTest {

  private static Authentication buildAuth(String role, String detailsValue) {
    Authentication auth = mock(Authentication.class);
    GrantedAuthority authority = () -> role;
    doReturn(List.of(authority)).when(auth).getAuthorities();
    when(auth.getDetails()).thenReturn(detailsValue);
    when(auth.isAuthenticated()).thenReturn(true);
    return auth;
  }

  @Test
  void shouldReturnOccupancyForAdmin() {
    ReservationService service = mock(ReservationService.class);
    UUID ownerId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(7);

    OccupancyDto dto =
        new OccupancyDto(UUID.randomUUID(), UUID.randomUUID(), "Unit A", start, end, "CONFIRMED");
    when(service.getOccupancy(start, end, ownerId, true)).thenReturn(List.of(dto));

    Authentication auth = buildAuth("ROLE_ADMIN", ownerId.toString());
    AdminOccupancyController controller = new AdminOccupancyController(service);

    List<OccupancyDto> result = controller.getOccupancy(start, end, auth);

    assertEquals(1, result.size());
    assertEquals("Unit A", result.get(0).getUnitName());
  }

  @Test
  void shouldReturnOccupancyForOwner() {
    ReservationService service = mock(ReservationService.class);
    UUID ownerId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(3);

    OccupancyDto dto =
        new OccupancyDto(UUID.randomUUID(), UUID.randomUUID(), "Unit B", start, end, "PENDING");
    when(service.getOccupancy(start, end, ownerId, false)).thenReturn(List.of(dto));

    Authentication auth = buildAuth("ROLE_OWNER", ownerId.toString());
    AdminOccupancyController controller = new AdminOccupancyController(service);

    List<OccupancyDto> result = controller.getOccupancy(start, end, auth);

    assertEquals(1, result.size());
    assertEquals("PENDING", result.get(0).getStatus());
  }

  @Test
  void shouldThrowForbiddenWhenRoleIsGuest() {
    ReservationService service = mock(ReservationService.class);
    Authentication auth = buildAuth("ROLE_GUEST", UUID.randomUUID().toString());
    AdminOccupancyController controller = new AdminOccupancyController(service);
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.getOccupancy(start, end, auth));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorizedWhenDetailsIsInvalidUuid() {
    ReservationService service = mock(ReservationService.class);
    Authentication auth = buildAuth("ROLE_ADMIN", "not-a-uuid");
    AdminOccupancyController controller = new AdminOccupancyController(service);
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.getOccupancy(start, end, auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorizedWhenDetailsIsBlankString() {
    ReservationService service = mock(ReservationService.class);
    Authentication auth = buildAuth("ROLE_ADMIN", "   ");
    AdminOccupancyController controller = new AdminOccupancyController(service);
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.getOccupancy(start, end, auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorizedWhenDetailsIsNotString() {
    ReservationService service = mock(ReservationService.class);

    Authentication auth = mock(Authentication.class);
    GrantedAuthority authority = () -> "ROLE_ADMIN";
    doReturn(List.of(authority)).when(auth).getAuthorities();
    when(auth.getDetails()).thenReturn(12345); // not a String
    when(auth.isAuthenticated()).thenReturn(true);

    AdminOccupancyController controller = new AdminOccupancyController(service);
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.getOccupancy(start, end, auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorizedWhenDetailsIsNull() {
    ReservationService service = mock(ReservationService.class);

    Authentication auth = mock(Authentication.class);
    GrantedAuthority authority = () -> "ROLE_OWNER";
    doReturn(List.of(authority)).when(auth).getAuthorities();
    when(auth.getDetails()).thenReturn(null);
    when(auth.isAuthenticated()).thenReturn(true);

    AdminOccupancyController controller = new AdminOccupancyController(service);
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> controller.getOccupancy(start, end, auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldSupportMultipleRolesWithAdminTakingPrecedence() {
    ReservationService service = mock(ReservationService.class);
    UUID ownerId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(5);

    when(service.getOccupancy(any(), any(), any(), eq(true))).thenReturn(List.of());

    Authentication auth = mock(Authentication.class);
    Collection<GrantedAuthority> authorities =
        List.of((GrantedAuthority) () -> "ROLE_ADMIN", (GrantedAuthority) () -> "ROLE_OWNER");
    doReturn(authorities).when(auth).getAuthorities();
    when(auth.getDetails()).thenReturn(ownerId.toString());
    when(auth.isAuthenticated()).thenReturn(true);

    AdminOccupancyController controller = new AdminOccupancyController(service);
    List<OccupancyDto> result = controller.getOccupancy(start, end, auth);

    assertNotNull(result);
    verify(service).getOccupancy(start, end, ownerId, true);
  }
}
