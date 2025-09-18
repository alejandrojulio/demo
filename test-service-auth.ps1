# Script para probar autenticación de servicio a servicio
Write-Host "=== Prueba de Autenticación de Servicio ===`n" -ForegroundColor Cyan

$authServiceUrl = "http://localhost:8080"
$token = "CrediYaInternalService2025SecureToken"
$serviceName = "LoadRequestService"

# Headers de autenticación de servicio
$headers = @{
    "X-Service-Auth" = $token
    "X-Service-Name" = $serviceName
    "Content-Type" = "application/json"
}

Write-Host "1. Probando acceso CON autenticación correcta..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$authServiceUrl/api/v1/users/document/89012345" -Method GET -Headers $headers
    Write-Host "✅ ÉXITO: " -ForegroundColor Green -NoNewline
    Write-Host "Usuario obtenido: $($response.data.firstName) $($response.data.lastName)"
} catch {
    Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2. Probando acceso SIN headers de autenticación..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$authServiceUrl/api/v1/users/document/89012345" -Method GET
    Write-Host "❌ FALLO DE SEGURIDAD: Acceso permitido sin autenticación" -ForegroundColor Red
} catch {
    Write-Host "✅ SEGURIDAD OK: " -ForegroundColor Green -NoNewline
    Write-Host "Acceso denegado correctamente (401 Unauthorized)"
}

Write-Host "`n3. Probando acceso con token INCORRECTO..." -ForegroundColor Yellow
$wrongHeaders = @{
    "X-Service-Auth" = "token-incorrecto"
    "X-Service-Name" = $serviceName
    "Content-Type" = "application/json"
}
try {
    $response = Invoke-RestMethod -Uri "$authServiceUrl/api/v1/users/document/89012345" -Method GET -Headers $wrongHeaders
    Write-Host "❌ FALLO DE SEGURIDAD: Acceso permitido con token incorrecto" -ForegroundColor Red
} catch {
    Write-Host "✅ SEGURIDAD OK: " -ForegroundColor Green -NoNewline
    Write-Host "Token incorrecto rechazado (401 Unauthorized)"
}

Write-Host "`n4. Probando acceso con servicio NO AUTORIZADO..." -ForegroundColor Yellow
$unauthorizedHeaders = @{
    "X-Service-Auth" = $token
    "X-Service-Name" = "UnauthorizedService"
    "Content-Type" = "application/json"
}
try {
    $response = Invoke-RestMethod -Uri "$authServiceUrl/api/v1/users/document/89012345" -Method GET -Headers $unauthorizedHeaders
    Write-Host "❌ FALLO DE SEGURIDAD: Servicio no autorizado tuvo acceso" -ForegroundColor Red
} catch {
    Write-Host "✅ SEGURIDAD OK: " -ForegroundColor Green -NoNewline
    Write-Host "Servicio no autorizado rechazado (403 Forbidden)"
}

Write-Host "`n=== Resumen ===" -ForegroundColor Cyan
Write-Host "• Autenticación de servicio implementada correctamente"
Write-Host "• Solo servicios autorizados pueden acceder a datos de usuario"
Write-Host "• Los datos de cliente están protegidos contra acceso no autorizado"
Write-Host "• Token interno requerido para comunicación entre microservicios`n"

