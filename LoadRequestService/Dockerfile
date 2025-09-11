# Dockerfile para LoadRequestService (Microservicio de Solicitudes)
# Utilizando multi-stage build para optimizar el tamaño de la imagen

# ========================================
# Stage 1: Build - Compilación con Gradle
# ========================================
FROM gradle:8.5-jdk21-alpine AS builder

# Metadatos de la imagen
LABEL maintainer="CrediYa Development Team"
LABEL description="LoadRequestService - Microservicio de Gestión de Solicitudes de Préstamos"
LABEL version="1.0.0"

# Configurar directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Gradle (para aprovechar cache de dependencias)
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY gradle.properties .
COPY settings.gradle .
COPY main.gradle .
COPY lombok.config .

# Copiar archivos build.gradle de todos los módulos
COPY build.gradle .
COPY applications/app-service/build.gradle applications/app-service/
COPY domain/model/build.gradle domain/model/
COPY domain/usecase/build.gradle domain/usecase/
COPY infrastructure/driven-adapters/r2dbc-mysql/build.gradle infrastructure/driven-adapters/r2dbc-mysql/
COPY infrastructure/entry-points/reactive-web/build.gradle infrastructure/entry-points/reactive-web/
COPY infrastructure/helpers/build.gradle infrastructure/helpers/

# Descargar dependencias (aprovecha el cache de Docker si no cambian)
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente
COPY . .

# Compilar la aplicación (skip architecture validation en Docker por incompatibilidad de paths)
RUN ./gradlew build -x test -x validateStructure --no-daemon

# ========================================
# Stage 2: Runtime - Imagen final optimizada
# ========================================
FROM eclipse-temurin:21-jre-alpine

# Instalar herramientas necesarias y configurar timezone
RUN apk add --no-cache \
    curl \
    tzdata && \
    cp /usr/share/zoneinfo/America/Bogota /etc/localtime && \
    echo "America/Bogota" > /etc/timezone && \
    apk del tzdata

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Configurar directorio de trabajo
WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=builder /app/applications/app-service/build/libs/*.jar app.jar

# Crear directorio para logs
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Cambiar a usuario no-root
USER appuser

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8081 \
    MYSQL_HOST=mysql-crediya \
    MYSQL_PORT=3306 \
    MYSQL_DATABASE=crediya_db \
    MYSQL_USERNAME=crediya_user \
    MYSQL_PASSWORD=crediya_pass \
    AUTH_SERVICE_URL=http://auth-service:8080

# Exponer puerto
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
