#!/usr/bin/env pwsh

Write-Host "=== Configuración Actual de SQS ===" -ForegroundColor Green

Write-Host "`nVariable de entorno del sistema:" -ForegroundColor Yellow
if ($env:AWS_SQS_DEBT_CAPACITY_QUEUE_URL) {
    Write-Host "AWS_SQS_DEBT_CAPACITY_QUEUE_URL = $env:AWS_SQS_DEBT_CAPACITY_QUEUE_URL" -ForegroundColor Cyan
} else {
    Write-Host "AWS_SQS_DEBT_CAPACITY_QUEUE_URL = No configurada (usará default)" -ForegroundColor Red
}

Write-Host "`nConfiguración en docker-compose.yml:" -ForegroundColor Yellow
$dockerConfig = Select-String -Path "docker-compose.yml" -Pattern "AWS_SQS_DEBT_CAPACITY_QUEUE_URL"
if ($dockerConfig) {
    Write-Host $dockerConfig.Line.Trim() -ForegroundColor Cyan
}

Write-Host "`nConfiguración en application.yaml:" -ForegroundColor Yellow
$appConfig = Select-String -Path "LoadRequestService/applications/app-service/src/main/resources/application.yaml" -Pattern "debt-capacity-queue-url"
if ($appConfig) {
    Write-Host $appConfig.Line.Trim() -ForegroundColor Cyan
}

Write-Host "`n=== Para cambiar la configuración ===" -ForegroundColor Green
Write-Host "1. Establecer variable de entorno:" -ForegroundColor Yellow
Write-Host '   $env:AWS_SQS_DEBT_CAPACITY_QUEUE_URL = "tu-nueva-url"' -ForegroundColor White
Write-Host "2. O editar docker-compose.yml directamente" -ForegroundColor Yellow
Write-Host "3. Reiniciar los servicios: docker-compose restart" -ForegroundColor Yellow
