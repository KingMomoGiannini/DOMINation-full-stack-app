# Anotaciones explicadas

Este documento recopila las anotaciones más relevantes utilizadas en el proyecto DOMINation y explica de forma didáctica su finalidad, su ubicación en el código y el concepto teórico que representan. Las descripciones se organizan por categorías para facilitar su consulta. Para cada anotación se incluyen cuatro aspectos: **¿qué hace?**, **¿dónde aparece?**, **¿por qué se usa en este proyecto?** y **¿qué concepto teórico representa?**.

## 1 Anotaciones de configuración y componentes

### `@Configuration`
- **¿Qué hace?**: Declara que una clase contiene definiciones de beans de Spring. Es equivalente a un archivo de configuración.
- **¿Dónde aparece?**: En las clases de configuración de seguridad (`SecurityConfig`) y del servidor de autorización (`OAuth2AuthorizationServerConfig`).
- **¿Por qué se usa aquí?**: Para agrupar las definiciones de filtros de seguridad, decodificadores JWT, clientes registrados y otras configuraciones que Spring debe inicializar.
- **Concepto teórico**: Aplica el principio de *Inversión de Control*, indicando que la creación de objetos es responsabilidad del contenedor.

### `@Bean`
- **¿Qué hace?**: Marca un método que devuelve un objeto que Spring debe registrar como bean para inyección de dependencias.
- **¿Dónde aparece?**: En las clases de configuración, para registrar `SecurityFilterChain`, decodificadores JWT (`JwtDecoder`), `PasswordEncoder` y otros componentes.
- **¿Por qué se usa aquí?**: Permite instanciar y configurar objetos una sola vez y compartirlos en toda la aplicación.
- **Concepto teórico**: *Inyección de dependencias* y reutilización de objetos gestionados por el contenedor IoC.

### `@Component`, `@Service` y `@Repository`
- **¿Qué hacen?**: Marcan clases que Spring debe detectar automáticamente mediante *component scanning* y registrar como beans. `@Service` y `@Repository` son especializaciones de `@Component` para capas de servicio y persistencia respectivamente.
- **¿Dónde aparecen?**: En clases de servicios de negocio (por ejemplo, `ReservationService`) y repositorios que implementan lógicas personalizadas además de JPA.
- **¿Por qué se usan aquí?**: Para distinguir las capas de la aplicación y permitir que Spring inyecte automáticamente estas clases allí donde se necesiten.
- **Concepto teórico**: Organización por capas y *Dependency Injection*.

### `@SpringBootApplication`
- **¿Qué hace?**: Combinación de `@Configuration`, `@EnableAutoConfiguration` y `@ComponentScan`. Marca la clase principal de arranque de un microservicio.
- **¿Dónde aparece?**: En las clases `AuthServiceApplication`, `CatalogServiceApplication`, `BookingServiceApplication` y `GatewayApplication`.
- **¿Por qué se usa aquí?**: Simplifica la configuración de Spring Boot permitiendo la detección automática de componentes y configuración automática.
- **Concepto teórico**: Arranque de aplicaciones con *convención sobre configuración*.

### `@EnableWebSecurity` y `@EnableMethodSecurity`
- **¿Qué hacen?**: Activan las características de Spring Security en la aplicación web y habilitan la seguridad basada en anotaciones a nivel de método (`@PreAuthorize`).
- **¿Dónde aparecen?**: En las clases de configuración de seguridad de cada microservicio (`SecurityConfig`).
- **¿Por qué se usan aquí?**: Permiten definir filtros de autenticación, reglas de autorización y restricciones en métodos sin escribir código imperativo repetitivo.
- **Concepto teórico**: Aplicación del patrón *Filter Chain* y seguridad declarativa.

### `@Order`
- **¿Qué hace?**: Establece el orden de precedencia entre múltiples filtros o beans cuando hay varios del mismo tipo.
- **¿Dónde aparece?**: En las configuraciones del servidor de autorización y del resource server para ordenar las cadenas de filtros.
- **¿Por qué se usa aquí?**: Para garantizar que los filtros del Authorization Server se apliquen antes que los del Resource Server y así procesar correctamente las peticiones.
- **Concepto teórico**: Control de prioridad y secuencia en pipelines de procesamiento.

## 2 Anotaciones de controladores REST

### `@RestController`
- **¿Qué hace?**: Marca una clase como controlador REST y combina `@Controller` con `@ResponseBody`, lo que hace que los métodos devuelvan objetos serializados a JSON automáticamente.
- **¿Dónde aparece?**: En controladores como `ProviderController` y `ReservationController`.
- **¿Por qué se usa aquí?**: Para exponer APIs RESTful que devuelven respuestas JSON y manejar solicitudes HTTP.
- **Concepto teórico**: Arquitectura REST y serialización de recursos.

