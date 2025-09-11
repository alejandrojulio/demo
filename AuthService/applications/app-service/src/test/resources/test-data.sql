-- =========================================
-- CREACIÓN DE TABLAS PARA TESTS
-- =========================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    address VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMINISTRADOR', 'ASESOR', 'CLIENTE') NOT NULL DEFAULT 'CLIENTE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    document VARCHAR(50) UNIQUE NOT NULL,
    base_salary DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================
-- DATOS DE PRUEBA
-- =========================================

-- Insertar usuarios de prueba con contraseñas encriptadas (password123)
INSERT INTO users (
    id, 
    first_name, 
    last_name, 
    birth_date, 
    address, 
    phone, 
    email, 
    password_hash, 
    role, 
    is_active, 
    document, 
    base_salary,
    created_at,
    updated_at
) VALUES

-- Usuario Administrador
('admin-123', 
 'Admin', 
 'Sistema', 
 '1980-01-01', 
 'Calle Admin #1-1', 
 '+57-300-000-0001', 
 'admin@crediya.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'ADMINISTRADOR', 
 true, 
 'admin001', 
 8000000.00,
 NOW(),
 NOW()),

-- Usuario Asesor
('asesor-123', 
 'María', 
 'García', 
 '1985-06-15', 
 'Carrera Asesor #2-2', 
 '+57-300-000-0002', 
 'asesor@crediya.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'ASESOR', 
 true, 
 'asesor001', 
 5000000.00,
 NOW(),
 NOW()),

-- Usuarios Cliente
('cliente-001', 
 'Ana', 
 'Pérez', 
 '1990-03-20', 
 'Calle 123 #45-67', 
 '+57-301-234-5678', 
 'ana.perez@email.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', 
 true, 
 '45678901', 
 2800000.00,
 NOW(),
 NOW()),

('cliente-002', 
 'Juan', 
 'Rodríguez', 
 '1988-09-10', 
 'Carrera 456 #78-90', 
 '+57-302-345-6789', 
 'juan.rodriguez@email.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', 
 true, 
 '56789012', 
 4200000.00,
 NOW(),
 NOW()),

('cliente-003', 
 'Laura', 
 'González', 
 '1995-01-12', 
 'Calle 63 #20-45', 
 '+57-306-789-0123', 
 'laura.gonzalez@email.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', 
 true, 
 '67890123', 
 3100000.00,
 NOW(),
 NOW()),

-- Usuario inactivo para testing
('cliente-inactive', 
 'Usuario', 
 'Inactivo', 
 '1992-05-25', 
 'Calle Inactiva #99-99', 
 '+57-309-999-9999', 
 'inactivo@email.com', 
 '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', 
 false,  -- Usuario inactivo
 'inactive001', 
 1500000.00,
 NOW(),
 NOW());

-- =========================================
-- INFORMACIÓN PARA TESTING
-- =========================================

-- Consulta para verificar los datos de testing
SELECT 
    CONCAT('Usuario: ', first_name, ' ', last_name) as nombre_completo,
    email,
    role,
    CASE WHEN is_active THEN 'Activo' ELSE 'Inactivo' END as estado,
    document,
    'password123' as password_testing
FROM users 
ORDER BY 
    CASE role 
        WHEN 'ADMINISTRADOR' THEN 1 
        WHEN 'ASESOR' THEN 2 
        WHEN 'CLIENTE' THEN 3 
    END,
    first_name;
