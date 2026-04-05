-- Sprint 8/9: columnas de snapshot legible para listados (idempotente si la tabla ya existe vía Hibernate).
-- Solo ejecuta ALTER si la tabla existe (bases vacías: sin-op hasta que Hibernate cree el esquema en dev).

DO $$
BEGIN
    IF to_regclass('public.reservations') IS NOT NULL THEN
        ALTER TABLE reservations ADD COLUMN IF NOT EXISTS branch_name VARCHAR(255);
    END IF;
    IF to_regclass('public.reservation_lines') IS NOT NULL THEN
        ALTER TABLE reservation_lines ADD COLUMN IF NOT EXISTS item_name VARCHAR(255);
    END IF;
END $$;
