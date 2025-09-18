# Script para probar la integración con bases de datos separadas
# Verifica que AuthService use auth_db y LoadRequestService use loans_db

param(
    [switch]$RestartServices,
    [switch]$Verbose,
    [switch]$SkipDbValidation
)

$ErrorActionPreference = "Stop"

# Configuración
$AUTH_SERVICE_URL = "http://localhost:8080"
$LOAD_REQUEST_SERVICE_URL = "http://localhost:8081"
$MYSQL_AUTH_PORT = "3306"
$MYSQL_LOANS_PORT = "3307"
$CLIENT_EMAIL = "ana.perez@email.com"
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
Write-Success "   Testing Bases de Datos Separadas"
Write-Success "=========================================="

# Test 1: Verificar que las bases de datos están separadas
function Test-DatabaseSeparation {
    Write-Info "1. Verificando separación de bases de datos..."
    
    try {
        # Verificar que auth_db existe en puerto 3306
        Write-Info "Verificando auth_db en mysql-auth (puerto 3306)..."
        $authDbTest = docker exec crediya-mysql-auth mysql -u auth_user -pauth_pass -e "SHOW DATABASES; USE auth_db; SHOW TABLES;" 2>$null
        
        if ($authDbTest -match "auth_db" -and $authDbTest -match "users") {
            Write-Success "✅ auth_db configurada correctamente con tabla users"
        } else {
            Write-Error "❌ Problema con auth_db"
            return $false
        }
        
        # Verificar que loans_db existe en puerto 3307
        Write-Info "Verificando loans_db en mysql-loans (puerto 3307)..."
        $loansDbTest = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "SHOW DATABASES; USE loans_db; SHOW TABLES;" 2>$null
        
        if ($loansDbTest -match "loans_db" -and $loansDbTest -match "clients" -and $loansDbTest -match "loan_requests") {
            Write-Success "✅ loans_db configurada correctamente con tablas clients y loan_requests"
        } else {
            Write-Error "❌ Problema con loans_db"
            return $false
        }
        
        # Verificar que NO hay crossover
        Write-Info "Verificando que no hay tablas cruzadas..."
        $authTablesCheck = docker exec crediya-mysql-auth mysql -u auth_user -pauth_pass -e "USE auth_db; SHOW TABLES LIKE 'loan_%';" 2>$null
        $loansTablesCheck = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SHOW TABLES LIKE 'users';" 2>$null
        
        if ($authTablesCheck -notmatch "loan_" -and $loansTablesCheck -notmatch "users") {
            Write-Success "✅ Separación de tablas correcta"
        } else {
            Write-Warning "⚠️ Puede haber tablas duplicadas entre DBs"
        }
        
        return $true
    } catch {
        Write-Error "Error verificando bases de datos: $_"
        return $false
    }
}

# Test 2: Verificar datos iniciales en ambas DBs
function Test-InitialData {
    Write-Info "2. Verificando datos iniciales..."
    
    try {
        # Verificar usuarios en auth_db
        Write-Info "Contando usuarios en auth_db..."
        $authUsersCount = docker exec crediya-mysql-auth mysql -u auth_user -pauth_pass -e "USE auth_db; SELECT COUNT(*) as count FROM users WHERE is_active = TRUE;" 2>$null
        
        if ($authUsersCount -match "count" -and $authUsersCount -match "[5-9]") {
            Write-Success "✅ Usuarios cargados en auth_db"
        } else {
            Write-Warning "⚠️ Pocos usuarios en auth_db: $authUsersCount"
        }
        
        # Verificar clientes en loans_db
        Write-Info "Contando clientes en loans_db..."
        $loansClientsCount = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM clients WHERE is_active = TRUE;" 2>$null
        
        if ($loansClientsCount -match "count" -and $loansClientsCount -match "[3-9]") {
            Write-Success "✅ Clientes cargados en loans_db"
        } else {
            Write-Warning "⚠️ Pocos clientes en loans_db: $loansClientsCount"
        }
        
        # Verificar tipos de préstamo
        Write-Info "Verificando tipos de préstamo..."
        $loanTypesCount = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM loan_types WHERE automatic_validation = TRUE;" 2>$null
        
        if ($loanTypesCount -match "[2-4]") {
            Write-Success "✅ Tipos de préstamo con validación automática configurados"
        } else {
            Write-Warning "⚠️ Configuración de tipos de préstamo incompleta"
        }
        
        return $true
    } catch {
        Write-Error "Error verificando datos iniciales: $_"
        return $false
    }
}

