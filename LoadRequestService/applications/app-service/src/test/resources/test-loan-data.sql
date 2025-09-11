-- =========================================
-- CREACIÓN DE TABLAS PARA TESTS
-- =========================================

-- Tabla de usuarios (referencial)
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

-- Tabla de solicitudes de préstamo
CREATE TABLE IF NOT EXISTS loan_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_document VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    term_in_months INT NOT NULL,
    loan_type ENUM('PERSONAL', 'VEHICLE', 'HOME', 'BUSINESS') NOT NULL,
    status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW') NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    notes TEXT,
    
    -- Campos adicionales para aprobaciones/rechazos
    approved_amount DECIMAL(15,2),
    interest_rate DECIMAL(5,2),
    monthly_payment DECIMAL(15,2),
    approved_at TIMESTAMP NULL,
    approved_by VARCHAR(255),
    rejection_reason TEXT,
    
    INDEX idx_client_document (client_document),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Tabla de historial de solicitudes
CREATE TABLE IF NOT EXISTS loan_request_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_request_id BIGINT NOT NULL,
    previous_status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW'),
    new_status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CANCELLED', 'MANUAL_REVIEW') NOT NULL,
    changed_by VARCHAR(255) NOT NULL,
    change_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loan_request_id) REFERENCES loan_requests(id) ON DELETE CASCADE,
    INDEX idx_loan_request_id (loan_request_id),
    INDEX idx_created_at (created_at)
);

-- =========================================
-- DATOS DE PRUEBA - USUARIOS
-- =========================================

INSERT INTO users (
    id, first_name, last_name, birth_date, address, phone, email, 
    password_hash, role, is_active, document, base_salary, created_at, updated_at
) VALUES

-- Usuario Administrador
('admin-123', 'Admin', 'Sistema', '1980-01-01', 'Calle Admin #1-1', '+57-300-000-0001', 
 'admin@crediya.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'ADMINISTRADOR', true, 'admin001', 8000000.00, NOW(), NOW()),

-- Usuario Asesor
('asesor-123', 'María', 'García', '1985-06-15', 'Carrera Asesor #2-2', '+57-300-000-0002', 
 'asesor@crediya.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'ASESOR', true, 'asesor001', 5000000.00, NOW(), NOW()),

-- Usuarios Cliente
('cliente-001', 'Ana', 'Pérez', '1990-03-20', 'Calle 123 #45-67', '+57-301-234-5678', 
 'ana.perez@email.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', true, '45678901', 2800000.00, NOW(), NOW()),

('cliente-002', 'Juan', 'Rodríguez', '1988-09-10', 'Carrera 456 #78-90', '+57-302-345-6789', 
 'juan.rodriguez@email.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', true, '56789012', 4200000.00, NOW(), NOW()),

('cliente-003', 'Laura', 'González', '1995-01-12', 'Calle 63 #20-45', '+57-306-789-0123', 
 'laura.gonzalez@email.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', true, '67890123', 3100000.00, NOW(), NOW()),

('cliente-004', 'Roberto', 'López', '1983-12-03', 'Carrera 68 #35-20', '+57-307-890-1234', 
 'roberto.lopez@email.com', '$2a$10$N.hl6LNS5wUL6aaR5Z3mOeqfBSYrj9mLc/kMj8nKvMKklD1nK0A6e', 
 'CLIENTE', true, '78901234', 5500000.00, NOW(), NOW());

-- =========================================
-- DATOS DE PRUEBA - SOLICITUDES DE PRÉSTAMO
-- =========================================

INSERT INTO loan_requests (
    client_document, amount, term_in_months, loan_type, status, 
    created_at, updated_at, notes, approved_amount, interest_rate, 
    monthly_payment, approved_at, approved_by, rejection_reason
) VALUES

-- Solicitudes PENDIENTES DE REVISIÓN (para testing del endpoint de consulta)
('45678901', 8500000.00, 36, 'PERSONAL', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 
 'Solicitud de préstamo personal para consolidación de deudas. Cliente con buen historial crediticio.', 
 NULL, NULL, NULL, NULL, NULL, NULL),

('67890123', 15000000.00, 60, 'VEHICLE', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 
 'Préstamo para compra de vehículo nuevo. Documentación completa.', 
 NULL, NULL, NULL, NULL, NULL, NULL),

('78901234', 25000000.00, 84, 'HOME', 'MANUAL_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 
 'Préstamo hipotecario. Requiere evaluación manual por monto alto.', 
 NULL, NULL, NULL, NULL, NULL, NULL),

-- Solicitudes APROBADAS (para cálculo de deuda total)
('56789012', 12000000.00, 48, 'VEHICLE', 'APPROVED', 
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), 
 'Préstamo para vehículo aprobado.', 
 12000000.00, 12.8, 350000.00, DATE_SUB(NOW(), INTERVAL 25 DAY), 'asesor@crediya.com', NULL),

('45678901', 5000000.00, 24, 'PERSONAL', 'APPROVED', 
 DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), 
 'Préstamo personal aprobado anteriormente.', 
 5000000.00, 15.5, 250000.00, DATE_SUB(NOW(), INTERVAL 55 DAY), 'asesor@crediya.com', NULL),

