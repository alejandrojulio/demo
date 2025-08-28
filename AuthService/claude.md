# AuthService - Documentación del Proyecto

## 📋 Descripción General

**AuthService** es un microservicio de autenticación y gestión de usuarios desarrollado con **Spring Boot 3.x** y **WebFlux** para operaciones reactivas, siguiendo la **arquitectura hexagonal** del scaffold de Bancolombia.

## 🏗️ Arquitectura del Proyecto

### Estructura de Capas (Clean Architecture)

```
AuthService/
├── applications/                    # Capa de Aplicación
│   └── app-service/               # Configuración principal de Spring Boot
├── domain/                         # Capa de Dominio (Core Business)
│   ├── model/                     # Entidades y modelos de negocio
│   │   └── user/                  # Modelo de usuario
│   └── usecase/                   # Casos de uso de la aplicación
├── infrastructure/                 # Capa de Infraestructura
│   ├── driven-adapters/           # Adaptadores conducidos (R2DBC)
│   ├── entry-points/              # Puntos de entrada (WebFlux)
│   └── helpers/                   # Utilidades transversales
└── deployment/                     # Configuración de despliegue
```

### Principios de Arquitectura Hexagonal

- **Puertos**: Interfaces que definen contratos (`UserRepository`, `ApplicationLogger`)
- **Adaptadores**: Implementaciones concretas de los puertos
- **Dominio**: Lógica de negocio pura, independiente de frameworks
- **Separación de Responsabilidades**: Cada capa tiene una responsabilidad específica

## 🚀 Tecnologías y Dependencias

### Core Framework
- **Java 21** - Versión LTS más reciente
- **Spring Boot 3.3.2** - Framework principal
- **Spring WebFlux** - Programación reactiva no bloqueante
- **Spring Data R2DBC** - Acceso reactivo a base de datos

### Base de Datos
- **R2DBC MySQL** - Driver reactivo para MySQL
- **MySQL 8.0+** - Base de datos relacional

### Herramientas de Desarrollo
- **Gradle 8.14.3** - Gestor de dependencias y build
- **Lombok** - Reducción de código boilerplate
- **Clean Architecture Plugin 3.24.0** - Scaffold de Bancolombia

### Calidad y Testing
- **SonarQube 6.2.0.5505** - Análisis de calidad de código
- **JaCoCo 0.8.13** - Cobertura de código
- **PITest 1.19.0-rc.1** - Tests de mutación
- **JUnit 5** - Framework de testing

### Monitoreo y Observabilidad
- **Spring Boot Actuator** - Endpoints de monitoreo
- **Micrometer** - Métricas de aplicación
- **Prometheus** - Recolección de métricas

## 📡 Programación Reactiva con WebFlux

### Características Reactivas

- **No Bloqueante**: Operaciones I/O asíncronas
- **Backpressure**: Control de flujo de datos
- **Streams**: Procesamiento de flujos de datos
- **Schedulers**: Control de threads y concurrencia

### Componentes Reactivos

```java
// Mono para operaciones que retornan 0 o 1 elemento
Mono<User> user = userRepository.findById(id);

// Flux para operaciones que retornan múltiples elementos
Flux<User> users = userRepository.findAll();

// Operadores reactivos
userRepository.findById(id)
    .map(this::enrichUserData)
    .flatMap(this::validateUser)
    .flatMap(userRepository::save)
    .doOnSuccess(savedUser -> logger.info("Usuario guardado: {}", savedUser.getId()))
    .doOnError(error -> logger.error("Error al guardar usuario", error));
```

## 🔌 R2DBC - Acceso Reactivo a Base de Datos

### Configuración R2DBC

```java
@Configuration
@EnableR2dbcRepositories(basePackages = "co.com.pragma.r2dbc")
public class R2dbcConfig extends AbstractR2dbcConfiguration {
    
    @Override
    @Bean
    public ConnectionPool connectionFactory() {
        return connectionPool;
    }
}
```

### Repositorio Reactivo

```java
@Repository
public interface UserReactiveRepository extends 
    ReactiveCrudRepository<UserEntity, String>, 
    ReactiveQueryByExampleExecutor<UserEntity> {
    
    Mono<Boolean> existsByEmail(String email);
}
```

