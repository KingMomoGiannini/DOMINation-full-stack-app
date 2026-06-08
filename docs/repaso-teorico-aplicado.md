# Repaso teórico aplicado

Este documento conecta conceptos teóricos de arquitectura de software, seguridad, persistencia y observabilidad con su implementación práctica en DOMINation. El objetivo es servir como material de estudio para estudiantes y desarrolladores que deseen comprender cómo se plasman estos principios en un proyecto real.

## Microservicios y arquitectura distribuida

**Microservicios** es un estilo arquitectónico que descompone una aplicación en servicios pequeños, independientes y desplegables de forma autónoma. Cada servicio se centra en una responsabilidad concreta y comunica con otros servicios a través de API bien definidas.

En DOMINation esto se refleja en la separación en tres servicios principales (`auth-service`, `catalog-service` y `booking-service`) y un API Gateway. Cada servicio mantiene su base de datos y su lógica de negocio. El gateway actúa como punto de entrada único, aplicando políticas comunes como CORS y trazabilidad.

Beneficios de este enfoque:

- **Escalabilidad independiente**: se puede escalar solo el servicio que necesite más recursos (por ejemplo, `booking-service` en épocas de alta demanda).
- **Aislamiento de fallos**: un problema en un servicio no derriba toda la aplicación. La tolerancia a fallos puede mejorarse con circuit breakers.
- **Despliegue independiente**: cada servicio puede evolucionar y desplegarse de forma autónoma.

Desafíos comunes:

- **Gestión de la transaccionalidad**: en operaciones que abarcan múltiples servicios (reservas y pagos) es necesario utilizar patrones como *Saga* o *choreography* para mantener la consistencia.
- **Descubrimiento y balanceo**: en proyectos complejos se requieren mecanismos de descubrimiento y registro de servicios (Eureka, Consul).
- **Tracing distribuido**: es necesario propagar identificadores de petición para rastrear el flujo a través de múltiples servicios.

## Inversión de control e inyección de dependencias

Spring Framework implementa el principio de **Inversión de Control (IoC)**, mediante el cual el contenedor crea y gestiona las dependencias en lugar de que los objetos las instancien manualmente. En DOMINation esto se observa en la configuración con `@Configuration` y la definición de beans con `@Bean`, así como en la anotación de clases con `@Service` y `@Repository` para que Spring las detecte y registre en el contenedor.

La **inyección de dependencias (DI)** simplifica la gestión de objetos y promueve un código desacoplado. Por ejemplo, `ReservationService` recibe repositorios y clientes HTTP como argumentos de su constructor; Spring se encarga de proporcionar instancias concretas al momento de ejecutar la aplicación.

## Seguridad con OAuth 2.1 y JWT

DOMINation implementa un flujo de autenticación basado en **OAuth 2.1**. El `auth-service` actúa como *Authorization Server* y emite **tokens JWT** firmados. Estos tokens contienen claims como `userId` y `authorities` que representan las credenciales del usuario.

Los microservicios de negocio (`catalog-service` y `booking-service`) se configuran como **Resource Servers**. Validan los tokens recibidos en las cabeceras y aplican políticas de autorización usando anotaciones como `@PreAuthorize`. El uso de JWT permite verificar la identidad sin necesidad de consultas de base de datos en cada llamada, ya que el token encapsula toda la información relevante. Los tokens se firman con claves RSA para garantizar su integridad.

Aspectos clave de este esquema:

- **Grant de Authorization Code**: el frontend redirige al usuario al auth‑service para autenticarse. Tras el login, el usuario obtiene un token que utiliza para consumir los servicios.
- **Roles y claims**: los roles (`ROLE_USER`, `ROLE_PROVIDER`, `ROLE_ADMIN`) se codifican en claims. Los controladores extraen estas claims con `@AuthenticationPrincipal` para asociar los recursos al usuario correspondiente.
- **Protección de endpoints**: las anotaciones `@EnableMethodSecurity` y `@PreAuthorize` garantizan que solo los usuarios con los roles adecuados puedan ejecutar determinadas operaciones.

## Persistencia y acceso a datos

