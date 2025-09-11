-- Script de configuración de base de datos para CrediYa
-- Base de datos: crediyadb

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS crediyadb;
USE crediyadb;

-- Tabla de roles del sistema
CREATE TABLE IF NOT EXISTS role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL COMMENT 'Nombre del rol: ADMINISTRADOR, ASESOR, CLIENTE',
    description TEXT COMMENT 'Descripción detallada del rol',
    permissions JSON COMMENT 'Permisos específicos del rol en formato JSON',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE COMMENT 'Indica si el rol está activo',
    
    INDEX idx_role_name (name),
    INDEX idx_role_active (active)
) COMMENT 'Catálogo de roles del sistema CrediYa';

-- Insertar roles base del sistema
INSERT INTO role (name, description, permissions) VALUES
('ADMINISTRADOR', 'Administrador del sistema con acceso completo', JSON_OBJECT(
    'users', JSON_ARRAY('create', 'read', 'update', 'delete'),
    'loans', JSON_ARRAY('create', 'read', 'update', 'delete', 'approve', 'reject'),
    'reports', JSON_ARRAY('read', 'generate'),
    'system', JSON_ARRAY('configure', 'monitor')
)),
('ASESOR', 'Asesor de crédito que gestiona solicitudes de préstamo', JSON_OBJECT(
    'users', JSON_ARRAY('read'),
    'loans', JSON_ARRAY('read', 'update', 'approve', 'reject'),
    'reports', JSON_ARRAY('read')
)),
('CLIENTE', 'Cliente del sistema que puede solicitar préstamos', JSON_OBJECT(
    'loans', JSON_ARRAY('create', 'read_own'),
    'profile', JSON_ARRAY('read', 'update_own')
))
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- Tabla de usuarios con campos de autenticación
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY DEFAULT (UUID()),
    document VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255) UNIQUE NOT NULL,
    base_salary DECIMAL(15,2),
    password VARCHAR(255) NOT NULL COMMENT 'Contraseña encriptada con BCrypt',
    role_id INT NOT NULL DEFAULT 3 COMMENT 'Clave foránea a la tabla role',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_email (email),
    INDEX idx_document (document),
    INDEX idx_role_id (role_id),
    
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE RESTRICT ON UPDATE CASCADE
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

-- Insertar usuarios de prueba con autenticación
-- IMPORTANTE: Todas las contraseñas son "password123" encriptadas con BCrypt
-- role_id: 1=ADMINISTRADOR, 2=ASESOR, 3=CLIENTE
INSERT INTO users (id, document, first_name, last_name, birth_date, address, phone, email, base_salary, password, role_id) VALUES

-- Administrador del sistema (role_id = 1)
(UUID(), '12345678', 'Admin', 'Sistema', '1990-01-01', 'Carrera 7 #123-45, Bogotá', '3001234567', 'admin@crediya.com', 5000000.00, 
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1),

-- Asesores de crédito (role_id = 2)
(UUID(), '87654321', 'Juan Carlos', 'Pérez Rodríguez', '1985-06-15', 'Calle 50 #30-20, Medellín', '3009876543', 'asesor@crediya.com', 3000000.00,
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 2),

(UUID(), '55667788', 'Ana Patricia', 'Martínez Silva', '1988-11-10', 'Carrera 15 #45-60, Bucaramanga', '3007777777', 'asesor2@crediya.com', 3200000.00,
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 2),

-- Clientes del sistema (role_id = 3)
(UUID(), '11223344', 'María Elena', 'García López', '1992-03-20', 'Avenida 6 #15-30, Cali', '3005555555', 'cliente@crediya.com', 2500000.00,
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 3),

(UUID(), '99887766', 'Carlos Eduardo', 'Ramírez Torres', '1990-07-25', 'Calle 20 #10-40, Barranquilla', '3008888888', 'cliente2@crediya.com', 2800000.00,
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 3),

