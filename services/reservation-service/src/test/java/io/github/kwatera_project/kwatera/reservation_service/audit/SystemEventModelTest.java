package io.github.kwatera_project.kwatera.reservation_service.audit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SystemEventModelTest {

  @Test
  void systemEventAccessorsRoundTripValues() {
    UUID id = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    Instant timestamp = Instant.parse("2026-06-10T12:00:00Z");

    SystemEvent event = new SystemEvent();
    event.setId(id);
    event.setTimestamp(timestamp);
    event.setActionType(SystemEventType.RESERVATION_STATUS_CHANGED);
    event.setActorUserId(actorUserId);
    event.setEntityType("RESERVATION");
    event.setEntityId(entityId);
    event.setDetails("oldStatus=PENDING, newStatus=CONFIRMED");

    assertEquals(id, event.getId());
    assertEquals(timestamp, event.getTimestamp());
    assertEquals(SystemEventType.RESERVATION_STATUS_CHANGED, event.getActionType());
    assertEquals(actorUserId, event.getActorUserId());
    assertEquals("RESERVATION", event.getEntityType());
    assertEquals(entityId, event.getEntityId());
    assertEquals("oldStatus=PENDING, newStatus=CONFIRMED", event.getDetails());
  }

  @Test
  void responseDtoAccessorsExposeConstructorValues() {
    UUID id = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    Instant timestamp = Instant.parse("2026-06-10T12:00:00Z");

    SystemEventResponseDto dto =
        new SystemEventResponseDto(
            id,
            timestamp,
            SystemEventType.UNIT_BLOCKED,
            actorUserId,
            "RESERVATION",
            entityId,
            "unit blocked");

    assertEquals(id, dto.id());
    assertEquals(timestamp, dto.timestamp());
    assertEquals(SystemEventType.UNIT_BLOCKED, dto.actionType());
    assertEquals(actorUserId, dto.actorUserId());
    assertEquals("RESERVATION", dto.entityType());
    assertEquals(entityId, dto.entityId());
    assertEquals("unit blocked", dto.details());
  }

  @Test
  void systemEventTypeContainsExpectedValuesInOrder() {
    assertArrayEquals(
        new SystemEventType[] {
          SystemEventType.RESERVATION_CREATED,
          SystemEventType.MANUAL_RESERVATION_CREATED,
          SystemEventType.UNIT_BLOCKED,
          SystemEventType.RESERVATION_STATUS_CHANGED,
          SystemEventType.EXPIRED_RESERVATION_CANCELLED
        },
        SystemEventType.values());
  }
}
