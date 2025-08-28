# AuthService

Servicio de autenticación y gestión de usuarios construido con Spring WebFlux y arquitectura hexagonal.

## Endpoints Disponibles

### GET /api/v1/users/{id}
Obtiene un usuario por su ID.

**Parámetros:**
- `id` (path): ID del usuario

**Respuesta:**
- `200 OK`: Usuario encontrado
- `404 Not Found`: Usuario no encontrado
- `500 Internal Server Error`: Error del servidor

### POST /api/v1/users
Crea un nuevo usuario.

**Cuerpo de la petición:**
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan.perez@example.com",
  "birthDate": "1990-05-15",
  "address": "Calle 123 #45-67",
  "phone": "+57 300 123 4567",
  "baseSalary": 2500000
}
```

**Respuesta:**
- `201 Created`: Usuario creado exitosamente
- `400 Bad Request`: Error en los datos de entrada o email duplicado
- `500 Internal Server Error`: Error del servidor

## Arquitectura

El proyecto sigue una arquitectura hexagonal (puertos y adaptadores) con:

- **Domain**: Modelos de negocio y casos de uso
- **Infrastructure**: Adaptadores para base de datos (R2DBC + MySQL) y web (WebFlux)
- **Applications**: Configuración de la aplicación Spring Boot

## Tecnologías

- Spring Boot 3.x
- Spring WebFlux (Reactivo)
- R2DBC para acceso a base de datos reactivo
- MySQL como base de datos
- Gradle como gestor de dependencias
- Lombok para reducción de código boilerplate

## Esquema de Base de Datos

La tabla `users` tiene la siguiente estructura:
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birth_date DATE,
    address VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE NOT NULL,
    base_salary DECIMAL(10, 2)
);
```

**Nota**: Los campos en Java usan camelCase (firstName, lastName, birthDate, baseSalary) y se mapean automáticamente a snake_case en la base de datos mediante anotaciones `@Column`.

## Ejecución

1. Asegúrate de tener MySQL ejecutándose
2. Configura las credenciales en `application.yaml`
3. Ejecuta `./gradlew bootRun`

## Tests

Ejecuta los tests con:
```bash
./gradlew test
```
