# API Usuario — SmartLogix

Microservicio REST responsable de la gestión de usuarios y roles de SmartLogix. Incluye integración con `api-pedidos` y `api-inventario` para exponer pedidos del usuario y catálogo de productos. Construido con Spring Boot 3.3.4, Spring Data JPA, MySQL y Liquibase. Puerto: **8081**.

Swagger UI: `http://localhost:8081/swagger-ui.html`

---

## Responsabilidad

Este microservicio gestiona el dominio de identidad y acceso:

- **Usuarios** (`/api/v1/users`): registro, búsqueda por RUT/email/nombre, actualización y eliminación.
- **Roles** (`/api/v1/roles`): creación y asignación de roles a usuarios.
- Expone los pedidos de un usuario consultando a `api-pedidos` (comunicación inter-servicio).
- Expone el catálogo de productos consultando a `api-inventario` (comunicación inter-servicio).

---

## Arquitectura

```
api-usuario
├── Controller (REST)
│   ├── UserController    /api/v1/users
│   └── RoleController    /api/v1/roles
├── Service (lógica)
│   ├── UserService       Lógica de negocio + llamadas a otros servicios
│   └── RoleService
├── Repository (JPA)
│   ├── UserRepository
│   └── RoleRepository
├── Model (entidades JPA)
│   ├── User              Usuario con relación ManyToOne a Role
│   └── Role
├── DTO (transferencia)
│   ├── OrderDTO          Respuesta de api-pedidos
│   └── ProductDTO        Respuesta de api-inventario
└── config/
    └── RestTemplateConfig
```

### Comunicación inter-servicio

`api-usuario` consulta a:
- `api-pedidos` (vía gateway): para `GET /api/v1/users/{id}/pedidos`.
- `api-inventario` (vía gateway): para `GET /api/v1/users/catalogo`.

Ambas URLs se configuran con la variable `gateway.api.url`.

### Patrones de diseño aplicados

**1. MVC (Model-View-Controller)**
Separación clara entre controladores (`UserController`, `RoleController`), servicios (`UserService`, `RoleService`) y modelos (`User`, `Role`). Los controladores no contienen lógica de negocio; sólo transforman peticiones HTTP en llamadas al servicio y retornan respuestas apropiadas. Beneficio: facilita el testing unitario de cada capa por separado.

**2. Repository Pattern**
`UserRepository` y `RoleRepository` extienden `JpaRepository` y añaden métodos de consulta derivados (`findByRut`, `findByEmail`, `findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCaseOrRutContainingIgnoreCase`). Esto encapsula el acceso a datos detrás de una interfaz, desacoplando la lógica de negocio de la tecnología de persistencia.

---

## Estructura de directorios

