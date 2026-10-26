UPDATE reservations r
SET cancelled_at = cancellation.cancelled_at
FROM (
    SELECT entity_id AS reservation_id, MIN(timestamp) AS cancelled_at
    FROM system_events
    WHERE entity_type = 'RESERVATION'
      AND (
        action_type = 'EXPIRED_RESERVATION_CANCELLED'
        OR (
          action_type = 'RESERVATION_STATUS_CHANGED'
          AND details LIKE '%newStatus=CANCELLED%'
          AND details NOT LIKE '%oldStatus=CANCELLED%'
        )
      )
    GROUP BY entity_id
) cancellation
WHERE r.id = cancellation.reservation_id
  AND r.status = 'CANCELLED'
  AND r.cancelled_at IS NULL;
