# Script de testing integral para el sistema de capacidad de endeudamiento
# Requiere Docker y servicios ejecutándose

param(
    [switch]$SkipDockerStart,
    [switch]$Verbose,
    [string]$TestTarget = "all" # all, lambda, api, integration
)

$ErrorActionPreference = "Stop"

# Configuración
$AUTH_SERVICE_URL = "http://localhost:8080"
$LOAD_REQUEST_SERVICE_URL = "http://localhost:8081"
$ADMIN_EMAIL = "admin@crediya.com"
$CLIENT_EMAIL = "ana.perez@email.com"
$PASSWORD = "password123"

# Colores para output
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Write-Success($msg) { Write-ColorOutput Green $msg }
function Write-Error($msg) { Write-ColorOutput Red $msg }
function Write-Warning($msg) { Write-ColorOutput Yellow $msg }
function Write-Info($msg) { Write-ColorOutput Cyan $msg }

Write-Success "=========================================="
Write-Success "   Testing Sistema Capacidad Endeudamiento"
Write-Success "=========================================="

# Verificar dependencias
function Test-Dependencies {
    Write-Info "Verificando dependencias..."
    
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Error "Docker no está instalado o no está en el PATH"
        exit 1
    }
    
    if (!(Get-Command curl -ErrorAction SilentlyContinue)) {
        Write-Warning "curl no encontrado, usando Invoke-RestMethod como alternativa"
    }
    
    Write-Success "✅ Dependencias verificadas"
}

# Iniciar servicios Docker
function Start-Services {
    if ($SkipDockerStart) {
        Write-Info "Saltando inicio de servicios Docker..."
        return
    }
    
    Write-Info "Iniciando servicios con Docker..."
    
    try {
        # Verificar si los servicios ya están ejecutándose
        $authHealth = Test-ServiceHealth $AUTH_SERVICE_URL "/actuator/health"
        $loadHealth = Test-ServiceHealth $LOAD_REQUEST_SERVICE_URL "/actuator/health"
        
        if ($authHealth -and $loadHealth) {
            Write-Info "Los servicios ya están ejecutándose"
            return
        }
        
        # Iniciar servicios
        Write-Info "Ejecutando docker-compose..."
        docker-compose up -d
        
        # Esperar que los servicios estén listos
        Wait-ForServices
        
    } catch {
        Write-Error "Error iniciando servicios: $_"
        exit 1
    }
}

# Probar conectividad de servicios
function Test-ServiceHealth($baseUrl, $healthPath) {
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl$healthPath" -Method Get -TimeoutSec 5
        return $response.status -eq "UP"
    } catch {
        return $false
    }
}

function Wait-ForServices {
    Write-Info "Esperando que los servicios estén listos..."
    
    $maxWait = 120 # 2 minutos
    $waited = 0
    
    while ($waited -lt $maxWait) {
        $authReady = Test-ServiceHealth $AUTH_SERVICE_URL "/actuator/health"
        $loadReady = Test-ServiceHealth $LOAD_REQUEST_SERVICE_URL "/actuator/health"
        
        if ($authReady -and $loadReady) {
            Write-Success "✅ Servicios listos"
            return
        }
        
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
        $waited += 2
    }
    
    Write-Error "`n❌ Timeout esperando servicios"
    exit 1
}

# Obtener token de autenticación
function Get-AuthToken($email, $password) {
    Write-Info "Obteniendo token de autenticación para $email..."
    
    try {
        $loginData = @{
            email = $email
            password = $password
        }
        
        $response = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/login" `
                                      -Method Post `
                                      -ContentType "application/json" `
                                      -Body ($loginData | ConvertTo-Json)
        
        if ($response.success -and $response.data.token) {
            Write-Success "✅ Token obtenido exitosamente"
            return $response.data.token
        } else {
            throw "Respuesta de login inválida"
        }
    } catch {
        Write-Error "Error obteniendo token: $_"
        throw
    }
}