```
api-usuario/
├── src/
│   ├── main/
│   │   ├── java/com/smartlogix/usuario/
│   │   │   ├── UsuarioApplication.java
│   │   │   ├── config/
│   │   │   │   └── RestTemplateConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── UserController.java
│   │   │   │   └── RoleController.java
│   │   │   ├── dto/
│   │   │   │   ├── OrderDTO.java
│   │   │   │   └── ProductDTO.java
│   │   │   ├── model/
│   │   │   │   ├── User.java
│   │   │   │   └── Role.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RoleRepository.java
│   │   │   └── service/
│   │   │       ├── UserService.java
│   │   │       └── RoleService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/changelog/
│   └── test/
│       ├── java/
│       └── resources/application-test.properties
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Modelos de datos

### User (Usuario)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long | Identificador autoincremental |
| nombre | String | Nombre del usuario |
| apellido | String | Apellido del usuario |
| rut | String | RUT chileno único (formato XXXXXXXX-X) |
| email | String | Correo electrónico único |
| password | String | Contraseña (hash recomendado en producción) |
| fechaNacimiento | LocalDate | Fecha de nacimiento |
| direccion | String | Dirección del usuario |
| fechaRegistro | LocalDateTime | Fecha de registro en el sistema |
| role | Role | Rol asignado (FK, ManyToOne) |

### Role (Rol)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long | Identificador autoincremental |
| nombre | String | Nombre del rol (ej. ADMIN, CLIENTE, BODEGUERO) |

---

## Endpoints REST

### Usuarios — `/api/v1/users`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| GET | `/api/v1/users` | Listar todos los usuarios | 200 / 204 |
| GET | `/api/v1/users/{id}` | Obtener usuario por ID | 200 / 404 |
| GET | `/api/v1/users/by-rut/{rut}` | Buscar usuario por RUT | 200 / 404 |
| GET | `/api/v1/users/by-email?email=x` | Buscar usuario por email | 200 / 404 |
| GET | `/api/v1/users/buscar?q=juan` | Buscar por nombre/apellido/RUT | 200 / 204 |
| GET | `/api/v1/users/{id}/pedidos` | Pedidos del usuario (→ api-pedidos) | 200 / 404 |
| GET | `/api/v1/users/catalogo` | Catálogo de productos (→ api-inventario) | 200 / 204 |
| POST | `/api/v1/users` | Registrar nuevo usuario | 201 |
| PUT | `/api/v1/users/{id}` | Actualizar usuario | 200 / 404 |
| DELETE | `/api/v1/users/{id}` | Eliminar usuario | 204 / 404 |

**Ejemplo POST `/api/v1/users`:**

```json
{
  "nombre": "María",
  "apellido": "González",
  "rut": "12345678-9",
  "email": "maria@ejemplo.cl",
  "password": "hashed_password",
  "fechaNacimiento": "1990-05-15",
  "direccion": "Av. Principal 123, Santiago",
  "fechaRegistro": "2026-06-23T10:00:00",
  "role": { "id": 2 }
}
```

**Respuesta 201:**

```json
{
  "id": 7,
  "nombre": "María",
  "apellido": "González",
  "rut": "12345678-9",
  "email": "maria@ejemplo.cl",
  "fechaNacimiento": "1990-05-15",
  "direccion": "Av. Principal 123, Santiago",
  "fechaRegistro": "2026-06-23T10:00:00",
  "role": { "id": 2, "nombre": "CLIENTE" }
}
```

---

### Roles — `/api/v1/roles`

| Método | Ruta | Descripción | Respuesta |
|--------|------|-------------|-----------|
| GET | `/api/v1/roles` | Listar todos los roles | 200 / 204 |
| GET | `/api/v1/roles/{id}` | Obtener rol por ID | 200 / 404 |
| GET | `/api/v1/roles/by-nombre/{nombre}` | Buscar rol por nombre | 200 / 404 |
| POST | `/api/v1/roles` | Crear rol | 201 |
| PUT | `/api/v1/roles/{id}` | Actualizar rol | 200 / 404 |
| DELETE | `/api/v1/roles/{id}` | Eliminar rol | 204 / 404 |

---

## Dependencias principales (`pom.xml`)

| Artefacto | Versión | Propósito |
|-----------|---------|-----------|
| spring-boot-starter-parent | 3.3.4 | BOM de Spring Boot |
| spring-boot-starter-web | — | API REST con Spring MVC |
| spring-boot-starter-data-jpa | — | Persistencia con Hibernate |
| mysql-connector-j | — | Driver MySQL |
| liquibase-core | — | Migraciones de base de datos |
| lombok | — | Reducción de código boilerplate |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 | Swagger UI |
| h2 (test) | — | Base de datos en memoria para pruebas |

---

## Instalación y ejecución

**Requisitos previos:** Java 17, Maven 3.8+, MySQL 8.

```bash
# 1. Compilar
./mvnw clean package -DskipTests

# 2. Ejecutar
./mvnw spring-boot:run
```

Variables de entorno disponibles:

| Variable | Por defecto | Descripción |
|----------|-------------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/db_usuarios?...` | URL MySQL |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario MySQL |
| `SPRING_DATASOURCE_PASSWORD` | *(vacío)* | Contraseña MySQL |
| `GATEWAY_API_URL` | `localhost` | Host del API Gateway para comunicación inter-servicio |

### Con Docker

```bash
docker build -t smartlogix-usuario .
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/db_usuarios?createDatabaseIfNotExist=true \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  smartlogix-usuario
```

---

## Pruebas

```bash
# Ejecutar pruebas unitarias (usan H2 en memoria)
./mvnw test

# Reportes en:
# target/surefire-reports/
```

La cobertura objetivo es ≥ 60% sobre las funcionalidades del servicio.

---

## Persistencia

Las migraciones son gestionadas por **Liquibase** (`src/main/resources/db/changelog/db.changelog-master.yaml`). Las pruebas utilizan el perfil `test` con H2 en memoria.

---

## Estrategia de branching

| Rama | Propósito |
|------|-----------|
| `main` | Código en producción |
| `develop` | Integración de cambios |
| `feature/*` | Nuevas funcionalidades |
| `fix/*` | Corrección de bugs |
