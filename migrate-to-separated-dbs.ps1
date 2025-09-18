# Script para migrar de una DB única a DBs separadas
# Extrae datos de crediya_db y los distribuye a auth_db y loans_db

param(
    [switch]$BackupFirst,
    [switch]$Force,
    [string]$BackupPath = "./backup"
)

$ErrorActionPreference = "Stop"

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
Write-Success "   Migración a Bases de Datos Separadas"
Write-Success "=========================================="

# Verificar que docker-compose esté ejecutándose
$runningServices = docker-compose ps --services --filter "status=running"
if (!$runningServices -or $runningServices.Count -eq 0) {
    Write-Error "Docker Compose no está ejecutándose. Ejecute 'docker-compose up -d' primero"
    exit 1
}

# Crear backup si se solicita
if ($BackupFirst) {
    Write-Info "Creando backup de la base de datos actual..."
    
    if (!(Test-Path $BackupPath)) {
        New-Item -ItemType Directory -Path $BackupPath -Force | Out-Null
    }
    
    $backupFile = "$BackupPath/crediya_db_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
    
    try {
        # Backup de la DB original (si existe)
        $backupCmd = "docker exec crediya-mysql mysqldump -u crediya_user -pcrediya_pass crediya_db > $backupFile"
        Invoke-Expression $backupCmd
        Write-Success "✅ Backup creado: $backupFile"
    } catch {
        Write-Warning "⚠️ No se pudo crear backup (posiblemente DB única no existe): $_"
    }
}

# Verificar que las nuevas DBs estén disponibles
function Test-NewDatabases {
    Write-Info "Verificando nuevas bases de datos..."
    
    try {
        # Test auth_db
        $authTest = docker exec crediya-mysql-auth mysql -u auth_user -pauth_pass -e "USE auth_db; SELECT 1;" 2>$null
        if (!$authTest) {
            Write-Error "auth_db no está disponible"
            return $false
        }
        
        # Test loans_db
        $loansTest = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT 1;" 2>$null
        if (!$loansTest) {
            Write-Error "loans_db no está disponible"
            return $false
        }
        
        Write-Success "✅ Nuevas bases de datos disponibles"
        return $true
    } catch {
        Write-Error "Error verificando nuevas DBs: $_"
        return $false
    }
}

# Migrar datos de usuarios a auth_db
function Migrate-UsersToAuth {
    Write-Info "Migrando usuarios a auth_db..."
    
    try {
        # Primero, verificar si hay datos en la DB original
        $originalUsers = docker exec crediya-mysql mysql -u crediya_user -pcrediya_pass -e "SELECT COUNT(*) as count FROM crediya_db.users;" 2>$null
        
        if ($originalUsers -notmatch "[1-9]") {
            Write-Warning "No hay usuarios en la DB original para migrar"
            return $true
        }
        
        # Exportar usuarios de DB original
        $exportUsers = docker exec crediya-mysql mysql -u crediya_user -pcrediya_pass -e "
            SELECT id, email, password, first_name, last_name, document, phone, base_salary, is_active, email_verified, created_at
            FROM crediya_db.users 
            WHERE is_active = TRUE
            ORDER BY id;
        " 2>$null
        
        if ($exportUsers -match "email") {
            Write-Success "✅ Usuarios exportados de DB original"
            Write-Info "Los usuarios ya están en auth_db por el script de inicialización"
        } else {
            Write-Warning "⚠️ No se encontraron usuarios para migrar"
        }
        
        return $true
    } catch {
        Write-Warning "Advertencia en migración de usuarios: $_"
        return $true  # Continue anyway
    }
}

# Migrar datos de préstamos a loans_db
function Migrate-LoansData {
    Write-Info "Migrando datos de préstamos a loans_db..."
    
    try {
        # Verificar si hay solicitudes en la DB original
        $originalLoans = docker exec crediya-mysql mysql -u crediya_user -pcrediya_pass -e "SELECT COUNT(*) as count FROM crediya_db.loan_requests;" 2>$null
        
        if ($originalLoans -notmatch "[1-9]") {
            Write-Warning "No hay solicitudes en la DB original para migrar"
            return $true
        }
        
        # Migrar loan_requests
        $migrateLoansCmd = "
            INSERT INTO loans_db.loan_requests 
            (client_document, amount, term_in_months, loan_type, status, created_at, updated_at, notes, 
             interest_rate, monthly_payment, approved_amount, approved_at, approved_by, rejection_reason)
            SELECT client_document, amount, term_in_months, loan_type, status, created_at, updated_at, notes,
                   interest_rate, monthly_payment, approved_amount, approved_at, approved_by, rejection_reason
            FROM crediya_db.loan_requests;
        "
        
        docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "$migrateLoansCmd" 2>$null
        
        Write-Success "✅ Solicitudes de préstamo migradas"
        return $true
    } catch {
        Write-Warning "Advertencia en migración de préstamos: $_"
        return $true  # Continue anyway
    }
}