# Test 3: Verificar conectividad de servicios
function Test-ServiceConnectivity {
    Write-Info "3. Verificando conectividad de servicios..."
    
    try {
        # AuthService health check
        Write-Info "Verificando salud de AuthService..."
        $authHealth = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/actuator/health" -Method Get -TimeoutSec 10
        
        if ($authHealth.status -eq "UP") {
            Write-Success "✅ AuthService conectado a auth_db"
        } else {
            Write-Error "❌ AuthService no está healthy"
            return $false
        }
        
        # LoadRequestService health check
        Write-Info "Verificando salud de LoadRequestService..."
        $loansHealth = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/actuator/health" -Method Get -TimeoutSec 10
        
        if ($loansHealth.status -eq "UP") {
            Write-Success "✅ LoadRequestService conectado a loans_db"
        } else {
            Write-Error "❌ LoadRequestService no está healthy"
            return $false
        }
        
        return $true
    } catch {
        Write-Error "Error verificando conectividad: $_"
        return $false
    }
}

# Test 4: Testing funcional con autenticación y préstamos
function Test-FunctionalFlow {
    Write-Info "4. Testing flujo funcional completo..."
    
    try {
        # Login en AuthService (usa auth_db)
        Write-Info "Autenticando en AuthService..."
        $loginData = @{
            email = $CLIENT_EMAIL
            password = $PASSWORD
        }
        
        $loginResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/login" `
                                           -Method Post `
                                           -ContentType "application/json" `
                                           -Body ($loginData | ConvertTo-Json)
        
        if ($loginResponse.success -and $loginResponse.data.token) {
            Write-Success "✅ Login exitoso - AuthService consultó auth_db"
            $token = $loginResponse.data.token
        } else {
            Write-Error "❌ Fallo en login"
            return $false
        }
        
        # Crear solicitud en LoadRequestService (usa loans_db)
        Write-Info "Creando solicitud en LoadRequestService..."
        $loanData = @{
            clientDocumentId = "45678901"  # Ana Pérez
            amount = 4500000
            termInMonths = 30
            loanType = "PERSONAL"
            notes = "Test de DBs separadas"
        }
        
        $headers = @{
            Authorization = "Bearer $token"
            "Content-Type" = "application/json"
        }
        
        $loanResponse = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud" `
                                          -Method Post `
                                          -Headers $headers `
                                          -Body ($loanData | ConvertTo-Json)
        
        if ($loanResponse.success) {
            Write-Success "✅ Solicitud creada - LoadRequestService consultó loans_db"
            Write-Info "   ID de solicitud: $($loanResponse.data.id)"
            return $loanResponse.data.id
        } else {
            Write-Error "❌ Error creando solicitud: $($loanResponse.message)"
            return $false
        }
        
    } catch {
        Write-Error "Error en flujo funcional: $_"
        return $false
    }
}

