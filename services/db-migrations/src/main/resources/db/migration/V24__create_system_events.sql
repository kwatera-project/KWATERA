CREATE TABLE system_events
(
    id            UUID PRIMARY KEY,
    timestamp     TIMESTAMP   NOT NULL,
    action_type   VARCHAR(80) NOT NULL,
    actor_user_id UUID,
    entity_type   VARCHAR(80),
    entity_id     UUID,
    details       TEXT
);

CREATE INDEX idx_system_events_timestamp ON system_events (timestamp);
CREATE INDEX idx_system_events_action_type ON system_events (action_type);
CREATE INDEX idx_system_events_actor_user_id ON system_events (actor_user_id);
