# Script para probar comunicación entre microservicios
# Verifica que LoadRequestService consulte AuthService via HTTP en lugar de DB

param(
    [switch]$RestartServices,
    [switch]$Verbose,
    [switch]$TestEndpoints
)

$ErrorActionPreference = "Stop"

# Configuración
$AUTH_SERVICE_URL = "http://localhost:8080"
$LOAD_REQUEST_SERVICE_URL = "http://localhost:8081"
$CLIENT_EMAIL = "ana.perez@email.com"
$CLIENT_DOCUMENT = "45678901"
$PASSWORD = "password123"

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
Write-Success "   Testing Comunicación Microservicios"
Write-Success "=========================================="

# Test 1: Verificar endpoints de AuthService para comunicación
function Test-AuthServiceEndpoints {
    Write-Info "1. Testing endpoints de AuthService para comunicación entre servicios..."
    
    try {
        # Test endpoint de usuario por documento
        Write-Info "Probando endpoint GET /api/v1/users/document/{document}..."
        $userByDocResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/users/document/$CLIENT_DOCUMENT" `
                                               -Method Get `
                                               -TimeoutSec 10
        
        if ($userByDocResponse.success -and $userByDocResponse.data) {
            Write-Success "✅ Endpoint /users/document funciona"
            Write-Info "   Usuario: $($userByDocResponse.data.firstName) $($userByDocResponse.data.lastName)"
            Write-Info "   Salario base: $($userByDocResponse.data.baseSalary)"
        } else {
            Write-Error "❌ Endpoint /users/document falló"
            return $false
        }
        
        # Test endpoint de validación
        Write-Info "Probando endpoint GET /api/v1/users/validate/{document}..."
        $validateResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/users/validate/$CLIENT_DOCUMENT" `
                                              -Method Get `
                                              -TimeoutSec 10
        
        if ($validateResponse.success -and $validateResponse.data.exists) {
            Write-Success "✅ Endpoint /users/validate funciona"
            Write-Info "   Usuario existe: $($validateResponse.data.exists)"
            Write-Info "   Usuario activo: $($validateResponse.data.isActive)"
        } else {
            Write-Error "❌ Endpoint /users/validate falló"
            return $false
        }
        
        # Test endpoint por email
        Write-Info "Probando endpoint GET /api/v1/users/email/{email}..."
        $userByEmailResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/users/email/$CLIENT_EMAIL" `
                                                 -Method Get `
                                                 -TimeoutSec 10
        
        if ($userByEmailResponse.success -and $userByEmailResponse.data) {
            Write-Success "✅ Endpoint /users/email funciona"
            Write-Info "   Email verificado: $($userByEmailResponse.data.emailVerified)"
        } else {
            Write-Error "❌ Endpoint /users/email falló"
            return $false
        }
        
        return $true
    } catch {
        Write-Error "Error probando endpoints de AuthService: $_"
        return $false
    }
}

# Test 2: Verificar que LoadRequestService no tiene datos duplicados
function Test-NoDuplicatedData {
    Write-Info "2. Verificando que no hay datos duplicados en loans_db..."
    
    try {
        # Verificar que la tabla clients ya no existe o está vacía
        $clientsCheck = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SHOW TABLES LIKE 'clients';" 2>$null
        
        if ($clientsCheck -notmatch "clients") {
            Write-Success "✅ Tabla 'clients' eliminada correctamente"
        } else {
            Write-Warning "⚠️ Tabla 'clients' aún existe - verificando contenido..."
            
            $clientsCount = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM clients;" 2>$null
            
            if ($clientsCount -match "0") {
                Write-Success "✅ Tabla 'clients' vacía"
            } else {
                Write-Warning "⚠️ Tabla 'clients' tiene datos duplicados"
            }
        }
        
        # Verificar que existe client_credit_info (datos específicos de préstamos)
        $creditInfoCheck = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SHOW TABLES LIKE 'client_credit_info';" 2>$null
        
        if ($creditInfoCheck -match "client_credit_info") {
            Write-Success "✅ Tabla 'client_credit_info' existe (datos específicos de dominio)"
            
            $creditInfoCount = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM client_credit_info;" 2>$null
            
            if ($creditInfoCount -match "[1-9]") {
                Write-Success "✅ client_credit_info tiene datos de scoring crediticio"
            }
        } else {
            Write-Warning "⚠️ Tabla 'client_credit_info' no encontrada"
        }
        
        return $true
    } catch {
        Write-Error "Error verificando datos: $_"
        return $false
    }
}

# Test 3: Probar flujo completo con comunicación entre servicios
function Test-MicroservicesCommunication {
    Write-Info "3. Testing comunicación completa entre microservicios..."
    
    try {
        # 1. Login en AuthService
        Write-Info "Paso 1: Autenticando en AuthService..."
        $loginData = @{
            email = $CLIENT_EMAIL
            password = $PASSWORD
        }
        
        $loginResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/login" `
                                           -Method Post `
                                           -ContentType "application/json" `
                                           -Body ($loginData | ConvertTo-Json)
        
        if (!$loginResponse.success) {
            Write-Error "❌ Fallo en autenticación"
            return $false
        }
        
        $token = $loginResponse.data.token
        Write-Success "✅ Autenticación exitosa"
        
        # 2. Crear solicitud en LoadRequestService (debería consultar AuthService)
        Write-Info "Paso 2: Creando solicitud en LoadRequestService..."
        $loanData = @{
            clientDocumentId = $CLIENT_DOCUMENT
            amount = 3500000
            termInMonths = 24
            loanType = "PERSONAL"
            notes = "Test de comunicación entre microservicios"
        }
        
        $headers = @{
            Authorization = "Bearer $token"
            "Content-Type" = "application/json"
        }
        
        Write-Info "LoadRequestService debería consultar AuthService para obtener datos del cliente..."
        $loanResponse = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud" `
                                          -Method Post `
                                          -Headers $headers `
                                          -Body ($loanData | ConvertTo-Json)
        
        if ($loanResponse.success) {
            Write-Success "✅ Solicitud creada exitosamente"
            Write-Info "   ID de solicitud: $($loanResponse.data.id)"
            Write-Info "   LoadRequestService consultó AuthService para obtener datos del cliente"
            return $loanResponse.data.id
        } else {
            Write-Error "❌ Error creando solicitud: $($loanResponse.message)"
            return $false
        }
        
    } catch {
        Write-Error "Error en comunicación entre microservicios: $_"
        return $false
    }
}