La persistencia en DOMINation se implementa con **Spring Data JPA**, que proporciona un repositorio basado en interfaces para cada entidad. Esto se apoya en el estándar **JPA (Jakarta Persistence API)** y el motor **Hibernate** para traducir objetos Java en registros de base de datos.

Elementos importantes:

- **Mapeo objeto–relacional (ORM)**: las entidades se anotan con `@Entity` y se mapean a tablas mediante `@Table`. Las relaciones entre entidades se definen con `@OneToMany` y `@ManyToOne`, y los identificadores se generan con `@GeneratedValue`.
- **DTOs y mapeo**: los controladores no devuelven las entidades directamente. En su lugar se utilizan Data Transfer Objects (DTOs) para exponer solo la información necesaria. Esto promueve el encapsulamiento y permite validar y transformar datos antes de la persistencia.
- **Migraciones de base de datos**: en entornos productivos se recomienda utilizar herramientas como **Flyway** para versionar los esquemas. En el repositorio actual se detectó la ausencia de scripts de migración en `booking-service`, lo que es una oportunidad de mejora.

## Comunicación entre servicios y API Gateway

El **API Gateway** es un patrón que proporciona un único punto de entrada para todos los clientes. En DOMINation, el gateway enruta solicitudes al microservicio correspondiente, aplica filtros (como la generación de `X‑Request‑Id`) y gestiona las políticas de CORS. Esto simplifica la interacción del frontend, ya que oculta detalles como los puertos de cada servicio.

Para la comunicación entre microservicios, el proyecto utiliza **HTTP sincrono**. Por ejemplo, el `booking-service` llama al `catalog-service` mediante un cliente WebClient para verificar la disponibilidad antes de crear una reserva. Aunque este enfoque es sencillo, se podría evolucionar hacia **comunicación asíncrona** con mensajes (Kafka o RabbitMQ) para mejorar la resiliencia y el desac acoplamiento, aplicando patrones como *event-driven architecture* o *Saga* para transacciones distribuidas.

## Observabilidad: métricas, logs y trazabilidad

La **observabilidad** permite entender el estado interno del sistema a partir de sus salidas. DOMINation integra varias prácticas:

- **Métricas**: Spring Boot Actuator y Micrometer exponen métricas sobre uso de CPU, memoria, tiempo de respuesta y contadores de solicitudes. Prometheus recoge estas métricas y las almacena en una base de datos temporal.
- **Dashboards**: Grafana lee los datos de Prometheus y los presenta en paneles que muestran latencia, throughput y porcentaje de errores por servicio. Esto ayuda a detectar cuellos de botella y comportamientos anómalos.
- **Request tracing**: para correlacionar las llamadas entre servicios se propaga un identificador de solicitud (`X‑Request‑Id`). Los filtros en el gateway y en los servicios añaden este identificador a los logs (MDC), permitiendo reconstruir el flujo de una petición completa.
- **Alertas**: aunque se documenta la posibilidad de usar Alertmanager, aún no hay reglas de alerta configuradas. Implementar alertas (por ejemplo, cuando la tasa de errores 5xx supera cierto umbral) mejoraría la detección temprana de problemas.

## Testing y calidad de código

El proyecto combina pruebas unitarias y de integración utilizando **JUnit 5** y **Spring Test**. Se utilizan anotaciones como `@SpringBootTest` para levantar el contexto completo y `@WebMvcTest` para probar los controladores aislados.

Prácticas recomendadas:

- **Pruebas de integración**: verificar que los microservicios interactúan correctamente, incluyendo la validación de JWT y la comunicación con la base de datos.
- **Testcontainers**: usar contenedores efímeros de bases de datos y servicios en las pruebas para replicar el entorno de producción.
- **Cobertura de seguridad**: incluir casos que verifiquen que los endpoints están correctamente protegidos y que los roles no autorizados reciben errores 403.

## Conclusión

DOMINation es un proyecto que ilustra cómo aplicar principios modernos de arquitectura de software en un contexto real. La combinación de microservicios, OAuth 2.1, JPA, un API Gateway y herramientas de observabilidad proporciona una base sólida tanto para un producto comercial como para un proyecto de estudio. Comprender la teoría que sustenta estas decisiones ayuda a tomar mejores decisiones de diseño y a planificar evoluciones futuras.