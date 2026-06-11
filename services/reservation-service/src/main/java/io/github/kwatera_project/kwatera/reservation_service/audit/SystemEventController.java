package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequiredArgsConstructor
public class SystemEventController {

  private final SystemEventService systemEventService;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String expectedInternalToken;

  @GetMapping("/api/v1/admin/system-events")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public List<SystemEventResponseDto> getSystemEvents(
      @RequestParam(name = "actionType", required = false) SystemEventType actionType,
      @RequestParam(name = "limit", required = false) Integer limit,
      @RequestParam(name = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(name = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return systemEventService.getLatestEvents(actionType, limit, from, to);
  }

  @PostMapping("/api/v1/internal/system-events")
  public void createInternalSystemEvent(
      @RequestBody InternalSystemEventRequest request,
      @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
    if (!Objects.equals(internalToken, expectedInternalToken)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: Invalid internal token");
    }
    systemEventService.logSafely(
        request.actionType(),
        request.actorUserId(),
        request.entityType(),
        request.entityId(),
        request.details());
  }
}