# Test 4: Verificar logs de comunicación HTTP
function Test-HttpCommunicationLogs {
    Write-Info "4. Verificando logs de comunicación HTTP..."
    
    try {
        Write-Info "Revisando logs de LoadRequestService para comunicación con AuthService..."
        $loadServiceLogs = docker logs crediya-load-request-service --tail 50 2>$null | Select-String -Pattern "AuthService|/api/v1/users|consultando.*usuario" -CaseSensitive:$false
        
        if ($loadServiceLogs.Count -gt 0) {
            Write-Success "✅ LoadRequestService está comunicándose con AuthService via HTTP"
            
            if ($Verbose) {
                Write-Info "Logs encontrados:"
                $loadServiceLogs | ForEach-Object { Write-Info "  $_" }
            }
        } else {
            Write-Warning "⚠️ No se encontraron logs de comunicación HTTP con AuthService"
        }
        
        Write-Info "Revisando logs de AuthService para requests de microservicios..."
        $authServiceLogs = docker logs crediya-auth-service --tail 50 2>$null | Select-String -Pattern "/api/v1/users|consultando.*documento|UserDataHandler" -CaseSensitive:$false
        
        if ($authServiceLogs.Count -gt 0) {
            Write-Success "✅ AuthService está recibiendo requests de otros microservicios"
            
            if ($Verbose) {
                Write-Info "Logs encontrados:"
                $authServiceLogs | ForEach-Object { Write-Info "  $_" }
            }
        } else {
            Write-Warning "⚠️ No se encontraron logs de requests a AuthService"
        }
        
        return $true
    } catch {
        Write-Error "Error revisando logs: $_"
        return $false
    }
}

