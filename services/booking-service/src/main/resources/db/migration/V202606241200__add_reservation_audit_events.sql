CREATE TABLE IF NOT EXISTS reservation_audit_events (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    actor_user_id VARCHAR(255) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    reason VARCHAR(255),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_reservation_id
    ON reservation_audit_events (reservation_id);

CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_reservation_created_at
    ON reservation_audit_events (reservation_id, created_at);
