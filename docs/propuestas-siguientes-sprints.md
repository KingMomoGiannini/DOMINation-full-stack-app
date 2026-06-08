# Propuestas de continuidad y siguientes sprints

Este documento presenta distintos caminos para continuar el desarrollo de DOMINation, así como una propuesta concreta para el siguiente sprint numerado según la secuencia actual del proyecto. Las propuestas se basan en la revisión técnica y buscan equilibrar objetivos de portfolio, potencial comercial y valor educativo.

## Caminos de continuidad

### Camino portfolio

**Objetivo**: pulir DOMINation para que sirva como ejemplo completo de arquitectura de microservicios en un portfolio o CV.

**Acciones recomendadas**:

- Unificar todas las dependencias a la misma versión de Spring Boot y Spring Cloud para simplificar el mantenimiento.
- Incorporar scripts de migración (Flyway) en todos los servicios y proporcionar datos de ejemplo para demostraciones.
- Mejorar la documentación: actualizar el README, crear diagramas de arquitectura y escribir una guía de despliegue paso a paso.
- Incluir pruebas de integración y end‑to‑end con herramientas como Testcontainers y Cypress.
- Preparar un entorno de demostración desplegado (por ejemplo, en Heroku o Fly.io) accesible mediante una URL pública.

### Camino producto real

**Objetivo**: evolucionar DOMINation hacia un producto comercial para estudios de música y músicos.

**Acciones recomendadas**:

- Rediseñar el dominio para soportar un modelo multi‑tenant donde cada estudio esté aislado, permitiendo que varios proveedores convivan sin interferencia.
- Integrar una pasarela de pagos (por ejemplo, MercadoPago o Stripe) para gestionar cobros de reservas y comisiones.
- Añadir un microservicio de notificaciones (correo electrónico y SMS) para enviar confirmaciones, recordatorios y alertas de no‑show.
- Implementar versionado de APIs y estrategias de despliegue azul‑verde o canario para minimizar tiempo de inactividad.
- Refactorizar la seguridad para externalizar el Authorization Server (por ejemplo, integrando Keycloak o Auth0) y permitir flujos de inicio de sesión social.

### Camino estudio académico/técnico

**Objetivo**: utilizar DOMINation como base para experimentar con patrones avanzados y nuevas tecnologías.

**Acciones recomendadas**:

- Implementar patrones de transacción distribuida como *Saga* (orchestrated o choreographed) y estudiar sus implicaciones en la gestión de reservas y pagos.
- Migrar la comunicación entre servicios a un bus de eventos (Kafka, RabbitMQ) para explorar la arquitectura orientada a eventos.
- Añadir un gateway GraphQL para comparar su uso frente al API Gateway REST y exponer una capa de agregación de datos.
- Probar otras bases de datos (MongoDB, Neo4j) en servicios específicos y medir su impacto en el rendimiento.
- Integrar herramientas de trazabilidad distribuida (OpenTelemetry + Jaeger) y estudiar cómo facilitan el debugging en microservicios.

## Propuesta de Sprint 19 – Consolidación de seguridad y observabilidad

Tras revisar que el repositorio llega hasta el **Sprint 18**, se propone continuar con el **Sprint 19**, enfocado en mejorar la seguridad y la observabilidad. Este sprint servirá como puente entre la versión actual y los caminos de continuidad.

### Objetivo

Reforzar la seguridad y la observabilidad del sistema para acercarlo a un entorno real, unificar las versiones de dependencias y preparar la base para futuras evoluciones.

### Alcance y tareas

| Nº | Tarea | Criterios de aceptación |
|---|---|---|
| 1 | **Unificar versiones de Spring Boot**: actualizar todos los microservicios y el gateway a la misma versión (por ejemplo, 4.0.x) y ajustar las dependencias relacionadas. | Todos los servicios compilan y arrancan correctamente. No hay incompatibilidades de dependencia. |
| 2 | **Implementar migraciones Flyway** en `booking-service`: crear scripts para las tablas `reservations` y `reservation_lines` y eliminar `spring.jpa.hibernate.ddl-auto=update`. | Al ejecutar `mvn flyway:migrate`, las tablas se crean y el servicio se inicia con `ddl-auto=validate`. |
| 3 | **Externalizar secretos**: mover `clientSecret` y claves RSA a variables de entorno o Docker secrets. Actualizar la configuración para leerlos mediante `@Value`. | El código no contiene secretos en texto plano y el sistema funciona con las nuevas variables. |
| 4 | **Simplificar la gestión de roles**: unificar el claim de roles (usando solo `authorities`) y actualizar los servicios que leen claims múltiples. | Todos los controladores autorizan correctamente y la documentación refleja el nuevo esquema de JWT. |
| 5 | **Propagar `requestId` en todos los servicios**: añadir un filtro `RequestIdMdcFilter` al catalog y booking que lea la cabecera `X‑Request‑Id` y la introduzca en el MDC. | Los logs muestran el identificador de solicitud y se puede seguir una petición completa a través de los servicios. |
| 6 | **Configurar alertas básicas**: agregar Alertmanager al docker‑compose y definir al menos dos reglas de alerta (error rate alto y uso elevado de memoria). | Al generar errores 5xx o elevar el consumo de memoria, Prometheus dispara alertas visibles en Alertmanager. |

### Criterios de aceptación generales

- Todas las tareas se realizan en una rama dedicada (por ejemplo, `sprint-19-security-observability`) y se fusionan a `main` mediante pull request.
- El proyecto se ejecuta sin errores en local utilizando `docker-compose up` y se mantienen los tests existentes.
- La documentación se actualiza para reflejar los cambios (por ejemplo, secret management, estructura de JWT, nuevas alertas).

---

Estas propuestas proporcionan un camino claro para continuar con el desarrollo, ya sea como proyecto demostrativo, producto comercial o plataforma de experimentación académica. El Sprint 19 sienta las bases para estas metas al reforzar aspectos críticos de la aplicación.