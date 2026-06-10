package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/admin/system-events")
@RequiredArgsConstructor
public class SystemEventController {

  private final SystemEventService systemEventService;

  @GetMapping
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public List<SystemEventResponseDto> getSystemEvents(
      @RequestParam(name = "actionType", required = false) SystemEventType actionType,
      @RequestParam(name = "limit", required = false) Integer limit) {
    return systemEventService.getLatestEvents(actionType, limit);
  }
}