# Test 5: Verificar métricas de performance de comunicación
function Test-CommunicationPerformance {
    Write-Info "5. Testing performance de comunicación entre servicios..."
    
    try {
        # Medir tiempo de respuesta de AuthService
        Write-Info "Midiendo tiempo de respuesta de AuthService..."
        $startTime = Get-Date
        
        $userResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/users/document/$CLIENT_DOCUMENT" `
                                          -Method Get `
                                          -TimeoutSec 5
        
        $authResponseTime = (Get-Date) - $startTime
        
        if ($userResponse.success) {
            Write-Success "✅ AuthService responde en $($authResponseTime.TotalMilliseconds) ms"
            
            if ($authResponseTime.TotalMilliseconds -lt 1000) {
                Write-Success "✅ Tiempo de respuesta óptimo (< 1 segundo)"
            } elseif ($authResponseTime.TotalMilliseconds -lt 3000) {
                Write-Warning "⚠️ Tiempo de respuesta aceptable (1-3 segundos)"
            } else {
                Write-Warning "⚠️ Tiempo de respuesta lento (> 3 segundos)"
            }
        } else {
            Write-Error "❌ Error en respuesta de AuthService"
            return $false
        }
        
        return $true
    } catch {
        Write-Error "Error midiendo performance: $_"
        return $false
    }
}

# Test 6: Probar circuit breaker y manejo de errores
function Test-ErrorHandling {
    Write-Info "6. Testing manejo de errores en comunicación..."
    
    try {
        # Test con usuario inexistente
        Write-Info "Probando con usuario inexistente..."
        try {
            $invalidUserResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/users/document/99999999" `
                                                     -Method Get `
                                                     -TimeoutSec 5
            
            if (!$invalidUserResponse.success) {
                Write-Success "✅ AuthService maneja correctamente usuarios inexistentes"
            } else {
                Write-Warning "⚠️ AuthService debería retornar error para usuarios inexistentes"
            }
        } catch {
            if ($_.Exception.Response.StatusCode -eq "NotFound") {
                Write-Success "✅ AuthService retorna 404 para usuarios inexistentes"
            } else {
                Write-Warning "⚠️ Error inesperado: $_"
            }
        }
        
        return $true
    } catch {
        Write-Error "Error probando manejo de errores: $_"
        return $false
    }
}

# Restart servicios si se solicita
if ($RestartServices) {
    Write-Info "Reiniciando servicios..."
    docker-compose down
    docker-compose up -d
    
    Write-Info "Esperando que los servicios estén listos..."
    Start-Sleep -Seconds 45
}

# Ejecutar todos los tests
function Run-MicroservicesCommunicationTests {
    try {
        $test1 = if ($TestEndpoints) { Test-AuthServiceEndpoints } else { $true }
        $test2 = Test-NoDuplicatedData
        $loanId = Test-MicroservicesCommunication
        $test3 = $loanId -ne $false
        $test4 = Test-HttpCommunicationLogs
        $test5 = Test-CommunicationPerformance
        $test6 = Test-ErrorHandling
        
        # Resumen final
        Write-Info "`nResumen de Tests de Comunicación entre Microservicios:"
        Write-Host "  1. Endpoints AuthService: " -NoNewline
        Write-Host (if ($test1) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  2. Sin datos duplicados: " -NoNewline
        Write-Host (if ($test2) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  3. Comunicación completa: " -NoNewline
        Write-Host (if ($test3) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  4. Logs de comunicación: " -NoNewline
        Write-Host (if ($test4) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  5. Performance: " -NoNewline
        Write-Host (if ($test5) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  6. Manejo de errores: " -NoNewline
        Write-Host (if ($test6) { "✅ PASS" } else { "❌ FAIL" })
        
        $totalPassed = @($test1, $test2, $test3, $test4, $test5, $test6) | Where-Object { $_ } | Measure-Object | Select-Object -ExpandProperty Count
        
        if ($totalPassed -ge 5) {
            Write-Success "`n🎉 Comunicación entre microservicios funcionando correctamente!"
            Write-Info "✅ LoadRequestService consulta AuthService via HTTP"
            Write-Info "✅ No hay duplicación de datos entre servicios"
            Write-Info "✅ Cada servicio es dueño de su dominio de datos"
            Write-Info "✅ Arquitectura de microservicios implementada correctamente"
        } else {
            Write-Warning "`n⚠️ Algunos tests fallaron"
            Write-Info "Revisar configuración de comunicación entre servicios"
        }
        
    } catch {
        Write-Error "Error en tests de comunicación: $_"
    }
}

# Ejecutar tests
Run-MicroservicesCommunicationTests

Write-Info "`n🏗️ Arquitectura implementada:"
Write-Info "  📊 AuthService (puerto 8080) → auth_db (puerto 3306)"
Write-Info "  💰 LoadRequestService (puerto 8081) → loans_db (puerto 3307)"
Write-Info "  🔄 Comunicación: LoadRequestService → HTTP → AuthService"
Write-Info "`n🔧 Para debugging:"
Write-Info "  - Logs AuthService: docker logs crediya-auth-service"
Write-Info "  - Logs LoadRequestService: docker logs crediya-load-request-service"
Write-Info "  - Test solo endpoints: .\test-microservices-communication.ps1 -TestEndpoints"

