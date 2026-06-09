# Sprint 19 - Migraciones y persistencia

## Objetivo

Consolidar la persistencia de `catalog-service` con migraciones versionadas, reduciendo la dependencia de `spring.jpa.hibernate.ddl-auto=update` sin romper el flujo local ni los sprints anteriores.

## Politica aplicada

- `booking-service` ya contaba con Flyway y migraciones reales.
- `catalog-service` era el siguiente servicio con mayor valor operativo para migrar porque concentra sucursales, items, inventario y holds.
- `auth-service` queda pendiente porque requiere una migracion mas cuidadosa de usuarios, roles, provider requests y datos demo.
- No se agregaron relaciones nuevas ni cambios de dominio: las migraciones reflejan el modelo JPA existente.

## Cambios de persistencia

- Se agrego Flyway a `catalog-service`.
- Se creo una baseline versionada para `branches`, `rentable_items`, `inventory` e `inventory_holds`.
- Se agregaron indices para consultas publicas/provider e inventory holds.
- El perfil default/dev mantiene `ddl-auto=update` para compatibilidad local.
- El perfil `prod` usa `ddl-auto=validate`.
- El perfil `test` queda preparado para Flyway + validacion contra PostgreSQL real.

## Criterio de compatibilidad

Las migraciones usan `CREATE TABLE IF NOT EXISTS` e indices idempotentes para que una base local creada previamente por Hibernate pueda adoptar Flyway con menor friccion.

`spring.flyway.baseline-on-migrate=true` queda habilitado en default/dev/test para facilitar la transicion de esquemas locales existentes. En `prod` queda deshabilitado para evitar baselines accidentales sobre una base incorrecta.

## Testing agregado

Se agrego una prueba de integracion con Testcontainers que ejecuta las migraciones de `catalog-service` sobre PostgreSQL real y verifica tablas, columnas e indices clave.

## Pendiente

- Migrar `auth-service` a Flyway.
- Revisar seeders hardcodeados y datos demo por profile.
- Evaluar pasar `catalog-service` a `ddl-auto=validate` tambien en dev cuando el equipo ya no dependa de updates automaticos.
- Corregir la integracion protegida booking -> catalog inventory hold/release en el sprint de seguridad.
