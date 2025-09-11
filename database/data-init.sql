-- Script de datos iniciales para CrediYa
-- Base de datos única con datos de prueba para ambos microservicios

USE crediya_db;

-- ========================================
-- DATOS DE USUARIOS (Compartidos entre microservicios)
-- ========================================

-- Insertar usuarios de prueba
-- Nota: Las contraseñas están hasheadas con BCrypt (strength 12)
-- Contraseña original para todos: "password123"

INSERT INTO users (id, document, first_name, last_name, birth_date, address, phone, email, password_hash, role, is_active, created_at, updated_at, base_salary) VALUES

-- Administrador del sistema
('admin-001', '12345678', 'Carlos', 'Administrador', '1985-03-15', 'Calle 100 #15-30, Bogotá', '+57-301-234-5678', 'admin@crediya.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'ADMINISTRADOR', true, NOW(), NOW(), 5000000.00),

-- Asesores financieros
('asesor-001', '23456789', 'María', 'García López', '1990-07-22', 'Carrera 7 #45-67, Bogotá', '+57-302-345-6789', 'asesor@crediya.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'ASESOR', true, NOW(), NOW(), 3500000.00),

('asesor-002', '34567890', 'Luis', 'Martínez Rodríguez', '1988-11-08', 'Carrera 15 #80-25, Bogotá', '+57-303-456-7890', 'luis.martinez@crediya.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'ASESOR', true, NOW(), NOW(), 3500000.00),

-- Clientes de prueba
('cliente-001', '45678901', 'Ana', 'Pérez Silva', '1992-05-18', 'Calle 85 #12-34, Bogotá', '+57-304-567-8901', 'ana.perez@email.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'CLIENTE', true, NOW(), NOW(), 2800000.00),

('cliente-002', '56789012', 'Juan', 'Rodríguez Castro', '1987-09-25', 'Carrera 30 #50-75, Bogotá', '+57-305-678-9012', 'juan.rodriguez@email.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'CLIENTE', true, NOW(), NOW(), 4200000.00),

('cliente-003', '67890123', 'Laura', 'González Morales', '1995-01-12', 'Calle 63 #20-45, Bogotá', '+57-306-789-0123', 'laura.gonzalez@email.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'CLIENTE', true, NOW(), NOW(), 3100000.00),

('cliente-004', '78901234', 'Roberto', 'López Vargas', '1983-12-03', 'Carrera 68 #35-20, Bogotá', '+57-307-890-1234', 'roberto.lopez@email.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'CLIENTE', true, NOW(), NOW(), 5500000.00),

('cliente-005', '89012345', 'Patricia', 'Herrera Jiménez', '1991-08-30', 'Calle 127 #18-60, Bogotá', '+57-308-901-2345', 'patricia.herrera@email.com', '$2a$10$coafGzAD5510H6iBKoVtIOeXUUBzJ9wKXdZRGH/zMCeksXQrnLg9O', 'CLIENTE', true, NOW(), NOW(), 2900000.00);

-- ========================================
-- DATOS DE SOLICITUDES DE PRÉSTAMO
-- ========================================

-- Insertar solicitudes de préstamo de prueba
INSERT INTO loan_requests (client_document, amount, term_in_months, loan_type, status, created_at, updated_at, notes, interest_rate, approved_by) VALUES

-- Solicitudes PENDIENTES DE REVISIÓN (para testing del endpoint)
('45678901', 8500000.00, 36, 'PERSONAL', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 
 'Solicitud de préstamo personal para consolidación de deudas. Cliente con buen historial crediticio.', 15.5, NULL),

('67890123', 15000000.00, 60, 'VEHICLE', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 
 'Préstamo para compra de vehículo nuevo. Documentación completa.', 12.8, NULL),

('89012345', 25000000.00, 84, 'HOME', 'MANUAL_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 
 'Préstamo hipotecario. Requiere evaluación manual por monto alto.', 9.2, NULL),

-- Solicitudes APROBADAS (para cálculo de deuda total)
('56789012', 12000000.00, 48, 'VEHICLE', 'APPROVED', 
 DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY), 
 'Préstamo vehicular aprobado. Cliente cumple todos los requisitos.', 12.8, 'asesor-001'),

('78901234', 6000000.00, 24, 'PERSONAL', 'APPROVED', 
 DATE_SUB(NOW(), INTERVAL 45 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY), 
 'Préstamo personal aprobado para mejoras del hogar.', 15.5, 'asesor-002'),

-- Solicitudes RECHAZADAS (para testing del endpoint)
('45678901', 3500000.00, 18, 'PERSONAL', 'REJECTED', 
 DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 
 'Solicitud rechazada: relación deuda/ingreso muy alta.', NULL, 'asesor-001'),

-- Solicitudes CANCELADAS
('67890123', 5000000.00, 12, 'PERSONAL', 'CANCELLED', 
 DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 55 DAY), 
 'Cliente canceló la solicitud antes de la aprobación.', NULL, NULL),

-- Más solicitudes para REVISIÓN MANUAL
('56789012', 18000000.00, 72, 'BUSINESS', 'MANUAL_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 
 'Préstamo empresarial. Requiere análisis detallado del plan de negocio.', 18.3, NULL),

('78901234', 32000000.00, 120, 'HOME', 'PENDING_REVIEW', 
 DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW(), 
 'Préstamo hipotecario de alto valor. Pendiente de verificación de ingresos.', 9.2, NULL);

-- Actualizar campos calculados para solicitudes aprobadas
UPDATE loan_requests 
SET 
    approved_amount = amount,
    monthly_payment = ROUND((amount * (interest_rate/100/12) * POW(1 + interest_rate/100/12, term_in_months)) / (POW(1 + interest_rate/100/12, term_in_months) - 1), 2),
    approved_at = updated_at
WHERE status = 'APPROVED';

-- ========================================
-- DATOS DE SESIONES (Para testing de AuthService)
-- ========================================

-- Insertar algunas sesiones de ejemplo (expiradas para testing)
INSERT INTO user_sessions (user_id, token_hash, expires_at, created_at, is_active) VALUES
('admin-001', SHA2('sample_admin_token_123', 256), DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), true),
('asesor-001', SHA2('sample_asesor_token_456', 256), DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), true),
('cliente-001', SHA2('sample_client_token_789', 256), DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), true);

-- ========================================
-- MOSTRAR ESTADÍSTICAS DE DATOS CREADOS
-- ========================================

-- Resumen de usuarios por rol
SELECT 
    '=== USUARIOS CREADOS POR ROL ===' as 'RESUMEN';

SELECT 
    role as 'Rol',
    COUNT(*) as 'Cantidad',
    GROUP_CONCAT(CONCAT(first_name, ' ', last_name) SEPARATOR ', ') as 'Usuarios'
FROM users 
WHERE is_active = true
GROUP BY role
ORDER BY 
    CASE role 
        WHEN 'ADMINISTRADOR' THEN 1 
        WHEN 'ASESOR' THEN 2 
        WHEN 'CLIENTE' THEN 3 
    END;

-- Estadísticas de solicitudes
SELECT 
    '=== SOLICITUDES CREADAS POR ESTADO ===' as 'RESUMEN';

SELECT 
    status as 'Estado',
    COUNT(*) as 'Cantidad',
    CONCAT('$', FORMAT(AVG(amount), 0)) as 'Monto_Promedio',
    CONCAT('$', FORMAT(SUM(amount), 0)) as 'Monto_Total'
FROM loan_requests 
GROUP BY status
ORDER BY 
    CASE status 
        WHEN 'PENDING_REVIEW' THEN 1 
        WHEN 'MANUAL_REVIEW' THEN 2 
        WHEN 'APPROVED' THEN 3 
        WHEN 'REJECTED' THEN 4 
        WHEN 'CANCELLED' THEN 5 
    END;

-- Solicitudes que aparecerán en el endpoint de revisión
SELECT 
    '=== SOLICITUDES PARA REVISIÓN MANUAL (ENDPOINT TEST) ===' as 'INFO';

SELECT 
    lr.id,
    CONCAT(u.first_name, ' ', u.last_name) as 'Cliente',
    u.email,
    CONCAT('$', FORMAT(lr.amount, 0)) as 'Monto',
    lr.term_in_months as 'Plazo_Meses',
    lr.loan_type as 'Tipo',
    lr.status as 'Estado',
    DATE_FORMAT(lr.created_at, '%Y-%m-%d %H:%i') as 'Fecha_Creación'
FROM loan_requests lr
INNER JOIN users u ON lr.client_document = u.document
WHERE lr.status IN ('PENDING_REVIEW', 'REJECTED', 'MANUAL_REVIEW')
ORDER BY lr.created_at DESC;

-- Información para testing
SELECT 
    '=== INFORMACIÓN PARA TESTING ===' as 'INFO';

SELECT 
    CONCAT(first_name, ' ', last_name) as 'Nombre_Completo',
    email as 'Email',
    role as 'Rol',
    'password123' as 'Contraseña',
    document as 'Documento'
FROM users 
WHERE is_active = true
ORDER BY 
    CASE role 
        WHEN 'ADMINISTRADOR' THEN 1 
        WHEN 'ASESOR' THEN 2 
        WHEN 'CLIENTE' THEN 3 
    END,
    first_name;

SELECT 
    'Usar estos datos para probar los endpoints:' as 'INSTRUCCIONES',
    'POST http://localhost:8080/api/v1/auth/login' as 'Login',
    'GET http://localhost:8081/api/v1/solicitud?page=0&size=10' as 'Solicitudes_Revision';
