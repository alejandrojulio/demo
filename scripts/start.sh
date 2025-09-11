#!/bin/bash

# Script para iniciar el sistema completo CrediYa
# Uso: ./scripts/start.sh [ambiente]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Banner
echo -e "${BLUE}"
echo "=========================================="
echo "    CrediYa - Sistema de Préstamos"
echo "       Iniciando Contenedores Docker"
echo "=========================================="
echo -e "${NC}"

# Verificar que Docker esté corriendo
if ! docker info > /dev/null 2>&1; then
    log_error "Docker no está corriendo. Por favor, inicia Docker Desktop."
    exit 1
fi

# Verificar que docker-compose esté disponible
if ! command -v docker-compose &> /dev/null; then
    log_error "docker-compose no está instalado."
    exit 1
fi

# Limpiar contenedores previos si existen
log_info "Limpiando contenedores previos..."
docker-compose down --remove-orphans > /dev/null 2>&1 || true

# Construir imágenes
log_info "Construyendo imágenes de los microservicios..."
docker-compose build --no-cache

# Iniciar servicios
log_info "Iniciando servicios..."
log_info "1. Iniciando base de datos..."
docker-compose up -d mysql-crediya

# Esperar a que la base de datos esté lista
log_info "Esperando a que la base de datos esté lista..."
timeout=120
counter=0

while [ $counter -lt $timeout ]; do
    if docker-compose exec -T mysql-crediya mysqladmin ping -h localhost -u crediya_user -pcrediya_pass --silent > /dev/null 2>&1; then
        log_success "Base de datos CrediYa está lista"
        break
    fi
    sleep 2
    counter=$((counter + 2))
    echo -n "."
done

if [ $counter -ge $timeout ]; then
    log_error "Timeout esperando la base de datos CrediYa"
    exit 1
fi

# Iniciar microservicios
log_info "2. Iniciando microservicio de Autenticación..."
docker-compose up -d auth-service

# Esperar a que el servicio de auth esté listo
log_info "Esperando a que el servicio de Autenticación esté listo..."
counter=0
while [ $counter -lt $timeout ]; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_success "Servicio de Autenticación está listo"
        break
    fi
    sleep 3
    counter=$((counter + 3))
    echo -n "."
done

if [ $counter -ge $timeout ]; then
    log_error "Timeout esperando el servicio de Autenticación"
    exit 1
fi

log_info "3. Iniciando microservicio de Solicitudes..."
docker-compose up -d load-request-service

# Esperar a que el servicio de solicitudes esté listo
log_info "Esperando a que el servicio de Solicitudes esté listo..."
counter=0
while [ $counter -lt $timeout ]; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        log_success "Servicio de Solicitudes está listo"
        break
    fi
    sleep 3
    counter=$((counter + 3))
    echo -n "."
done

if [ $counter -ge $timeout ]; then
    log_error "Timeout esperando el servicio de Solicitudes"
    exit 1
fi

# Mostrar estado final
echo -e "\n${GREEN}=========================================="
echo "   ✅ Sistema CrediYa iniciado exitosamente"
echo "==========================================${NC}"

echo -e "\n${BLUE}📋 Servicios disponibles:${NC}"
echo "🔐 Autenticación:    http://localhost:8080"
echo "📄 Solicitudes:      http://localhost:8081"
echo "🗄️  MySQL CrediYa:   localhost:3307"

echo -e "\n${BLUE}🔍 Health Checks:${NC}"
echo "🔐 Auth Health:      http://localhost:8080/actuator/health"
echo "📄 Requests Health:  http://localhost:8081/actuator/health"

echo -e "\n${BLUE}👥 Usuarios de prueba:${NC}"
echo "🔑 Admin:     admin@crediya.com     / password123"
echo "🔑 Asesor:    asesor@crediya.com    / password123"
echo "🔑 Cliente:   ana.perez@email.com   / password123"

echo -e "\n${YELLOW}📊 Para ver logs en tiempo real:${NC}"
echo "docker-compose logs -f"

echo -e "\n${YELLOW}🛑 Para detener el sistema:${NC}"
echo "./scripts/stop.sh"
