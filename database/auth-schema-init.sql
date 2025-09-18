-- ========================================
-- ESQUEMA DE BASE DE DATOS PARA AUTHSERVICE
-- ========================================

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;

-- ========================================
-- CONFIGURACIÓN DE TIMEZONE Y ENCODING
-- ========================================
SET time_zone = '+00:00';
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- ========================================
-- TABLA DE USUARIOS (AUTENTICACIÓN)
-- ========================================

-- Tabla principal de usuarios
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document VARCHAR(20) NOT NULL UNIQUE,
    birth_date DATE,
    address VARCHAR(255),
    phone VARCHAR(20),
    base_salary DECIMAL(15,2) DEFAULT 0.00,
    role ENUM('ADMIN', 'ADVISOR', 'CLIENT') NOT NULL DEFAULT 'CLIENT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_users_email (email),
    INDEX idx_users_document (document),
    INDEX idx_users_active (is_active),
    INDEX idx_users_email_verified (email_verified),
    INDEX idx_users_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE ROLES
-- ========================================

-- Tabla de roles
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_roles_name (name),
    INDEX idx_roles_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar roles iniciales
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrador del sistema con acceso completo'),
('LOAN_OFFICER', 'Asesor de préstamos con acceso a gestión de solicitudes'),
('CLIENT', 'Cliente del sistema con acceso a solicitudes propias'),
('SUPERVISOR', 'Supervisor con acceso a reportes y aprobaciones'),
('AUDITOR', 'Auditor con acceso de solo lectura a transacciones');

-- ========================================
-- TABLA DE ROLES DE USUARIO
-- ========================================

-- Tabla de asignación de roles a usuarios
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id),
    INDEX idx_user_roles_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE SESIONES/TOKENS
-- ========================================

-- Tabla para gestión de tokens JWT/sesiones
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    token_type ENUM('ACCESS', 'REFRESH', 'RESET_PASSWORD', 'EMAIL_VERIFICATION') NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_sessions_user (user_id),
    INDEX idx_sessions_token (token_id),
    INDEX idx_sessions_type (token_type),
    INDEX idx_sessions_expires (expires_at),
    INDEX idx_sessions_revoked (is_revoked),
    INDEX idx_sessions_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE AUDITORÍA DE AUTENTICACIÓN
-- ========================================

-- Tabla para auditoría de eventos de autenticación
CREATE TABLE IF NOT EXISTS auth_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255),
    event_type ENUM('LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT', 'PASSWORD_CHANGE', 'PASSWORD_RESET', 'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED', 'EMAIL_VERIFIED') NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_email (email),
    INDEX idx_audit_event (event_type),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_ip (ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- DATOS INICIALES DE USUARIOS
-- ========================================

-- Usuario administrador por defecto
INSERT INTO users (email, password, first_name, last_name, document, birth_date, address, phone, base_salary, role, email_verified) VALUES
('admin@crediya.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Administrador', 'Sistema', '00000000', '1980-01-01', 'Calle Admin 123', '+573001234567', 15000000.00, 'ADMIN', TRUE),
('asesor@crediya.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Asesor', 'Préstamos', '11111111', '1985-05-15', 'Avenida Asesor 456', '+573001234568', 3500000.00, 'ADVISOR', TRUE);

-- Clientes de prueba
INSERT INTO users (email, password, first_name, last_name, document, birth_date, address, phone, base_salary, role, email_verified) VALUES
('ana.perez@email.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Ana', 'Pérez Silva', '45678901', '1990-03-15', 'Calle 45 #12-34', '+573012345678', 2800000.00, 'CLIENT', TRUE),
('juan.rodriguez@email.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Juan', 'Rodríguez', '56789012', '1988-07-22', 'Carrera 67 #89-01', '+573012345679', 4200000.00, 'CLIENT', TRUE),
('laura.gonzalez@email.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Laura', 'González', '67890123', '1992-11-08', 'Avenida 23 #45-67', '+573012345680', 5500000.00, 'CLIENT', TRUE),
('carlos.martinez@email.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'Carlos', 'Martínez', '78901234', '1987-02-14', 'Calle 78 #90-12', '+573012345681', 3200000.00, 'CLIENT', TRUE),
('maria.lopez@email.com', '$2a$10$8P8kZpXQGzc1tKzW2Qn6WO7Lv9fB3nC5dR8jK6mE1sA4qH7uJ9pL0', 'María', 'López', '89012345', '1985-12-25', 'Carrera 34 #56-78', '+573012345682', 6800000.00, 'CLIENT', TRUE);

-- ========================================
-- ROLES ASIGNADOS DIRECTAMENTE EN TABLA USERS
-- ========================================
-- Los roles están ahora en la columna 'role' de users:
-- admin@crediya.com -> ADMIN
-- asesor@crediya.com -> ADVISOR  
-- Todos los clientes -> CLIENT