# Contexto técnico actual

Este documento resume la situación técnica del proyecto **DOMINation** a junio de 2026. Su objetivo es ofrecer una visión actualizada de la arquitectura, las tecnologías empleadas, los servicios y flujos principales, así como la deuda técnica y oportunidades de mejora detectadas.

## Estructura del repositorio

DOMINation se organiza como un monorepo con **arquitectura de microservicios**. A alto nivel, las carpetas más relevantes son:

- **gateway/**: API Gateway basado en Spring Cloud Gateway. Define rutas hacia los servicios de negocio, configura CORS global y aplica filtros para generar y propagar un identificador de solicitud. Provee un punto de entrada único para el frontend.
- **services/**: contiene los microservicios de dominio. Cada servicio incluye sus propios paquetes `domain`, `repository`, `dto`, `service`, `controller` y `config`.
  - **auth‑service**: implementa un *Authorization Server* y un *Resource Server* con Spring Authorization Server. Expone endpoints de registro y login, persiste usuarios/roles y emite tokens JWT. También actúa como API interna para consultar usuarios y roles.
  - **catalog‑service**: gestiona sucursales, salas e inventario. Proporciona endpoints públicos para consultar sucursales e ítems y endpoints protegidos para que los proveedores administren sus recursos. Utiliza JPA para mapear entidades como `Branch`, `RentableItem` e `Inventory`.
  - **booking‑service**: gestiona reservas. Valida disponibilidad consultando el catálogo, crea reservas y líneas de reserva, y permite cancelar o consultar reservas. Utiliza JPA y validaciones para modelar `Reservation` y `ReservationLine`.
- **frontend/**: aplicación React + TypeScript construida con Vite. Consume las APIs del gateway, maneja la autenticación mediante OIDC e implementa la interfaz de usuario.
- **infra/**: scripts y configuraciones de infraestructura: archivos Dockerfile, `docker-compose.yml`, configuración de Prometheus/Grafana y scripts de depuración. Permite levantar todo el stack localmente con un solo comando.
- **docs/**: documentación técnica y guías de usuario. Incluye manuales de instalación local, observabilidad, descripción de roles y sprints anteriores.

## Tecnologías empleadas

El proyecto utiliza una pila moderna que combina **Java 21**, **Spring Boot 3.x/4.x** y **Spring Cloud** en el backend, con **React 18 + TypeScript** en el frontend. Otras dependencias clave son:

- **Persistencia y JPA**: Spring Data JPA e Hibernate para mapear entidades y repositorios. PostgreSQL como base de datos, con una instancia por microservicio. 
- **Seguridad**: Spring Security y Spring Authorization Server para implementar OAuth 2.1, JWT y control de acceso. Los tokens se firman con claves RSA y contienen claims de `userId` y `authorities` para roles de usuario.
- **API Gateway**: Spring Cloud Gateway enruta solicitudes del frontend a los servicios de backend, añade cabeceras y aplica filtros. Simplifica el manejo de CORS y la propagación del `requestId`.
- **Contenedores**: Docker y Docker Compose orquestan bases de datos, microservicios, Prometheus y Grafana para levantar el entorno completo de desarrollo.
- **Observabilidad**: Spring Boot Actuator y Micrometer exponen métricas estándar; Prometheus las recolecta y Grafana permite visualizarlas en dashboards. Cada servicio expone `/actuator/health` y `/actuator/prometheus`.

## Servicios y dependencias

Cada microservicio sigue una arquitectura en capas, con responsabilidades bien definidas:

- **Auth Service**
  - *Responsabilidad*: servir como servidor de autorización y recursos. Gestiona usuarios, roles y permisos. Emite y valida JWT. 
  - *Dependencias*: Spring Authorization Server, Spring Security, Spring Data JPA, BCrypt para el hashing de contraseñas. Se comunica con la base de datos de autenticación.

- **Catalog Service**
  - *Responsabilidad*: administrar el catálogo de sucursales y salas. Ofrece endpoints públicos (`GET /api/catalog/branches`, `GET /api/catalog/items`) y endpoints protegidos para proveedores (crear/actualizar sucursales e ítems). 
  - *Dependencias*: Spring Boot, Spring Data JPA, Spring Validation, Spring Security (Resource Server) para verificar JWT y restringir roles. Se comunica con su propia base de datos.

- **Booking Service**
  - *Responsabilidad*: gestionar el ciclo de vida de las reservas. Incluye la creación de reservas, validación de disponibilidad, cancelaciones, check‑in/no‑show y consultas de reservas para clientes y proveedores. 
  - *Dependencias*: Spring Boot, Spring Data JPA, Validación, Spring Security (Resource Server). Implementa clientes HTTP para consultar el catálogo y la información de usuarios.

## Flujos principales

### Autenticación y autorización

1. **Login/registro**: el usuario se registra o inicia sesión mediante los endpoints del `auth-service`. El servicio verifica credenciales, asigna roles y devuelve un token JWT firmado.
2. **Consumo de APIs**: el frontend incluye el JWT en la cabecera `Authorization`. El gateway reenvía la cabecera a los microservicios, que actúan como *Resource Servers* y validan el token. 
3. **Control de acceso**: los controladores de los microservicios utilizan `@PreAuthorize` para restringir operaciones según los roles (`ROLE_USER`, `ROLE_PROVIDER`, `ROLE_ADMIN`). Además, las claims del JWT se extraen mediante `@AuthenticationPrincipal` para asociar las peticiones con el `userId`.

### Gestión de reservas

1. Un cliente autenticado solicita crear una reserva a través del endpoint `POST /api/booking/reservations`. 
2. El `booking-service` valida que la fecha y la sala estén disponibles consultando el `catalog-service`. 
3. Si hay disponibilidad, persiste la reserva y devuelve un identificador. Para cada línea de reserva se generan entidades `ReservationLine` relacionadas.
4. El usuario o el proveedor puede consultar reservas mediante `GET /api/booking/my/reservations` (cliente) o `GET /api/booking/provider/reservations` (proveedor), filtrando por fechas y estado.
5. Existen endpoints para cancelar reservas y marcar no‑shows, que actualizan el estado y liberan la disponibilidad correspondiente.

### Observabilidad y trazabilidad

1. **Métricas**: cada servicio expone métricas de Actuator y Micrometer. Prometheus las recolecta regularmente según lo definido en `prometheus.yml`. 
2. **Dashboards**: Grafana proporciona paneles preconfigurados para JVM, latencia, throughput y errores. Esto permite monitorear el comportamiento de cada microservicio en tiempo real. 
3. **RequestId**: el gateway añade una cabecera `X‑Request‑Id` única. Los servicios incluyen filtros que leen esta cabecera y la inyectan en el *Mapped Diagnostic Context* (MDC) para que los logs puedan correlacionarse a través de los microservicios.

## Deuda técnica y oportunidades

Al analizar el código se identificaron varias áreas susceptibles de mejora:

1. **Versionado de Spring Boot**: el gateway utiliza una versión 3.5.x mientras que los microservicios usan 4.x. Unificar las versiones reducirá problemas de compatibilidad y mantenimiento.
2. **Scripts de migración**: se declara la dependencia de Flyway pero faltan migraciones para `booking-service`, y la propiedad `spring.jpa.hibernate.ddl-auto=update` sigue activa. Es necesario crear scripts de migración y desactivar la creación automática de tablas en producción.
3. **Seguridad y secretos**: actualmente el `clientSecret` del `auth-service` está codificado en texto plano y las claves RSA se encuentran en código. Estos secretos deberían externalizarse mediante variables de entorno o gestores de secretos.
4. **Documentación desactualizada**: algunos documentos mencionan funcionalidades no implementadas (por ejemplo, orquestación BPM). Es importante sincronizar la documentación con el estado real del código y marcar las diferencias.
5. **Pruebas automatizadas**: la cobertura de tests es desigual. Se recomienda añadir pruebas de integración, de seguridad y de flujos de negocio completos utilizando herramientas como Testcontainers.
6. **Observabilidad avanzada**: aunque las métricas básicas están configuradas, faltan reglas de alertas y trazabilidad distribuida. Se podría integrar Alertmanager y OpenTelemetry para tracing.

Este panorama constituye la línea base desde la cual se pueden planificar mejoras y nuevas funcionalidades.