### Ventajas de R2DBC

- **No Bloqueante**: Conexiones asíncronas
- **Pool de Conexiones**: Gestión eficiente de recursos
- **Transacciones Reactivas**: Soporte para operaciones transaccionales
- **Tipado Seguro**: Sin reflection en runtime

## 🎯 Casos de Uso Implementados

### 1. Crear Usuario (POST /api/v1/users)

```java
public Mono<User> createUser(User userToCreate) {
    logger.info("Starting user creation process for email: {}", userToCreate.getEmail());

    return Mono.just(userToCreate)
            .flatMap(this::ensureEmailIsUnique)
            .map(this::enrichUserData)
            .flatMap(userRepository::save)
            .doOnSuccess(savedUser -> 
                logger.info("User created successfully with ID: {}", savedUser.getId()));
}
```

**Flujo de Validación:**
1. Verificar unicidad del email
2. Enriquecer datos del usuario
3. Persistir en base de datos
4. Logging de éxito/error

### 2. Obtener Usuario (GET /api/v1/users/{id})

```java
public Mono<User> getuser(String id) {
    return userRepository.findById(id);
}
```

## 🔄 Manejo de Flujos Reactivos

### Patrones de Diseño Reactivo

- **Publisher-Subscriber**: Flujo de datos reactivo
- **Backpressure**: Control de velocidad de procesamiento
- **Error Handling**: Manejo de errores en streams
- **Composition**: Composición de operaciones reactivas

### Operadores Reactivos Utilizados

- **`flatMap`**: Transformación asíncrona
- **`map`**: Transformación síncrona
- **`doOnSuccess`**: Side effects en éxito
- **`doOnError`**: Side effects en error
- **`onErrorResume`**: Recuperación de errores

## 📊 Modelo de Dominio

### Entidad User

```java
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String address;
    private String phone;
    private String email;
    private BigDecimal baseSalary;

    public boolean isAdult() {
        return birthDate != null && 
               LocalDate.now().minusYears(18).isAfter(birthDate);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

### Características del Modelo

- **Inmutabilidad**: Uso de Builder pattern
- **Encapsulación**: Lógica de negocio en métodos
- **Validaciones**: Reglas de negocio integradas
- **Independencia**: Sin dependencias de frameworks

## 🌐 API Endpoints

### Configuración de Rutas

```java
@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(GET("/api/v1/users/{id}"), handler::listenGETUseCase)
                .andRoute(POST("/api/v1/users"), handler::listenPOSTUseCase);
    }
}
```

### Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v1/users/{id}` | Obtener usuario por ID |
| `POST` | `/api/v1/users` | Crear nuevo usuario |

### Respuestas HTTP

- **200 OK**: Operación exitosa
- **201 Created**: Recurso creado
- **400 Bad Request**: Error en datos de entrada
- **404 Not Found**: Recurso no encontrado
- **500 Internal Server Error**: Error del servidor

## 🔧 Configuración y Propiedades

### application.yaml

```yaml
server:
  port: 8080
spring:
  application:
    name: "AuthService"
  devtools:
    add-properties: false
management:
  endpoints:
    web:
      exposure:
        include: "health,prometheus"
  endpoint:
    health:
      probes:
        enabled: true
cors:
  allowed-origins: "http://localhost:4200,http://localhost:8080"
adapters:
  r2dbc:
    host: localhost
    port: 5432
    database: demo
    schema: public
    username: postgres
    password: "123"
```

## 🧪 Testing y Calidad

### Framework de Testing

- **JUnit 5**: Framework principal de testing
- **Mockito**: Mocking de dependencias
- **Reactor Test**: Testing de streams reactivos
- **ArchUnit**: Testing de arquitectura

### Estrategias de Testing

- **Unit Tests**: Testing de componentes individuales
- **Integration Tests**: Testing de integración con base de datos
- **Architecture Tests**: Validación de principios arquitectónicos
- **Mutation Tests**: Validación de calidad de tests

### Cobertura de Código

