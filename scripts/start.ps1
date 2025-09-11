# Script para iniciar el sistema CrediYa con Docker
# Requiere Docker Desktop instalado y ejecutándose

Write-Host "=========================================="  -ForegroundColor Green
Write-Host "   Iniciando Sistema CrediYa"  -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

# Verificar que Docker esté disponible
try {
    docker --version | Out-Null
    docker-compose --version | Out-Null
} catch {
    Write-Host "[ERROR] Docker no está instalado o no está disponible en el PATH" -ForegroundColor Red
    Write-Host "Instala Docker Desktop desde: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
    exit 1
}

# Limpiar contenedores anteriores
Write-Host "[INFO] Limpiando contenedores anteriores..." -ForegroundColor Blue
docker-compose down --volumes --remove-orphans 2>$null

# Construir imágenes
Write-Host "[INFO] Construyendo imágenes de los microservicios..." -ForegroundColor Blue
docker-compose build --no-cache

# Iniciar servicios
Write-Host "[INFO] Iniciando servicios..." -ForegroundColor Blue
Write-Host "[INFO] 1. Iniciando base de datos..." -ForegroundColor Blue
docker-compose up -d mysql-crediya

# Esperar base de datos
Write-Host "[INFO] Esperando a que la base de datos esté lista..." -ForegroundColor Blue
$timeout = 120
$counter = 0

do {
    Start-Sleep -Seconds 2
    $counter += 2
    Write-Host "." -NoNewline
    $crediyaReady = docker-compose exec -T mysql-crediya mysqladmin ping -h localhost -u crediya_user -pcrediya_pass --silent 2>$null
} while (!$crediyaReady -and $counter -lt $timeout)

if ($counter -ge $timeout) {
    Write-Host "`n[ERROR] Timeout esperando la base de datos CrediYa" -ForegroundColor Red
    exit 1
}
Write-Host "`n[SUCCESS] Base de datos CrediYa está lista" -ForegroundColor Green

# Iniciar microservicios
Write-Host "[INFO] 2. Iniciando microservicio de Autenticación..." -ForegroundColor Blue
docker-compose up -d auth-service

# Esperar AuthService
Write-Host "[INFO] Esperando AuthService..." -ForegroundColor Blue
$counter = 0
do {
    Start-Sleep -Seconds 3
    $counter += 3
    Write-Host "." -NoNewline
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 2>$null
        $authReady = $response.status -eq "UP"
    } catch {
        $authReady = $false
    }
} while (!$authReady -and $counter -lt $timeout)

if ($counter -ge $timeout) {
    Write-Host "`n[ERROR] Timeout esperando AuthService" -ForegroundColor Red
    exit 1
}
Write-Host "`n[SUCCESS] AuthService está listo" -ForegroundColor Green

# Iniciar LoadRequestService
Write-Host "[INFO] 3. Iniciando microservicio de Solicitudes..." -ForegroundColor Blue
docker-compose up -d load-request-service

# Esperar LoadRequestService
Write-Host "[INFO] Esperando LoadRequestService..." -ForegroundColor Blue
$counter = 0
do {
    Start-Sleep -Seconds 3
    $counter += 3
    Write-Host "." -NoNewline
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -TimeoutSec 2 2>$null
        $loadRequestReady = $response.status -eq "UP"
    } catch {
        $loadRequestReady = $false
    }
} while (!$loadRequestReady -and $counter -lt $timeout)

if ($counter -ge $timeout) {
    Write-Host "`n[ERROR] Timeout esperando LoadRequestService" -ForegroundColor Red
    exit 1
}
Write-Host "`n[SUCCESS] LoadRequestService está listo" -ForegroundColor Green

# Mostrar información final
Write-Host "`n=========================================="  -ForegroundColor Green
Write-Host "   Sistema CrediYa iniciado exitosamente"  -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

Write-Host "`nServicios disponibles:" -ForegroundColor Blue
Write-Host "Autenticación:    http://localhost:8080"
Write-Host "Solicitudes:      http://localhost:8081"
Write-Host "MySQL CrediYa:    localhost:3307"

Write-Host "`nHealth Checks:" -ForegroundColor Blue
Write-Host "Auth Health:      http://localhost:8080/actuator/health"
Write-Host "Requests Health:  http://localhost:8081/actuator/health"

Write-Host "`nUsuarios de prueba:" -ForegroundColor Blue
Write-Host "Admin:     admin@crediya.com     / password123"
Write-Host "Asesor:    asesor@crediya.com    / password123"  
Write-Host "Cliente:   ana.perez@email.com   / password123"

Write-Host "`nPara ver logs en tiempo real:" -ForegroundColor Yellow
Write-Host "docker-compose logs -f"

Write-Host "`nPara detener el sistema:" -ForegroundColor Yellow
Write-Host ".\scripts\stop.ps1"