-- Solicitudes RECHAZADAS
('67890123', 30000000.00, 72, 'BUSINESS', 'REJECTED', 
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 
 'Solicitud de préstamo empresarial.', 
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), 'asesor@crediya.com', 
 'Monto solicitado excede la capacidad de pago del solicitante'),

-- Solicitudes CANCELADAS
('78901234', 20000000.00, 60, 'HOME', 'CANCELLED', 
 DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), 
 'Solicitud cancelada por el cliente.', 
 NULL, NULL, NULL, NULL, NULL, NULL),

-- Más solicitudes para testing de paginación
('45678901', 3000000.00, 18, 'PERSONAL', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 
 'Nueva solicitud de préstamo personal.', 
 NULL, NULL, NULL, NULL, NULL, NULL),

('67890123', 18000000.00, 72, 'HOME', 'MANUAL_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW(), 
 'Solicitud de préstamo hipotecario para revisión manual.', 
 NULL, NULL, NULL, NULL, NULL, NULL);

-- =========================================
-- DATOS DE PRUEBA - HISTORIAL
-- =========================================

INSERT INTO loan_request_history (
    loan_request_id, previous_status, new_status, changed_by, change_reason, created_at
) VALUES

(4, 'PENDING_REVIEW', 'APPROVED', 'asesor@crediya.com', 'Cliente cumple con todos los requisitos', DATE_SUB(NOW(), INTERVAL 25 DAY)),
(5, 'PENDING_REVIEW', 'APPROVED', 'asesor@crediya.com', 'Aprobación estándar', DATE_SUB(NOW(), INTERVAL 55 DAY)),
(6, 'PENDING_REVIEW', 'REJECTED', 'asesor@crediya.com', 'Capacidad de pago insuficiente', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(7, 'PENDING_REVIEW', 'CANCELLED', 'sistema', 'Cancelación por solicitud del cliente', DATE_SUB(NOW(), INTERVAL 40 DAY));

-- =========================================
-- CONSULTAS DE VERIFICACIÓN PARA TESTING
-- =========================================

-- Consulta para verificar los datos de testing
SELECT 
    'RESUMEN DE DATOS PARA TESTING' as info;

SELECT 
    COUNT(*) as total_usuarios,
    SUM(CASE WHEN role = 'ADMINISTRADOR' THEN 1 ELSE 0 END) as administradores,
    SUM(CASE WHEN role = 'ASESOR' THEN 1 ELSE 0 END) as asesores,
    SUM(CASE WHEN role = 'CLIENTE' THEN 1 ELSE 0 END) as clientes
FROM users;

SELECT 
    COUNT(*) as total_solicitudes,
    SUM(CASE WHEN status = 'PENDING_REVIEW' THEN 1 ELSE 0 END) as pendientes_revision,
    SUM(CASE WHEN status = 'MANUAL_REVIEW' THEN 1 ELSE 0 END) as revision_manual,
    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) as aprobadas,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rechazadas,
    SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as canceladas
FROM loan_requests;

-- Consulta que simula el endpoint de solicitudes para revisión
SELECT 
    lr.id,
    lr.amount as monto,
    lr.term_in_months as plazo,
    COALESCE(u.email, '') as email,
    CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as nombre,
    lr.loan_type as tipo_prestamo,
    CASE 
        WHEN lr.loan_type = 'PERSONAL' THEN 15.5
        WHEN lr.loan_type = 'VEHICLE' THEN 12.8
        WHEN lr.loan_type = 'HOME' THEN 9.2
        WHEN lr.loan_type = 'BUSINESS' THEN 18.3
        ELSE 15.0
    END as tasa_interes,
    lr.status as estado_solicitud,
    COALESCE(u.base_salary, 0) as salario_base,
    COALESCE(
        (SELECT SUM(deuda.amount * 0.02) 
         FROM loan_requests deuda 
         WHERE deuda.client_document = lr.client_document 
         AND deuda.status = 'APPROVED'), 0
    ) as deuda_mensual,
    lr.client_document as documento_cliente,
    COALESCE(lr.notes, '') as notas
FROM loan_requests lr
LEFT JOIN users u ON lr.client_document = u.document
WHERE lr.status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW')
ORDER BY lr.created_at DESC;
