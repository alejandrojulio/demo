# ========================================
# SCRIPT DE TESTING PARA CREDIYA
# ========================================

param(
    [string]$Action = "help",
    [switch]$NoDaemon = $true
)

$microservices = @("AuthService", "LoadRequestService")
$ErrorActionPreference = "SilentlyContinue"

function Write-Title($text) {
    Write-Host "`n> $text" -ForegroundColor Cyan
}

function Write-Success($text) {
    Write-Host "[OK] $text" -ForegroundColor Green
}

function Write-Error($text) {
    Write-Host "[ERROR] $text" -ForegroundColor Red
}

function Write-Warning($text) {
    Write-Host "[WARNING] $text" -ForegroundColor Yellow
}

function Run-GradleCommand($service, $command, $description) {
    Write-Title "$description en $service"
    
    $gradlewPath = Join-Path $service "gradlew.bat"
    
    if (-not (Test-Path $gradlewPath)) {
        Write-Error "No se encontró gradlew.bat en $service"
        return $false
    }
    
    $arguments = @($command, "--continue")
    if ($NoDaemon) {
        $arguments += "--no-daemon"
    }
    
    try {
        $result = & $gradlewPath @arguments
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Success "$description exitoso en $service"
            return $true
        } else {
            Write-Error "$description falló en $service (código: $exitCode)"
            return $false
        }
    } catch {
        Write-Error "$description falló en $service - Error: $($_.Exception.Message)"
        return $false
    }
}

function Test-Unit {
    Write-Title "EJECUTANDO TESTS UNITARIOS"
    $allSuccess = $true
    
    foreach ($service in $microservices) {
        $success = Run-GradleCommand $service "test" "Tests unitarios"
        if (-not $success) { $allSuccess = $false }
    }
    
    if ($allSuccess) {
        Write-Success "Todos los tests unitarios completados exitosamente"
    } else {
        Write-Warning "Algunos tests unitarios fallaron"
    }
}

function Test-Integration {
    Write-Title "EJECUTANDO TESTS DE INTEGRACIÓN"
    $allSuccess = $true
    
    foreach ($service in $microservices) {
        $success = Run-GradleCommand $service "test --tests *IntegrationTest" "Tests de integración"
        if (-not $success) { $allSuccess = $false }
    }
    
    if ($allSuccess) {
        Write-Success "Todos los tests de integración completados exitosamente"
    } else {
        Write-Warning "Algunos tests de integración fallaron"
    }
}

function Test-All {
    Write-Title "EJECUTANDO TODOS LOS TESTS"
    Test-Unit
    Test-Integration
    Generate-Coverage
}

function Test-Architecture {
    Write-Title "EJECUTANDO TESTS DE ARQUITECTURA"
    $allSuccess = $true
    
    foreach ($service in $microservices) {
        $success = Run-GradleCommand $service "test --tests *ArchitectureTest" "Tests de arquitectura"
        if (-not $success) { $allSuccess = $false }
    }
    
    if ($allSuccess) {
        Write-Success "Todos los tests de arquitectura completados exitosamente"
    } else {
        Write-Warning "Algunos tests de arquitectura fallaron"
    }
}

function Generate-Coverage {
    Write-Title "GENERANDO REPORTES DE COBERTURA"
    
    foreach ($service in $microservices) {
        $success = Run-GradleCommand $service "jacocoTestReport" "Reporte de cobertura"
        if ($success) {
        $reportPath = Join-Path $service "build\reports\jacoco\test\html\index.html"
        Write-Host "[REPORT] Reporte disponible: $reportPath" -ForegroundColor Yellow
        }
    }
}

function Clean-All {
    Write-Title "LIMPIANDO PROYECTOS"
    
    foreach ($service in $microservices) {
        Run-GradleCommand $service "clean" "Limpieza"
    }
    
    Write-Success "Limpieza completada"
}

function Build-All {
    Write-Title "COMPILANDO PROYECTOS"
    $allSuccess = $true
    
    foreach ($service in $microservices) {
        $success = Run-GradleCommand $service "build -x test" "Compilación"
        if (-not $success) { $allSuccess = $false }
    }
    
    if ($allSuccess) {
        Write-Success "Todas las compilaciones exitosas"
    } else {
        Write-Error "Algunas compilaciones fallaron"
    }
}

function Quality-Check {
    Write-Title "VERIFICACIÓN COMPLETA DE CALIDAD"
    Test-All
    Test-Architecture
    
    Write-Title "RESUMEN DE REPORTES"
    foreach ($service in $microservices) {
        Write-Host "`n[$service]:" -ForegroundColor Cyan
        Write-Host "  Cobertura: $service\build\reports\jacoco\test\html\index.html"
        Write-Host "  Tests: $service\build\reports\tests\test\index.html"
        Write-Host "  Arquitectura: $service\build\issues.json"
    }
}

function Show-Help {
    Write-Host @"

SISTEMA DE TESTING CREDIYA
===============================

COMANDOS DISPONIBLES:

TESTING:
  .\run-tests.ps1 testUnit         - Tests unitarios
  .\run-tests.ps1 testIntegration  - Tests de integración  
  .\run-tests.ps1 testAll          - Todos los tests
  .\run-tests.ps1 testArchitecture - Tests de arquitectura

REPORTES:
  .\run-tests.ps1 coverage         - Reportes de cobertura

BUILD:
  .\run-tests.ps1 clean            - Limpiar todo
  .\run-tests.ps1 build            - Compilar todo
  .\run-tests.ps1 qualityCheck     - Verificación completa

EJEMPLOS:
  # Ejecutar solo tests unitarios
  .\run-tests.ps1 testUnit
  
  # Verificación completa de calidad
  .\run-tests.ps1 qualityCheck
  
  # Limpiar y ejecutar todos los tests
  .\run-tests.ps1 clean; .\run-tests.ps1 testAll

PARAMETROS:
  -NoDaemon     Usar --no-daemon (por defecto: true)

Los reportes se generan en:
  - AuthService\build\reports\
  - LoadRequestService\build\reports\

"@ -ForegroundColor Green
}

# MAIN LOGIC
switch ($Action.ToLower()) {
    "testunit" { Test-Unit }
    "testintegration" { Test-Integration }
    "testall" { Test-All }
    "testarchitecture" { Test-Architecture }
    "coverage" { Generate-Coverage }
    "clean" { Clean-All }
    "build" { Build-All }
    "qualitycheck" { Quality-Check }
    "help" { Show-Help }
    default { 
        Write-Error "Acción no reconocida: $Action"
        Show-Help 
    }
}
