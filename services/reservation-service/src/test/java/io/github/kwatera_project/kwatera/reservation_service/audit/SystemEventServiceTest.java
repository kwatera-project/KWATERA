package io.github.kwatera_project.kwatera.reservation_service.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class SystemEventServiceTest {

  private final SystemEventRepository systemEventRepository =
      org.mockito.Mockito.mock(SystemEventRepository.class);

  private final SystemEventService systemEventService =
      new SystemEventService(systemEventRepository);

  @Test
  void getLatestEventsWithoutActionTypeUsesDefaultLimitAndMapsResults() {
    SystemEvent event =
        systemEvent(
            SystemEventType.RESERVATION_CREATED,
            UUID.randomUUID(),
            "RESERVATION",
            UUID.randomUUID(),
            "reservation created");
    when(systemEventRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));

    List<SystemEventResponseDto> result = systemEventService.getLatestEvents(null, null);

    assertEquals(1, result.size());
    assertDtoMatchesEvent(event, result.getFirst());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(systemEventRepository).findAll(pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(0, pageable.getPageNumber());
    assertEquals(100, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("timestamp").getDirection());
    verify(systemEventRepository, never()).findByActionType(any(), any());
  }

  @Test
  void getLatestEventsWithActionTypeUsesActionFilterAndMapsResults() {
    SystemEvent event =
        systemEvent(
            SystemEventType.UNIT_BLOCKED,
            UUID.randomUUID(),
            "RESERVATION",
            UUID.randomUUID(),
            "unit blocked");
    when(systemEventRepository.findByActionType(
            org.mockito.ArgumentMatchers.eq(SystemEventType.UNIT_BLOCKED), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));

    List<SystemEventResponseDto> result =
        systemEventService.getLatestEvents(SystemEventType.UNIT_BLOCKED, 50);

    assertEquals(1, result.size());
    assertDtoMatchesEvent(event, result.getFirst());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(systemEventRepository)
        .findByActionType(
            org.mockito.ArgumentMatchers.eq(SystemEventType.UNIT_BLOCKED),
            pageableCaptor.capture());
    assertEquals(50, pageableCaptor.getValue().getPageSize());
  }

  @Test
  void getLatestEventsClampsLimitToAtLeastOne() {
    when(systemEventRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    systemEventService.getLatestEvents(null, 0);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(systemEventRepository).findAll(pageableCaptor.capture());
    assertEquals(1, pageableCaptor.getValue().getPageSize());
  }

  @Test
  void getLatestEventsClampsLimitToMaxLimit() {
    when(systemEventRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    systemEventService.getLatestEvents(null, 999);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(systemEventRepository).findAll(pageableCaptor.capture());
    assertEquals(500, pageableCaptor.getValue().getPageSize());
  }

  @Test
  void logSafelyPersistsSystemEventWithExpectedFields() {
    UUID actorUserId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();

    systemEventService.logSafely(
        SystemEventType.MANUAL_RESERVATION_CREATED,
        actorUserId,
        "RESERVATION",
        entityId,
        "manual reservation");

    ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
    verify(systemEventRepository).save(eventCaptor.capture());
    SystemEvent event = eventCaptor.getValue();
    assertNotNull(event.getTimestamp());
    assertEquals(SystemEventType.MANUAL_RESERVATION_CREATED, event.getActionType());
    assertEquals(actorUserId, event.getActorUserId());
    assertEquals("RESERVATION", event.getEntityType());
    assertEquals(entityId, event.getEntityId());
    assertEquals("manual reservation", event.getDetails());
  }

  @Test
  void logSafelyCatchesRepositoryExceptions() {
    doThrow(new RuntimeException("database unavailable"))
        .when(systemEventRepository)
        .save(any(SystemEvent.class));

    assertDoesNotThrow(
        () ->
            systemEventService.logSafely(
                SystemEventType.EXPIRED_RESERVATION_CANCELLED,
                null,
                "RESERVATION",
                UUID.randomUUID(),
                "expired reservation"));
  }

  private SystemEvent systemEvent(
      SystemEventType actionType,
      UUID actorUserId,
      String entityType,
      UUID entityId,
      String details) {
    SystemEvent event = new SystemEvent();
    event.setId(UUID.randomUUID());
    event.setTimestamp(Instant.parse("2026-06-10T12:00:00Z"));
    event.setActionType(actionType);
    event.setActorUserId(actorUserId);
    event.setEntityType(entityType);
    event.setEntityId(entityId);
    event.setDetails(details);
    return event;
  }

  private void assertDtoMatchesEvent(SystemEvent event, SystemEventResponseDto dto) {
    assertEquals(event.getId(), dto.id());
    assertEquals(event.getTimestamp(), dto.timestamp());
    assertEquals(event.getActionType(), dto.actionType());
    assertEquals(event.getActorUserId(), dto.actorUserId());
    assertEquals(event.getEntityType(), dto.entityType());
    assertEquals(event.getEntityId(), dto.entityId());
    assertEquals(event.getDetails(), dto.details());
  }
}