### `@RequestMapping`
- **¿Qué hace?**: Define un prefijo de ruta común y atributos compartidos (métodos, cabeceras) para un controlador o método.
- **¿Dónde aparece?**: En la cabecera de los controladores para establecer rutas base como `/api/catalog/provider`.
- **¿Por qué se usa aquí?**: Para agrupar endpoints relacionados bajo un mismo path, mejorar legibilidad y reducir repetición.
- **Concepto teórico**: Organización de APIs por recurso.

### `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- **¿Qué hacen?**: Declaran métodos manejadores para solicitudes HTTP GET, POST, PUT, DELETE o PATCH. Heredan atributos de `@RequestMapping` si están presentes.
- **¿Dónde aparecen?**: En los métodos de los controladores para crear, obtener, actualizar o eliminar recursos (por ejemplo, `@GetMapping("/branches")`).
- **¿Por qué se usan aquí?**: Permiten mapear cada operación HTTP a un método concreto de forma declarativa.
- **Concepto teórico**: Principios de REST y mapeo entre operaciones CRUD y verbos HTTP.

### `@PathVariable` y `@RequestParam`
- **¿Qué hacen?**: Extraen valores de la ruta (`/reservations/{id}`) o de parámetros de consulta (`?page=0`) y los inyectan en los argumentos de un método.
- **¿Dónde aparecen?**: En parámetros de métodos de los controladores para capturar `id`, `page`, `size` u otros datos.
- **¿Por qué se usan aquí?**: Para obtener de forma sencilla valores de la URL sin parsear manualmente la cadena.
- **Concepto teórico**: Vinculación de rutas a parámetros de métodos y separación de responsabilidades.

### `@RequestBody`
- **¿Qué hace?**: Indica que el contenido del cuerpo de una solicitud HTTP debe deserializarse a un objeto Java.
- **¿Dónde aparece?**: En métodos que reciben DTOs para crear o actualizar recursos (por ejemplo, reservas o items).
- **¿Por qué se usa aquí?**: Permite recibir datos en formato JSON y mapearlos a objetos de dominio o DTOs de forma automática.
- **Concepto teórico**: Deserialización y *Data Binding*.

### `@Valid`
- **¿Qué hace?**: Indica que el objeto recibido debe validarse según las anotaciones de Bean Validation.
- **¿Dónde aparece?**: En parámetros de métodos de los controladores y en capas de servicio cuando se reciben DTOs.
- **¿Por qué se usa aquí?**: Para asegurar que las solicitudes cumplen restricciones (por ejemplo, no nulo, tamaño mínimo) antes de procesarlas.
- **Concepto teórico**: Validación declarativa de datos de entrada.

## 3 Anotaciones de validación (Bean Validation)

Las siguientes anotaciones provienen de Jakarta Validation (antes JSR 380) y definen restricciones en campos de entidades o DTOs.

| Anotación | Función | Uso en el proyecto | Concepto teórico |
|---|---|---|---|
| `@NotNull` | Exige que un campo no sea `null`. | En entidades y DTOs para asegurar la presencia de datos obligatorios. | Garantiza *invariantes* en el dominio. |
| `@NotBlank` | Requiere que una cadena no esté vacía ni compuesta solo por espacios. | En campos como nombre de usuario o correo en formularios de registro. | Integridad de datos de entrada. |
| `@Size(min, max)` | Restringe la longitud de colecciones o cadenas. | Para limitar caracteres en contraseñas o descripciones de items. | Validación de formatos y longitudes. |
| `@Min` / `@Max` | Establecen límites numéricos. | En cantidades de reservas o capacidades máximas de salas. | Control de rangos aceptables. |
| `@Email` | Valida el formato de una dirección de correo electrónico. | En campos de email de usuarios. | Validación de formatos estándar. |
| `@Pattern` | Aplica una expresión regular a una cadena. | En contraseñas o códigos para cumplir ciertos patrones. | Garantiza conformidad con políticas de formato. |

Estas anotaciones se combinan con `@Valid` para que Spring lance excepciones de validación cuando una solicitud no cumpla las reglas.

## 4 Anotaciones de JPA (persistencia)

### `@Entity` y `@Table`
- **¿Qué hacen?**: `@Entity` marca una clase como entidad JPA y `@Table` puede definir el nombre de la tabla o restricciones adicionales.
- **¿Dónde aparecen?**: En clases de dominio como `User`, `Role`, `Branch`, `RentableItem`, `Reservation` y otras.
- **¿Por qué se usan aquí?**: Para mapear las clases de dominio a tablas de la base de datos y permitir que Hibernate gestione su persistencia.
- **Concepto teórico**: ORM (*Object‑Relational Mapping*), donde las clases representan filas de tablas.

