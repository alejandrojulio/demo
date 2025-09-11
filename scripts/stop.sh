#!/bin/bash

# Script para detener el sistema CrediYa
# Uso: ./scripts/stop.sh [--remove-volumes]

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

# Banner
echo -e "${RED}"
echo "=========================================="
echo "    CrediYa - Sistema de Préstamos"
echo "       Deteniendo Contenedores Docker"
echo "=========================================="
echo -e "${NC}"

# Verificar si se debe eliminar volúmenes
REMOVE_VOLUMES=false
if [[ "$1" == "--remove-volumes" ]]; then
    REMOVE_VOLUMES=true
    log_warning "Se eliminarán los volúmenes de datos (¡Se perderán todos los datos!)"
    read -p "¿Estás seguro? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Operación cancelada."
        exit 0
    fi
fi

# Detener servicios
log_info "Deteniendo servicios..."
docker-compose down --remove-orphans

# Eliminar volúmenes si se solicita
if [ "$REMOVE_VOLUMES" = true ]; then
    log_warning "Eliminando volúmenes de datos..."
    docker-compose down --volumes
    docker volume prune -f
fi

# Limpiar imágenes no utilizadas (opcional)
if [ "$1" == "--clean" ] || [ "$2" == "--clean" ]; then
    log_info "Limpiando imágenes no utilizadas..."
    docker image prune -f
fi

# Estado final
echo -e "\n${GREEN}=========================================="
echo "   ✅ Sistema CrediYa detenido exitosamente"
echo "==========================================${NC}"

if [ "$REMOVE_VOLUMES" = false ]; then
    echo -e "\n${BLUE}💾 Los datos se han preservado.${NC}"
    echo "Para eliminar todos los datos, usa: ./scripts/stop.sh --remove-volumes"
fi

echo -e "\n${BLUE}🚀 Para iniciar nuevamente:${NC}"
echo "./scripts/start.sh"