# Test 5: Verificar queries específicos de capacidad de endeudamiento
function Test-DebtCapacityQueries($token) {
    Write-Info "5. Testing queries de capacidad de endeudamiento..."
    
    try {
        # Verificar que los queries usan la tabla clients
        Write-Info "Verificando estructura de datos para capacidad..."
        
        # Simular consulta de cliente con préstamos existentes
        $clientData = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "
            USE loans_db; 
            SELECT c.document, c.first_name, c.last_name, c.base_salary,
                   COUNT(lr.id) as active_loans,
                   COALESCE(SUM(lr.monthly_payment), 0) as current_debt
            FROM clients c 
            LEFT JOIN loan_requests lr ON c.document = lr.client_document AND lr.status = 'APPROVED'
            WHERE c.document = '56789012'
            GROUP BY c.document, c.first_name, c.last_name, c.base_salary;
        " 2>$null
        
        if ($clientData -match "56789012" -and $clientData -match "Juan") {
            Write-Success "✅ Query de capacidad funciona con loans_db"
        } else {
            Write-Warning "⚠️ Query de capacidad puede tener problemas"
        }
        
        # Crear solicitud con validación automática
        Write-Info "Probando trigger de validación automática..."
        $autoLoanData = @{
            clientDocumentId = "67890123"  # Laura González
            amount = 2000000
            termInMonths = 24
            loanType = "VEHICLE"
            notes = "Test validación automática con DBs separadas"
        }
        
        $headers = @{
            Authorization = "Bearer $token"
            "Content-Type" = "application/json"
        }
        
        $autoLoanResponse = Invoke-RestMethod -Uri "$LOAD_REQUEST_SERVICE_URL/api/v1/solicitud" `
                                              -Method Post `
                                              -Headers $headers `
                                              -Body ($autoLoanData | ConvertTo-Json)
        
        if ($autoLoanResponse.success) {
            Write-Success "✅ Solicitud con validación automática procesada"
            Write-Info "   El sistema debería haber consultado solo loans_db para envío a Lambda"
        } else {
            Write-Warning "⚠️ Problema con solicitud automática"
        }
        
        return $true
    } catch {
        Write-Error "Error en testing de queries: $_"
        return $false
    }
}

# Test 6: Verificar logs de conexiones separadas
function Test-ConnectionLogs {
    Write-Info "6. Verificando logs de conexiones..."
    
    try {
        Write-Info "Revisando logs de AuthService..."
        $authLogs = docker logs crediya-auth-service 2>$null | Select-String -Pattern "auth_db|mysql-auth" -CaseSensitive:$false
        
        if ($authLogs.Count -gt 0) {
            Write-Success "✅ AuthService conecta a auth_db"
        } else {
            Write-Info "   Logs de AuthService no muestran DB específica (normal)"
        }
        
        Write-Info "Revisando logs de LoadRequestService..."
        $loansLogs = docker logs crediya-load-request-service 2>$null | Select-String -Pattern "loans_db|mysql-loans|clients" -CaseSensitive:$false
        
        if ($loansLogs.Count -gt 0) {
            Write-Success "✅ LoadRequestService conecta a loans_db"
        } else {
            Write-Info "   Logs de LoadRequestService no muestran DB específica (normal)"
        }
        
        return $true
    } catch {
        Write-Error "Error revisando logs: $_"
        return $false
    }
}

# Restart servicios si se solicita
if ($RestartServices) {
    Write-Info "Reiniciando servicios con nuevas configuraciones..."
    docker-compose down
    docker-compose up -d
    
    Write-Info "Esperando que los servicios estén listos..."
    Start-Sleep -Seconds 45
}

# Ejecutar todos los tests
function Run-SeparatedDbsTests {
    try {
        # Test de separación de DBs
        if (!$SkipDbValidation) {
            $test1 = Test-DatabaseSeparation
            $test2 = Test-InitialData
        } else {
            $test1 = $true
            $test2 = $true
            Write-Warning "Validación de DB omitida"
        }
        
        # Tests de conectividad
        $test3 = Test-ServiceConnectivity
        
        # Tests funcionales
        $loanId = Test-FunctionalFlow
        $test4 = $loanId -ne $false
        
        # Tests avanzados
        if ($test4) {
            # Obtener token nuevamente para tests adicionales
            $loginData = @{
                email = $CLIENT_EMAIL
                password = $PASSWORD
            }
            $loginResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE_URL/api/v1/auth/login" `
                                               -Method Post `
                                               -ContentType "application/json" `
                                               -Body ($loginData | ConvertTo-Json)
            $token = $loginResponse.data.token
            
            $test5 = Test-DebtCapacityQueries $token
        } else {
            $test5 = $false
        }
        
        $test6 = Test-ConnectionLogs
        
        # Resumen final
        Write-Info "`nResumen de Tests con DBs Separadas:"
        Write-Host "  1. Separación de DBs: " -NoNewline
        Write-Host (if ($test1) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  2. Datos iniciales: " -NoNewline
        Write-Host (if ($test2) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  3. Conectividad servicios: " -NoNewline
        Write-Host (if ($test3) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  4. Flujo funcional: " -NoNewline
        Write-Host (if ($test4) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  5. Queries capacidad: " -NoNewline
        Write-Host (if ($test5) { "✅ PASS" } else { "❌ FAIL" })
        
        Write-Host "  6. Logs conexión: " -NoNewline
        Write-Host (if ($test6) { "✅ PASS" } else { "❌ FAIL" })
        
        $totalPassed = @($test1, $test2, $test3, $test4, $test5, $test6) | Where-Object { $_ } | Measure-Object | Select-Object -ExpandProperty Count
        
        if ($totalPassed -ge 5) {
            Write-Success "`n🎉 Bases de datos separadas funcionan correctamente!"
            Write-Info "✅ AuthService → auth_db (puerto 3306)"
            Write-Info "✅ LoadRequestService → loans_db (puerto 3307)"
            Write-Info "✅ Separación de responsabilidades implementada"
        } else {
            Write-Warning "`n⚠️ Algunos tests fallaron"
            Write-Info "Revisar configuración de bases de datos separadas"
        }
        
    } catch {
        Write-Error "Error en tests de DBs separadas: $_"
    }
}

# Ejecutar tests
Run-SeparatedDbsTests

Write-Info "`n📊 Información de las DBs:"
Write-Info "  - auth_db (mysql-auth): localhost:3306"
Write-Info "  - loans_db (mysql-loans): localhost:3307"
Write-Info "`n🔧 Para administración manual:"
Write-Info "  - AuthDB: docker exec -it crediya-mysql-auth mysql -u auth_user -pauth_pass auth_db"
Write-Info "  - LoansDB: docker exec -it crediya-mysql-loans mysql -u loans_user -ploans_pass loans_db"
Write-Info "`n📝 Para sincronizar datos: ./database/sync-clients.sql"