### `@Id`, `@GeneratedValue` y `@Column`
- **¿Qué hacen?**: `@Id` indica la clave primaria; `@GeneratedValue` define la estrategia de generación (por ejemplo, auto‑incremental o secuencia); `@Column` especifica el nombre de la columna y sus propiedades.
- **¿Dónde aparecen?**: En campos como `id` o `uuid` de todas las entidades.
- **¿Por qué se usan aquí?**: Para definir cómo se mapean los identificadores y atributos a las columnas de la base de datos.
- **Concepto teórico**: Identidad de entidad y mapeo de atributos.

### Relaciones: `@OneToMany`, `@ManyToOne`, `@JoinColumn`, `@Enumerated`
- **¿Qué hacen?**: Modelan relaciones entre entidades. `@OneToMany` y `@ManyToOne` describen cardinalidades; `@JoinColumn` indica la columna de unión; `@Enumerated` persiste enumeraciones como cadenas o ordinales.
- **¿Dónde aparecen?**: Entre `Reservation` y `ReservationLine` (uno a muchos), entre `Branch` y `RentableItem`, etc.
- **¿Por qué se usan aquí?**: Para que JPA gestione automáticamente las asociaciones y las claves foráneas.
- **Concepto teórico**: Relaciones de base de datos y asociaciones de objetos.

## 5 Anotaciones de seguridad

### `@PreAuthorize`
- **¿Qué hace?**: Evalúa una expresión de seguridad antes de ejecutar el método. Permite restringir el acceso según roles o atributos del token.
- **¿Dónde aparece?**: En métodos de controladores y servicios que solo deben ejecutar determinados roles (por ejemplo, `@PreAuthorize("hasRole('ADMIN')")`).
- **¿Por qué se usa aquí?**: Para aplicar reglas de autorización a nivel de método de forma declarativa y centralizada.
- **Concepto teórico**: *AOP* (programación orientada a aspectos) y seguridad declarativa.

### `@AuthenticationPrincipal`
- **¿Qué hace?**: Inyecta el principal autenticado en un argumento de método; en este proyecto se usa con `Jwt` para extraer claims (ej. `userId`).
- **¿Dónde aparece?**: En controladores para obtener el usuario autenticado y asociar una reserva a su propietario.
- **¿Por qué se usa aquí?**: Facilita acceder al token JWT completo sin tener que buscarlo en el contexto de seguridad.
- **Concepto teórico**: Acceso a la identidad y autenticación en el contexto de seguridad.

## 6 Anotaciones de Lombok

El proyecto utiliza **Lombok** para reducir código boilerplate. Algunas de sus anotaciones son:

| Anotación | Función | Uso en el proyecto | Concepto teórico |
|---|---|---|---|
| `@Getter` / `@Setter` | Generan métodos getter y setter para los campos. | En casi todas las entidades y DTOs. | Reduce la escritura de código repetitivo. |
| `@Builder` | Proporciona un patrón *Builder* para crear objetos de forma fluida. | En DTOs para construir objetos con muchas propiedades. | Patrón de diseño *Builder*. |
| `@NoArgsConstructor` / `@AllArgsConstructor` | Generan constructores sin argumentos y con todos los argumentos respectivamente. | En entidades para cumplir requisitos de JPA y simplificar la creación. | Boileplate reduction. |
| `@Data` | Combinación de `@Getter`, `@Setter`, `@EqualsAndHashCode`, `@ToString`. | En algunos DTOs simples. | Simplifica la definición de clases de datos. |

## 7 Anotaciones de OpenAPI / Swagger

DOMINation expone documentación de APIs mediante OpenAPI (Swagger). Las anotaciones se encuentran principalmente en los controladores y DTOs:

- **`@Tag`**: Agrupa endpoints bajo un tema para la documentación.
- **`@Operation`**: Describe un endpoint específico con su resumen y descripción detallada.
- **`@Parameter` y `@RequestBody`** (versión de Swagger): Describen parámetros y cuerpos de solicitud con ejemplos.

Estas anotaciones ayudan a generar una interfaz interactiva (Swagger UI) que facilita a los desarrolladores explorar y probar la API.

## 8 Anotaciones de testing

Aunque la cobertura de pruebas es limitada, se utilizan anotaciones de Spring Test y JUnit:

- **`@SpringBootTest`**: Arranca el contexto completo de Spring para pruebas de integración.
- **`@WebMvcTest`**: Levanta únicamente la capa web (controladores) para pruebas unitarias de endpoints.
- **`@MockBean`**: Crea un bean simulado que reemplaza a un bean real en el contexto de Spring durante la prueba.
- **`@Test`**: Marca un método como caso de prueba de JUnit.

Estas anotaciones permiten probar de forma aislada los controladores y servicios, emulando dependencias con mocks.

---

Esta colección de anotaciones muestra cómo Spring, JPA, Bean Validation y otras bibliotecas proporcionan una sintaxis declarativa para configurar comportamiento complejo de forma concisa. Comprender su propósito facilita el mantenimiento y la evolución del código.