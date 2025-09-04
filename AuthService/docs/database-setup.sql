-- Script de configuración de base de datos para CrediYa
-- Base de datos: crediyadb

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS crediyadb;
USE crediyadb;

-- Tabla de usuarios (si no existe)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255) UNIQUE NOT NULL,
    base_salary DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_document (document),
    INDEX idx_id (id)
);

-- Tabla de solicitudes de préstamo
CREATE TABLE IF NOT EXISTS loan_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_document VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    term_in_months INT NOT NULL,
    loan_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Índices para mejor performance
    INDEX idx_client_document (client_document),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_loan_type (loan_type),
    
    -- Clave foránea a la tabla users por documento
    FOREIGN KEY (client_document) REFERENCES users(document) ON DELETE CASCADE
);

-- Insertar algunos usuarios de prueba
INSERT INTO users (document, first_name, last_name, birth_date, address, phone, email, base_salary) VALUES
('12345678', 'Juan', 'Pérez', '1990-05-15', 'Calle 123 #45-67, Bogotá', '+57 300 123 4567', 'juan.perez@email.com', 2500000),
('87654321', 'María', 'García', '1985-08-20', 'Avenida 78 #12-34, Medellín', '+57 310 987 6543', 'maria.garcia@email.com', 3200000),
('11223344', 'Carlos', 'Rodríguez', '1992-12-10', 'Carrera 15 #23-45, Cali', '+57 315 456 7890', 'carlos.rodriguez@email.com', 2800000)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Verificar las tablas creadas
SHOW TABLES;

-- Verificar estructura de la tabla loan_requests
DESCRIBE loan_requests;

-- Verificar estructura de la tabla users
DESCRIBE users;
