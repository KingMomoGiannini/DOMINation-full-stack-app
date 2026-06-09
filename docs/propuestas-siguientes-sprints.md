# Propuestas de continuidad y siguientes sprints

Este documento presenta caminos posibles para continuar DOMINation y registra la decision actual para el Sprint 19. Las propuestas buscan equilibrar valor de portfolio, valor de producto real y aprendizaje tecnico.

## Caminos de continuidad

### Camino portfolio

**Objetivo**: pulir DOMINation como ejemplo completo de arquitectura de microservicios para portfolio o CV.

**Acciones recomendadas**:

- Documentar decisiones arquitectonicas reales y mantener los docs alineados con el codigo.
- Incorporar migraciones versionadas en todos los servicios.
- Mejorar tests de integracion y contratos entre servicios.
- Provisionar dashboards de Grafana como codigo.
- Preparar una demo reproducible con Docker Compose.

### Camino producto real

**Objetivo**: evolucionar DOMINation hacia un producto comercial para salas de ensayo y musicos.

**Acciones recomendadas**:

- Fortalecer el modelo multi-provider y evitar acoplamientos por IDs demo.
- Integrar pagos cuando el flujo de reserva este mas estable.
- Agregar notificaciones para confirmaciones, recordatorios y no-show.
- Endurecer seguridad service-to-service.
- Definir politicas operativas mas completas solo cuando aporten valor real.

### Camino estudio academico/tecnico

**Objetivo**: usar DOMINation como base para aprender y experimentar con patrones avanzados.

**Acciones recomendadas**:

- Explorar transacciones distribuidas o Sagas sobre reservas e inventario.
- Evaluar comunicacion asincronica con eventos.
- Agregar trazabilidad distribuida con OpenTelemetry.
- Comparar estrategias de persistencia y migraciones entre servicios.
- Medir impacto de indices, paginacion y consultas reales.

## Propuesta de Sprint 19 - Migraciones y persistencia

Tras revisar el estado real del repositorio, se decide continuar con un Sprint 19 enfocado en persistencia. La seguridad y la observabilidad siguen siendo caminos relevantes, pero el primer paso elegido es reducir el riesgo de drift entre JPA y PostgreSQL.

### Objetivo

Consolidar el esquema de base de datos con Flyway, empezando por `catalog-service`, sin romper la operacion local ni los contratos existentes.

### Alcance y tareas

| Nro | Tarea | Criterios de aceptacion |
|---|---|---|
| 1 | Agregar Flyway a `catalog-service`. | El servicio compila y Flyway queda activo sin romper el arranque local. |
| 2 | Crear baseline de catalogo. | Las tablas `branches`, `rentable_items`, `inventory` e `inventory_holds` se crean desde migraciones. |
| 3 | Agregar indices operativos. | Las consultas publicas/provider e inventory holds tienen indices versionados. |
| 4 | Separar perfiles. | Default/dev mantienen compatibilidad local y `prod` valida el esquema con Hibernate. |
| 5 | Preparar testing de esquema. | Testcontainers ejecuta Flyway sobre PostgreSQL real y verifica tablas/columnas/indices. |
| 6 | Documentar decisiones. | Queda claro por que `auth-service` se migra despues y que no se cambio el dominio. |

### Criterios de aceptacion generales

- El trabajo se realiza en la rama `sprint-19-migraciones-persistencia`.
- `catalog-service` tiene migraciones versionadas.
- Los tests existentes se mantienen.
- La documentacion refleja Flyway, perfiles y pendientes de persistencia.
- No se introducen cambios de endpoints ni de reglas de negocio.

## Siguientes candidatos despues del Sprint 19

### Seguridad y JWT

Resolver la integracion protegida booking -> catalog para inventory hold/release, externalizar secretos y documentar el contrato JWT real.

### Observabilidad y trazabilidad

Agregar dashboards provisionados, metricas de negocio y reglas Prometheus basicas.

### Flyway en auth-service

Migrar usuarios, roles y provider requests con cuidado sobre datos demo y credenciales hardcodeadas.
