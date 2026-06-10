package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemEventService {

  private static final Logger log = LoggerFactory.getLogger(SystemEventService.class);
  public static final String ENTITY_TYPE_RESERVATION = "RESERVATION";
  private static final int DEFAULT_LIMIT = 100;
  private static final int MAX_LIMIT = 500;

  private final SystemEventRepository systemEventRepository;

  @Transactional(readOnly = true)
  public List<SystemEventResponseDto> getLatestEvents(
      SystemEventType actionType, Integer limit, Instant from, Instant to) {
    int effectiveLimit = Math.min(Math.max(limit == null ? DEFAULT_LIMIT : limit, 1), MAX_LIMIT);
    PageRequest pageRequest =
        PageRequest.of(0, effectiveLimit, Sort.by(Sort.Direction.DESC, "timestamp"));

    if (actionType != null) {
      if (from != null && to != null) {
        return systemEventRepository
            .findByActionTypeAndTimestampBetween(actionType, from, to, pageRequest)
            .stream()
            .map(this::toResponse)
            .toList();
      }
      if (from != null) {
        return systemEventRepository
            .findByActionTypeAndTimestampGreaterThanEqual(actionType, from, pageRequest)
            .stream()
            .map(this::toResponse)
            .toList();
      }
      if (to != null) {
        return systemEventRepository
            .findByActionTypeAndTimestampLessThanEqual(actionType, to, pageRequest)
            .stream()
            .map(this::toResponse)
            .toList();
      }
      return systemEventRepository.findByActionType(actionType, pageRequest).stream()
          .map(this::toResponse)
          .toList();
    }

    if (from != null && to != null) {
      return systemEventRepository.findByTimestampBetween(from, to, pageRequest).stream()
          .map(this::toResponse)
          .toList();
    }
    if (from != null) {
      return systemEventRepository.findByTimestampGreaterThanEqual(from, pageRequest).stream()
          .map(this::toResponse)
          .toList();
    }
    if (to != null) {
      return systemEventRepository.findByTimestampLessThanEqual(to, pageRequest).stream()
          .map(this::toResponse)
          .toList();
    }

    return systemEventRepository.findAll(pageRequest).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<SystemEventResponseDto> getLatestEvents(SystemEventType actionType, Integer limit) {
    return getLatestEvents(actionType, limit, null, null);
  }

  public void logSafely(
      SystemEventType actionType,
      UUID actorUserId,
      String entityType,
      UUID entityId,
      String details) {
    try {
      SystemEvent event = new SystemEvent();
      event.setTimestamp(Instant.now());
      event.setActionType(actionType);
      event.setActorUserId(actorUserId);
      event.setEntityType(entityType);
      event.setEntityId(entityId);
      event.setDetails(details);
      systemEventRepository.save(event);
    } catch (Exception e) {
      log.warn("Failed to persist system event {}", actionType, e);
    }
  }

  private SystemEventResponseDto toResponse(SystemEvent event) {
    return new SystemEventResponseDto(
        event.getId(),
        event.getTimestamp(),
        event.getActionType(),
        event.getActorUserId(),
        event.getEntityType(),
        event.getEntityId(),
        event.getDetails());
  }
}
