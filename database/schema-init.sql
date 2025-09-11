-- Script de inicialización del esquema de base de datos para CrediYa
-- Base de datos única con todas las tablas necesarias

-- ========================================
-- CREACIÓN DE LA BASE DE DATOS
-- ========================================

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS crediya_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci
   -- COMMENT 'Base de datos para el sistema CrediYa - Gestión de préstamos y autenticación'
   ;

-- Usar la base de datos creada
USE crediya_db;

-- ========================================
-- TABLAS PARA AUTENTICACIÓN (AuthService)
-- ========================================

-- Tabla de usuarios (compartida entre ambos microservicios)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    document VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMINISTRADOR', 'ASESOR', 'CLIENTE') NOT NULL DEFAULT 'CLIENTE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    base_salary DECIMAL(15,2) DEFAULT 0.00,
    
    INDEX idx_users_email (email),
    INDEX idx_users_document (document),
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de sesiones/tokens (para AuthService)
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sessions_token (token_hash),
    INDEX idx_sessions_user (user_id),
    INDEX idx_sessions_expires (expires_at),
    INDEX idx_sessions_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLAS PARA SOLICITUDES (LoadRequestService)
-- ========================================

-- Tabla de solicitudes de préstamo
CREATE TABLE IF NOT EXISTS loan_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_document VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    term_in_months INT NOT NULL,
    loan_type ENUM('PERSONAL', 'VEHICLE', 'HOME', 'BUSINESS') NOT NULL,
    status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW') NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Campos adicionales para evaluación
    interest_rate DECIMAL(5,2),
    monthly_payment DECIMAL(15,2),
    approved_amount DECIMAL(15,2),
    approved_at TIMESTAMP NULL,
    approved_by VARCHAR(50),
    rejection_reason TEXT,
    
    FOREIGN KEY (client_document) REFERENCES users(document) ON DELETE RESTRICT,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_loan_requests_client (client_document),
    INDEX idx_loan_requests_status (status),
    INDEX idx_loan_requests_type (loan_type),
    INDEX idx_loan_requests_created (created_at),
    INDEX idx_loan_requests_amount (amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de historial de cambios en solicitudes
CREATE TABLE IF NOT EXISTS loan_request_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_request_id BIGINT NOT NULL,
    previous_status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW'),
    new_status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW') NOT NULL,
    changed_by VARCHAR(50),
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loan_request_id) REFERENCES loan_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_history_loan_request (loan_request_id),
    INDEX idx_history_changed_at (changed_at),
    INDEX idx_history_changed_by (changed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- VISTAS ÚTILES
-- ========================================

-- Vista para solicitudes con información del cliente
CREATE OR REPLACE VIEW loan_requests_with_client AS
SELECT 
    lr.id,
    lr.client_document,
    u.first_name,
    u.last_name,
    u.email,
    u.base_salary,
    lr.amount,
    lr.term_in_months,
    lr.loan_type,
    lr.status,
    lr.created_at,
    lr.updated_at,
    lr.notes,
    lr.interest_rate,
    lr.monthly_payment,
    lr.approved_amount,
    lr.approved_at,
    lr.approved_by,
    lr.rejection_reason
FROM loan_requests lr
INNER JOIN users u ON lr.client_document = u.document;

-- Vista para estadísticas de solicitudes por estado
CREATE OR REPLACE VIEW loan_requests_stats AS
SELECT 
    status,
    COUNT(*) as total_requests,
    AVG(amount) as avg_amount,
    SUM(amount) as total_amount,
    MIN(amount) as min_amount,
    MAX(amount) as max_amount
FROM loan_requests
GROUP BY status;

-- ========================================
-- TRIGGERS PARA AUDITORÍA
-- ========================================

-- Trigger para registrar cambios de estado en solicitudes
DELIMITER //
CREATE TRIGGER loan_request_status_change 
    AFTER UPDATE ON loan_requests
    FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO loan_request_history (
            loan_request_id, 
            previous_status, 
            new_status, 
            changed_by,
            change_reason
        ) VALUES (
            NEW.id, 
            OLD.status, 
            NEW.status, 
            NEW.approved_by,
            CASE 
                WHEN NEW.status = 'APPROVED' THEN 'Solicitud aprobada'
                WHEN NEW.status = 'REJECTED' THEN NEW.rejection_reason
                WHEN NEW.status = 'MANUAL_REVIEW' THEN 'Requiere revisión manual'
                ELSE 'Cambio de estado'
            END
        );
    END IF;
END//
DELIMITER ;

-- ========================================
-- ÍNDICES ADICIONALES PARA PERFORMANCE
-- ========================================

-- Índices compuestos para consultas frecuentes
CREATE INDEX idx_loan_requests_client_status ON loan_requests(client_document, status);
CREATE INDEX idx_loan_requests_status_created ON loan_requests(status, created_at);
CREATE INDEX idx_loan_requests_type_amount ON loan_requests(loan_type, amount);

-- Índices para la vista de solicitudes para revisión
CREATE INDEX idx_loan_requests_review_states ON loan_requests(status, created_at) 
WHERE status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW');

-- ========================================
-- CONFIGURACIÓN DE LA BASE DE DATOS
-- ========================================

-- Configurar timezone
SET time_zone = '-05:00';

-- Configurar charset por defecto
ALTER DATABASE crediya_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Mostrar resumen de tablas creadas
SELECT 
    'Esquema de base de datos CrediYa inicializado correctamente' as 'STATUS',
    COUNT(*) as 'TABLAS_CREADAS'
FROM information_schema.tables 
WHERE table_schema = 'crediya_db' 
AND table_type = 'BASE TABLE';

-- Mostrar estructura de tablas
SELECT 
    table_name as 'TABLA',
    table_rows as 'FILAS',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) as 'TAMAÑO_MB'
FROM information_schema.tables 
WHERE table_schema = 'crediya_db' 
AND table_type = 'BASE TABLE'
ORDER BY table_name;