- **JaCoCo**: Generación de reportes de cobertura
- **Métricas**: Líneas, branches, complexity
- **Thresholds**: Mínimos de cobertura requeridos
- **Reports**: HTML, XML, CSV

## 📈 Monitoreo y Observabilidad

### Health Checks

- **Liveness Probe**: Estado de la aplicación
- **Readiness Probe**: Disponibilidad para recibir tráfico
- **Custom Health Indicators**: Métricas específicas del dominio

### Métricas Prometheus

- **JVM Metrics**: Memoria, threads, garbage collection
- **Application Metrics**: Métricas de negocio
- **HTTP Metrics**: Latencia, throughput, errores
- **Database Metrics**: Conexiones, queries, transacciones

### Logging

- **Structured Logging**: Logs en formato estructurado
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **MDC**: Contexto de transacción
- **Performance Logging**: Métricas de rendimiento

## 🚀 Despliegue y DevOps

### Docker

```dockerfile
FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Gradle Tasks

```bash
# Limpiar proyecto
./gradlew clean

# Construir proyecto
./gradlew build

# Ejecutar tests
./gradlew test

# Generar reporte de cobertura
./gradlew jacocoTestReport

# Análisis de SonarQube
./gradlew sonarqube

# Tests de mutación
./gradlew pitest
```

### CI/CD Pipeline

- **Build**: Compilación y testing
- **Quality Gates**: Validación de SonarQube
- **Security Scan**: Análisis de vulnerabilidades
- **Deploy**: Despliegue automático

## 🔒 Seguridad y Buenas Prácticas

### Principios de Seguridad

- **Input Validation**: Validación de entrada
- **SQL Injection Prevention**: Uso de prepared statements
- **Authentication**: Autenticación de usuarios
- **Authorization**: Control de acceso basado en roles

### Buenas Prácticas

- **SOLID Principles**: Principios de diseño
- **DRY**: Don't Repeat Yourself
- **KISS**: Keep It Simple, Stupid
- **Clean Code**: Código legible y mantenible

## 📚 Documentación y Recursos

### Swagger/OpenAPI

- **API Documentation**: Documentación automática
- **Interactive Testing**: Pruebas de endpoints
- **Schema Validation**: Validación de modelos
- **Code Generation**: Generación de clientes

### Documentación Técnica

- **Architecture Decision Records (ADRs)**: Decisiones arquitectónicas
- **API Contracts**: Contratos de API
- **Database Schema**: Esquema de base de datos
- **Deployment Guide**: Guía de despliegue

## 🔮 Roadmap y Mejoras Futuras

### Funcionalidades Planificadas

- [ ] **Autenticación JWT**: Tokens de autenticación
- [ ] **Autorización RBAC**: Control de acceso basado en roles
- [ ] **Rate Limiting**: Limitación de velocidad de requests
- [ ] **Caching**: Cache distribuido con Redis
- [ ] **Event Sourcing**: Auditoría de cambios
- [ ] **Saga Pattern**: Transacciones distribuidas

### Mejoras Técnicas

- [ ] **Circuit Breaker**: Patrón de resiliencia
- [ ] **Bulk Operations**: Operaciones en lote
- [ ] **Pagination**: Paginación de resultados
- [ ] **Search**: Búsqueda avanzada
- [ ] **Audit Logging**: Logging de auditoría

## 🤝 Contribución y Desarrollo

### Estándares de Código

- **Coding Standards**: Estándares de codificación
- **Code Review**: Revisión de código obligatoria
- **Pair Programming**: Programación en parejas
- **Continuous Learning**: Aprendizaje continuo

### Git Workflow

- **Feature Branches**: Desarrollo en ramas de características
- **Pull Requests**: Solicitudes de merge
- **Code Review**: Revisión de código
- **Automated Testing**: Tests automáticos en CI/CD

---

## 📞 Contacto y Soporte

- **Equipo**: CrediYa Development Team
- **Email**: dev@crediya.com
- **Documentación**: [Wiki del Proyecto]
- **Issues**: [GitHub Issues]

---

*Este documento se actualiza regularmente. Última actualización: Diciembre 2024*