# Test de creación de solicitud con validación automática
function Test-AutomaticValidationFlow($token) {
    Write-Info "Testing flujo de validación automática..."
    
    try {
        # Crear solicitud de préstamo personal (validación automática habilitada)
        $loanData = @{
            clientDocumentId = "45678901"  # Ana Pérez
            amount = 8500000
            termInMonths = 36
            loanType = "PERSONAL"
            notes = "Préstamo para consolidación de deudas - Test automático"
        }
        
        $headers = @{
            Authorization = "Bearer $token"
            "Content-Type" = "application/json"
        }
        
        Write-Info "Creando solicitud de préstamo..."
        $response = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud" `
                                      -Method Post `
                                      -Headers $headers `
                                      -Body ($loanData | ConvertTo-Json)
        
        if ($response.success) {
            $loanId = $response.data.id
            Write-Success "✅ Solicitud creada con ID: $loanId"
            
            # Esperar un momento para que se procese la validación automática
            Write-Info "Esperando procesamiento de validación automática..."
            Start-Sleep -Seconds 10
            
            # Verificar el estado de la solicitud
            Test-LoanStatus $token $loanId
            
            return $loanId
        } else {
            throw "Error creando solicitud: $($response.message)"
        }
    } catch {
        Write-Error "Error en flujo de validación automática: $_"
        throw
    }
}

function Test-LoanStatus($token, $loanId) {
    Write-Info "Verificando estado de solicitud $loanId..."
    
    try {
        $headers = @{
            Authorization = "Bearer $token"
        }
        
        # Obtener solicitudes para revisión
        $response = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud?page=0&size=20" `
                                      -Method Get `
                                      -Headers $headers
        
        if ($response.success) {
            $loan = $response.data.content | Where-Object { $_.id -eq $loanId }
            
            if ($loan) {
                Write-Info "Estado actual: $($loan.estadoSolicitud)"
                
                switch ($loan.estadoSolicitud) {
                    "PENDING_REVIEW" { Write-Warning "⏳ Solicitud aún pendiente de revisión" }
                    "APPROVED" { Write-Success "✅ Solicitud APROBADA automáticamente" }
                    "REJECTED" { Write-Warning "❌ Solicitud RECHAZADA automáticamente" }
                    "MANUAL_REVIEW" { Write-Warning "👤 Solicitud requiere REVISIÓN MANUAL" }
                    default { Write-Warning "❓ Estado desconocido: $($loan.estadoSolicitud)" }
                }
                
                return $loan.estadoSolicitud
            } else {
                Write-Warning "Solicitud no encontrada en la lista"
            }
        }
    } catch {
        Write-Warning "Error verificando estado: $_"
    }
}

# Test de la Lambda directamente
function Test-LambdaFunction {
    Write-Info "Testing Lambda de capacidad de endeudamiento..."
    
    if (!(Test-Path "DebtCapacityService/test_lambda.py")) {
        Write-Warning "Archivo de test de Lambda no encontrado, saltando..."
        return
    }
    
    try {
        Push-Location "DebtCapacityService"
        
        Write-Info "Ejecutando tests unitarios de la Lambda..."
        python test_lambda.py
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✅ Tests de Lambda exitosos"
        } else {
            Write-Error "❌ Tests de Lambda fallaron"
        }
        
    } catch {
        Write-Error "Error ejecutando tests de Lambda: $_"
    } finally {
        Pop-Location
    }
}

# Test de endpoint directo de capacidad
function Test-CapacityEndpoint($token) {
    Write-Info "Testing endpoint directo de capacidad..."
    
    # Este test requeriría que la Lambda esté desplegada en AWS
    # Por ahora solo verificamos que el flujo de creación funcione
    Write-Warning "Endpoint directo requiere Lambda desplegada en AWS - Saltando..."
}

# Test de diferentes escenarios
function Test-DifferentScenarios($token) {
    Write-Info "Testing diferentes escenarios..."
    
    $scenarios = @(
        @{
            name = "Préstamo Vehicular (Automático)"
            data = @{
                clientDocumentId = "56789012"  # Juan Rodríguez  
                amount = 25000000
                termInMonths = 60
                loanType = "VEHICLE"
                notes = "Préstamo vehicular - Test automático"
            }
        },
        @{
            name = "Préstamo Hipotecario (Manual)"
            data = @{
                clientDocumentId = "78901234"  # Roberto López
                amount = 80000000
                termInMonths = 180
                loanType = "HOME"
                notes = "Préstamo hipotecario - Test manual"
            }
        },
        @{
            name = "Préstamo Empresarial (Manual)"
            data = @{
                clientDocumentId = "67890123"  # Laura González
                amount = 45000000
                termInMonths = 84
                loanType = "BUSINESS"
                notes = "Préstamo empresarial - Test manual"
            }
        }
    )
    
    foreach ($scenario in $scenarios) {
        Write-Info "Probando: $($scenario.name)"
        
        try {
            $headers = @{
                Authorization = "Bearer $token"
                "Content-Type" = "application/json"
            }
            
            $response = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud" `
                                          -Method Post `
                                          -Headers $headers `
                                          -Body ($scenario.data | ConvertTo-Json)
            
            if ($response.success) {
                Write-Success "✅ $($scenario.name) - Solicitud creada con ID: $($response.data.id)"
                
                # Breve pausa para procesamiento
                Start-Sleep -Seconds 3
                
            } else {
                Write-Warning "⚠️ $($scenario.name) - Error: $($response.message)"
            }
        } catch {
            Write-Warning "⚠️ $($scenario.name) - Excepción: $_"
        }
    }
}