-- Cliente adicional para pruebas (role_id = 3)
(UUID(), '44556677', 'Laura Sofía', 'Vargas Moreno', '1995-09-12', 'Transversal 8 #25-30, Pereira', '3006666666', 'cliente3@crediya.com', 2200000.00,
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 3)

ON DUPLICATE KEY UPDATE 
    updated_at = CURRENT_TIMESTAMP,
    password = VALUES(password),
    role_id = VALUES(role_id);

-- Tabla de logs de autenticación (opcional para auditoría)
CREATE TABLE IF NOT EXISTS authentication_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36),
    email VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    login_successful BOOLEAN,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_auth_logs_user_id (user_id),
    INDEX idx_auth_logs_email (email),
    INDEX idx_auth_logs_created_at (created_at),
    INDEX idx_auth_logs_successful (login_successful)
) COMMENT 'Logs de intentos de autenticación para auditoría';

-- Verificar las tablas creadas
SHOW TABLES;

-- Verificar estructura de las tablas
DESCRIBE role;
DESCRIBE users;
DESCRIBE loan_requests;
DESCRIBE authentication_logs;

-- Consultas de verificación útiles

-- 1. Verificar roles creados
SELECT id, name, description, active, created_at 
FROM role 
ORDER BY id;

-- 2. Contar usuarios por rol
SELECT 
    r.name as rol_nombre,
    r.description as descripcion,
    COUNT(u.id) as cantidad_usuarios 
FROM role r
LEFT JOIN users u ON r.id = u.role_id
GROUP BY r.id, r.name, r.description
ORDER BY r.id;

-- 3. Listar todos los usuarios con roles (sin mostrar contraseñas)
SELECT 
    u.id,
    u.document,
    CONCAT(u.first_name, ' ', u.last_name) as nombre_completo,
    u.email,
    r.name as rol_nombre,
    u.base_salary,
    CASE 
        WHEN u.password IS NOT NULL THEN 'Configurada' 
        ELSE 'Sin configurar' 
    END as estado_password,
    u.created_at
FROM users u
JOIN role r ON u.role_id = r.id
ORDER BY r.name, u.email;

-- 4. Verificar configuración de autenticación
SELECT 
    COUNT(*) as total_usuarios,
    SUM(CASE WHEN u.password IS NOT NULL THEN 1 ELSE 0 END) as con_password,
    SUM(CASE WHEN u.role_id IS NOT NULL THEN 1 ELSE 0 END) as con_role,
    COUNT(DISTINCT u.role_id) as roles_diferentes
FROM users u;

-- 5. Verificar emails únicos
SELECT 
    CASE 
        WHEN COUNT(*) = COUNT(DISTINCT email) THEN 'OK - Todos los emails son únicos'
        ELSE 'ERROR - Hay emails duplicados'
    END as verificacion_emails
FROM users;

-- 6. Mostrar permisos por rol
SELECT 
    r.name as rol,
    r.description,
    JSON_PRETTY(r.permissions) as permisos
FROM role r
ORDER BY r.id;

-- =====================================
-- INFORMACIÓN DE CREDENCIALES DE PRUEBA
-- =====================================
-- 
-- USUARIOS PARA TESTING:
-- 
-- ADMINISTRADOR:
--   Email: admin@crediya.com
--   Password: password123
--   Rol: ADMINISTRADOR
-- 
-- ASESORES:
--   Email: asesor@crediya.com  
--   Password: password123
--   Rol: ASESOR
--   
--   Email: asesor2@crediya.com
--   Password: password123
--   Rol: ASESOR
-- 
-- CLIENTES:
--   Email: cliente@crediya.com
--   Password: password123
--   Rol: CLIENTE
--   
--   Email: cliente2@crediya.com
--   Password: password123
--   Rol: CLIENTE
--   
--   Email: cliente3@crediya.com
--   Password: password123
--   Rol: CLIENTE
-- 
-- IMPORTANTE: 
-- - Todas las contraseñas están hasheadas con BCrypt
-- - Cambiar estas contraseñas en producción
-- - El hash corresponde a "password123"
-- =====================================
