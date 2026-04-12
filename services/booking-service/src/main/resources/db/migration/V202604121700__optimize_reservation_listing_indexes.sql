-- Sprint 14: índices compuestos para listados paginados y lectura histórica por cliente.
-- Se priorizan los patrones reales introducidos en Sprint 13:
--   1) my reservations -> customer_id + orden por start_at/id
--   2) provider reservations -> provider_id + orden por start_at/id
--   3) provider reservations filtrado por sucursal -> provider_id + branch_id + orden por start_at/id
-- Los índices simples por customer/provider quedan cubiertos por los prefijos de estos compuestos.

CREATE INDEX IF NOT EXISTS idx_reservations_customer_start_id
    ON reservations (customer_id, start_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_reservations_provider_start_id
    ON reservations (provider_id, start_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_reservations_provider_branch_start_id
    ON reservations (provider_id, branch_id, start_at DESC, id DESC);

DROP INDEX IF EXISTS idx_reservations_customer_id;
DROP INDEX IF EXISTS idx_reservations_provider_id;
