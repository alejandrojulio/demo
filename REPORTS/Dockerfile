FROM openjdk:21-jdk-slim

# Crear directorio de trabajo
WORKDIR /app

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copiar el JAR compilado
COPY applications/app-service/build/libs/REPORTS.jar app.jar

# Exponer el puerto
EXPOSE 8083

# Configurar punto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8083/health || exit 1
