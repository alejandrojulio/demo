-- Script de configuración SIMPLIFICADO para CrediYa (sin JSON)
-- Usar SOLO si tienes problemas con JSON en MySQL

USE crediyadb;

-- Tabla de roles SIMPLIFICADA (sin JSON)
CREATE TABLE IF NOT EXISTS role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL COMMENT 'Nombre del rol: ADMINISTRADOR, ASESOR, CLIENTE',
    description TEXT COMMENT 'Descripción detallada del rol',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE COMMENT 'Indica si el rol está activo',
    
    INDEX idx_role_name (name),
    INDEX idx_role_active (active)
) COMMENT 'Catálogo de roles del sistema CrediYa';

-- Insertar roles base SIN permisos JSON
INSERT INTO role (name, description) VALUES
('ADMINISTRADOR', 'Administrador del sistema con acceso completo'),
('ASESOR', 'Asesor de crédito que gestiona solicitudes de préstamo'),
('CLIENTE', 'Cliente del sistema que puede solicitar préstamos')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Verificar roles creados
SELECT * FROM role ORDER BY id;

-- El resto de la estructura permanece igual...
-- (users, loan_requests, etc.)