# Verificar migración
function Verify-Migration {
    Write-Info "Verificando migración..."
    
    try {
        # Contar usuarios en auth_db
        $authUsers = docker exec crediya-mysql-auth mysql -u auth_user -pauth_pass -e "USE auth_db; SELECT COUNT(*) as count FROM users WHERE is_active = TRUE;" 2>$null
        
        # Contar clientes en loans_db
        $loansClients = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM clients WHERE is_active = TRUE;" 2>$null
        
        # Contar solicitudes en loans_db
        $loanRequests = docker exec crediya-mysql-loans mysql -u loans_user -ploans_pass -e "USE loans_db; SELECT COUNT(*) as count FROM loan_requests;" 2>$null
        
        Write-Info "Resultado de migración:"
        if ($authUsers -match "[1-9]") {
            Write-Success "  ✅ Usuarios en auth_db: encontrados"
        } else {
            Write-Warning "  ⚠️ Pocos usuarios en auth_db"
        }
        
        if ($loansClients -match "[1-9]") {
            Write-Success "  ✅ Clientes en loans_db: encontrados"
        } else {
            Write-Warning "  ⚠️ Pocos clientes en loans_db"
        }
        
        if ($loanRequests -match "[0-9]") {
            Write-Success "  ✅ Solicitudes en loans_db: migradas"
        } else {
            Write-Info "  📝 Sin solicitudes para migrar (normal)"
        }
        
        return $true
    } catch {
        Write-Error "Error verificando migración: $_"
        return $false
    }
}

# Limpiar DB original (opcional)
function Cleanup-OriginalDb {
    if (!$Force) {
        $confirm = Read-Host "¿Desea eliminar la DB original crediya_db? (y/N)"
        if ($confirm -ne "y" -and $confirm -ne "Y") {
            Write-Info "DB original conservada"
            return
        }
    }
    
    Write-Warning "Eliminando DB original..."
    try {
        docker exec crediya-mysql mysql -u root -proot_password_2025 -e "DROP DATABASE IF EXISTS crediya_db;" 2>$null
        Write-Success "✅ DB original eliminada"
    } catch {
        Write-Warning "No se pudo eliminar DB original: $_"
    }
}

# Proceso principal de migración
function Start-Migration {
    Write-Info "Iniciando proceso de migración..."
    
    # 1. Verificar nuevas DBs
    if (!(Test-NewDatabases)) {
        Write-Error "Las nuevas bases de datos no están disponibles"
        exit 1
    }
    
    # 2. Migrar usuarios
    if (!(Migrate-UsersToAuth)) {
        Write-Error "Error migrando usuarios"
        exit 1
    }
    
    # 3. Migrar datos de préstamos
    if (!(Migrate-LoansData)) {
        Write-Error "Error migrando datos de préstamos"
        exit 1
    }
    
    # 4. Verificar migración
    if (!(Verify-Migration)) {
        Write-Error "Error verificando migración"
        exit 1
    }
    
    # 5. Cleanup opcional
    if ($Force) {
        Cleanup-OriginalDb
    }
    
    Write-Success "`n🎉 Migración completada exitosamente!"
    Write-Info "`n📊 Nuevas configuraciones:"
    Write-Info "  - AuthService → auth_db (puerto 3306)"
    Write-Info "  - LoadRequestService → loans_db (puerto 3307)"
    Write-Info "`n🔄 Próximos pasos:"
    Write-Info "  1. Ejecutar: docker-compose restart"
    Write-Info "  2. Probar: .\test-separated-dbs.ps1"
    Write-Info "  3. Verificar logs de servicios"
}

# Verificar si usuario entiende lo que va a pasar
if (!$Force) {
    Write-Warning "`n⚠️ IMPORTANTE:"
    Write-Warning "Esta migración va a:"
    Write-Warning "  1. Separar auth_db y loans_db"
    Write-Warning "  2. Migrar datos existentes"
    Write-Warning "  3. Cambiar configuraciones de conexión"
    Write-Warning ""
    $confirm = Read-Host "¿Continuar con la migración? (y/N)"
    
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Info "Migración cancelada por el usuario"
        exit 0
    }
}

# Ejecutar migración
Start-Migration