# Test de carga básico
function Test-LoadBasic($token) {
    Write-Info "Testing carga básica (10 solicitudes simultáneas)..."
    
    $jobs = @()
    
    for ($i = 1; $i -le 10; $i++) {
        $job = Start-Job -ScriptBlock {
            param($url, $token, $i)
            
            $loanData = @{
                clientDocumentId = "45678901"
                amount = 3000000 + ($i * 100000)
                termInMonths = 24 + ($i * 2)
                loanType = "PERSONAL"
                notes = "Test de carga #$i"
            }
            
            $headers = @{
                Authorization = "Bearer $token"
                "Content-Type" = "application/json"
            }
            
            try {
                $response = Invoke-RestMethod -Uri "$url/api/v1/solicitud" `
                                              -Method Post `
                                              -Headers $headers `
                                              -Body ($loanData | ConvertTo-Json)
                return @{ success = $response.success; id = $response.data.id; error = $null }
            } catch {
                return @{ success = $false; id = $null; error = $_.Exception.Message }
            }
        } -ArgumentList $LOAD_REQUEST_SERVICE_URL, $token, $i
        
        $jobs += $job
    }
    
    # Esperar todos los jobs
    $results = $jobs | Wait-Job | Receive-Job
    $jobs | Remove-Job
    
    $successful = ($results | Where-Object { $_.success }).Count
    $failed = ($results | Where-Object { -not $_.success }).Count
    
    Write-Info "Resultados de carga: $successful exitosas, $failed fallidas"
    
    if ($successful -gt 7) {  # Al menos 70% exitosas
        Write-Success "✅ Test de carga básico exitoso"
    } else {
        Write-Warning "⚠️ Test de carga con problemas"
    }
}

# Función principal de testing
function Run-Tests {
    Test-Dependencies
    
    if ($TestTarget -in @("all", "services")) {
        Start-Services
    }
    
    if ($TestTarget -in @("all", "lambda")) {
        Test-LambdaFunction
    }
    
    if ($TestTarget -in @("all", "api", "integration")) {
        try {
            # Obtener tokens
            $adminToken = Get-AuthToken $ADMIN_EMAIL $PASSWORD
            $clientToken = Get-AuthToken $CLIENT_EMAIL $PASSWORD
            
            if ($TestTarget -in @("all", "integration")) {
                # Tests de integración
                Test-AutomaticValidationFlow $clientToken
                Test-DifferentScenarios $clientToken
                Test-LoadBasic $clientToken
            }
            
            if ($TestTarget -in @("all", "api")) {
                Test-CapacityEndpoint $adminToken
            }
            
        } catch {
            Write-Error "Error en tests de API/integración: $_"
            exit 1
        }
    }
    
    Write-Success "`n🎉 Testing completado exitosamente!"
    Write-Info "Revisa los logs de los servicios para más detalles sobre el procesamiento automático."
}

# Ejecutar tests
try {
    Run-Tests
} catch {
    Write-Error "Error durante testing: $_"
    exit 1
}

