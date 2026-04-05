-- Sprint 10: baseline explícito del núcleo booking (idempotente con IF NOT EXISTS).
-- Corre después de V202604051200; en BD nueva, la migración anterior no crea tablas (sin-op),
-- y esta define el esquema completo alineado con JPA.

CREATE TABLE IF NOT EXISTS reservations (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     VARCHAR(255) NOT NULL,
    branch_id       BIGINT NOT NULL,
    branch_name     VARCHAR(255),
    provider_id     BIGINT,
    start_at        TIMESTAMP NOT NULL,
    end_at          TIMESTAMP NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation_lines (
    id               BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    item_id          BIGINT NOT NULL,
    item_name        VARCHAR(255),
    quantity         INTEGER NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    hold_id          VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_reservation_lines_reservation_id ON reservation_lines (reservation_id);
CREATE INDEX IF NOT EXISTS idx_reservations_customer_id ON reservations (customer_id);
CREATE INDEX IF NOT EXISTS idx_reservations_provider_id ON reservations (provider_id);
