-- ========================================
-- ESQUEMA DE BASE DE DATOS PARA LOADREQUESTSERVICE
-- ========================================

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS loans_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE loans_db;

-- ========================================
-- CONFIGURACIÓN DE TIMEZONE Y ENCODING
-- ========================================
SET time_zone = '+00:00';
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- ========================================
-- TABLA DE INFORMACIÓN CREDITICIA (SOLO DATOS ESPECÍFICOS DE PRÉSTAMOS)
-- ========================================

-- Tabla para información crediticia específica de préstamos (NO duplica datos de auth)
CREATE TABLE IF NOT EXISTS client_credit_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_document VARCHAR(20) NOT NULL UNIQUE, -- Referencia al documento del usuario en AuthService
    credit_score INT DEFAULT 650,
    debt_to_income_ratio DECIMAL(5,2) DEFAULT 0.00,
    employment_years INT DEFAULT 0,
    risk_category ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    last_credit_check TIMESTAMP NULL,
    credit_history_months INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_credit_info_document (client_document),
    INDEX idx_credit_info_score (credit_score),
    INDEX idx_credit_info_risk (risk_category),
    INDEX idx_credit_info_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE TIPOS DE PRÉSTAMO
-- ========================================

-- Tabla de tipos de préstamo con configuración de validación automática
CREATE TABLE IF NOT EXISTS loan_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_code ENUM('PERSONAL', 'VEHICLE', 'HOME', 'BUSINESS') NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    automatic_validation BOOLEAN NOT NULL DEFAULT FALSE,
    default_interest_rate DECIMAL(5,2) NOT NULL DEFAULT 15.0,
    min_amount DECIMAL(15,2) NOT NULL DEFAULT 100000.00,
    max_amount DECIMAL(15,2) NOT NULL DEFAULT 50000000.00,
    min_term_months INT NOT NULL DEFAULT 1,
    max_term_months INT NOT NULL DEFAULT 120,
    max_salary_multiplier DECIMAL(3,1) DEFAULT 5.0, -- Para validación de REVISION_MANUAL
    debt_capacity_percentage DECIMAL(3,2) DEFAULT 0.35, -- 35% máximo de ingresos para deuda
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_loan_types_code (type_code),
    INDEX idx_loan_types_active (is_active),
    INDEX idx_loan_types_automatic (automatic_validation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar tipos de préstamo con configuración inicial
INSERT INTO loan_types (type_code, display_name, description, automatic_validation, default_interest_rate, min_amount, max_amount, min_term_months, max_term_months, max_salary_multiplier, debt_capacity_percentage) VALUES
('PERSONAL', 'Préstamo Personal', 'Préstamo personal para libre inversión con validación automática', TRUE, 15.5, 100000.00, 15000000.00, 6, 60, 5.0, 0.35),
('VEHICLE', 'Préstamo Vehicular', 'Préstamo para compra de vehículo con validación automática', TRUE, 12.8, 5000000.00, 80000000.00, 12, 84, 8.0, 0.40),
('HOME', 'Préstamo Hipotecario', 'Préstamo hipotecario que requiere revisión manual', FALSE, 9.2, 20000000.00, 500000000.00, 60, 360, 15.0, 0.30),
('BUSINESS', 'Préstamo Empresarial', 'Préstamo para empresas que requiere revisión manual', FALSE, 18.3, 10000000.00, 200000000.00, 6, 120, 10.0, 0.45);

-- ========================================
-- TABLA DE SOLICITUDES DE PRÉSTAMO
-- ========================================

-- Tabla principal de solicitudes de préstamo
CREATE TABLE IF NOT EXISTS loan_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_document VARCHAR(20) NOT NULL, -- Referencia al cliente
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
    approved_by VARCHAR(50), -- Usuario del AuthService que aprobó
    rejection_reason TEXT,
    
    -- Campos para tracking de validación automática
    automatic_validation_requested BOOLEAN DEFAULT FALSE,
    automatic_validation_completed BOOLEAN DEFAULT FALSE,
    validation_details JSON, -- Detalles del proceso de validación
    
    -- Referencia al documento del cliente (verificación via AuthService)
    
    INDEX idx_loan_requests_client (client_document),
    INDEX idx_loan_requests_status (status),
    INDEX idx_loan_requests_type (loan_type),
    INDEX idx_loan_requests_created (created_at),
    INDEX idx_loan_requests_amount (amount),
    INDEX idx_loan_requests_automatic (automatic_validation_requested),
    INDEX idx_loan_requests_validation_completed (automatic_validation_completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE EVALUACIÓN DE CAPACIDAD
-- ========================================

-- Tabla para guardar resultados de evaluación de capacidad de endeudamiento
CREATE TABLE IF NOT EXISTS debt_capacity_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_request_id BIGINT NOT NULL,
    client_document VARCHAR(20) NOT NULL,
    
    -- Datos de entrada
    monthly_income DECIMAL(15,2) NOT NULL,
    existing_monthly_debt DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    requested_amount DECIMAL(15,2) NOT NULL,
    requested_term_months INT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    
    -- Cálculos
    max_debt_capacity DECIMAL(15,2) NOT NULL, -- 35% de ingresos
    available_capacity DECIMAL(15,2) NOT NULL, -- Capacidad disponible
    new_loan_payment DECIMAL(15,2) NOT NULL, -- Cuota del nuevo préstamo
    capacity_utilization DECIMAL(5,2) NOT NULL, -- % de capacidad utilizada
    
    -- Resultado
    decision ENUM('APROBADO', 'RECHAZADO', 'REVISION_MANUAL') NOT NULL,
    decision_reason TEXT,
    
    -- Metadata
    evaluation_source ENUM('AUTOMATIC', 'MANUAL') NOT NULL DEFAULT 'AUTOMATIC',
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_by VARCHAR(100) DEFAULT 'DebtCapacityService',
    
    FOREIGN KEY (loan_request_id) REFERENCES loan_requests(id) ON DELETE CASCADE,
    
    INDEX idx_debt_evaluations_loan (loan_request_id),
    INDEX idx_debt_evaluations_client (client_document),
    INDEX idx_debt_evaluations_decision (decision),
    INDEX idx_debt_evaluations_processed (processed_at),
    INDEX idx_debt_evaluations_source (evaluation_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE PLAN DE PAGOS
-- ========================================

-- Tabla para almacenar el plan de pagos detallado
CREATE TABLE IF NOT EXISTS payment_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_request_id BIGINT NOT NULL,
    payment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_payment DECIMAL(15,2) NOT NULL,
    interest_payment DECIMAL(15,2) NOT NULL,
    total_payment DECIMAL(15,2) NOT NULL,
    remaining_balance DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loan_request_id) REFERENCES loan_requests(id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_loan_payment (loan_request_id, payment_number),
    INDEX idx_payment_plans_loan (loan_request_id),
    INDEX idx_payment_plans_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- TABLA DE AUDITORÍA DE SOLICITUDES
-- ========================================

-- Tabla para auditoría de cambios en solicitudes
CREATE TABLE IF NOT EXISTS loan_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_request_id BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(100), -- Usuario que hizo el cambio
    change_reason TEXT,
    change_details JSON, -- Detalles adicionales del cambio
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loan_request_id) REFERENCES loan_requests(id) ON DELETE CASCADE,
    
    INDEX idx_loan_audit_request (loan_request_id),
    INDEX idx_loan_audit_status (new_status),
    INDEX idx_loan_audit_changed_by (changed_by),
    INDEX idx_loan_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- DATOS INICIALES DE INFORMACIÓN CREDITICIA
-- ========================================

-- Insertar información crediticia para clientes existentes en AuthService
INSERT INTO client_credit_info (client_document, credit_score, employment_years, risk_category, credit_history_months) VALUES
('45678901', 720, 3, 'LOW', 36),      -- Ana Pérez Silva
('56789012', 680, 5, 'MEDIUM', 60),   -- Juan Rodríguez  
('67890123', 750, 7, 'LOW', 84),      -- Laura González
('78901234', 690, 2, 'MEDIUM', 24),   -- Carlos Martínez
('89012345', 800, 10, 'LOW', 120);    -- María López

-- ========================================
-- DATOS DE PRUEBA - PRÉSTAMOS EXISTENTES
-- ========================================

-- Insertar algunos préstamos aprobados para testing
INSERT INTO loan_requests (client_document, amount, term_in_months, loan_type, status, interest_rate, monthly_payment, approved_amount, approved_at, approved_by) VALUES
-- Juan Rodríguez tiene un préstamo vehicular aprobado
('56789012', 25000000, 48, 'VEHICLE', 'APPROVED', 12.8, 743680.50, 25000000.00, '2024-01-15 10:00:00', 'asesor@crediya.com'),
-- Laura González tiene un préstamo personal aprobado
('67890123', 8000000, 36, 'PERSONAL', 'APPROVED', 15.5, 287650.25, 8000000.00, '2024-02-20 14:30:00', 'asesor@crediya.com'),
-- María López tiene dos préstamos aprobados
('89012345', 15000000, 60, 'PERSONAL', 'APPROVED', 15.5, 356840.75, 15000000.00, '2024-01-10 09:15:00', 'asesor@crediya.com'),
('89012345', 35000000, 72, 'VEHICLE', 'APPROVED', 12.8, 662150.80, 35000000.00, '2024-03-05 16:45:00', 'asesor@crediya.com');

-- ========================================
-- PROCEDIMIENTOS ALMACENADOS
-- ========================================

DELIMITER $$

-- Procedimiento para calcular cuota mensual
CREATE FUNCTION IF NOT EXISTS fn_calculate_monthly_payment(
    principal DECIMAL(15,2),
    annual_rate DECIMAL(5,2),
    months INT
) RETURNS DECIMAL(15,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE monthly_rate DECIMAL(10,8);
    DECLARE payment DECIMAL(15,2);
    
    IF annual_rate = 0 THEN
        RETURN principal / months;
    END IF;
    
    SET monthly_rate = annual_rate / 100 / 12;
    SET payment = principal * (monthly_rate * POWER(1 + monthly_rate, months)) / (POWER(1 + monthly_rate, months) - 1);
    
    RETURN ROUND(payment, 2);
END$$

-- Procedimiento para obtener capacidad de endeudamiento de un cliente
CREATE PROCEDURE IF NOT EXISTS sp_get_client_debt_capacity(
    IN p_client_document VARCHAR(20),
    OUT p_monthly_income DECIMAL(15,2),
    OUT p_current_monthly_debt DECIMAL(15,2),
    OUT p_available_capacity DECIMAL(15,2)
)
BEGIN
    DECLARE v_debt_percentage DECIMAL(3,2) DEFAULT 0.35;
    
    -- Obtener ingresos del cliente
    SELECT base_salary INTO p_monthly_income
    FROM clients 
    WHERE document = p_client_document AND is_active = TRUE;
    
    -- Calcular deuda mensual actual (préstamos aprobados)
    SELECT COALESCE(SUM(monthly_payment), 0) INTO p_current_monthly_debt
    FROM loan_requests 
    WHERE client_document = p_client_document 
    AND status = 'APPROVED';
    
    -- Calcular capacidad disponible
    SET p_available_capacity = (p_monthly_income * v_debt_percentage) - p_current_monthly_debt;
END$$

-- Procedimiento para validar si un préstamo requiere revisión manual
CREATE FUNCTION IF NOT EXISTS fn_requires_manual_review(
    amount DECIMAL(15,2),
    client_salary DECIMAL(15,2),
    loan_type_code VARCHAR(20)
) RETURNS BOOLEAN
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE max_multiplier DECIMAL(3,1);
    DECLARE salary_multiplier DECIMAL(5,2);
    
    -- Obtener multiplicador máximo para el tipo de préstamo
    SELECT max_salary_multiplier INTO max_multiplier
    FROM loan_types
    WHERE type_code = loan_type_code;
    
    -- Calcular cuántos salarios representa el préstamo
    SET salary_multiplier = amount / client_salary;
    
    -- Si supera el límite, requiere revisión manual
    RETURN salary_multiplier > max_multiplier;
END$$

DELIMITER ;

-- ========================================
-- TRIGGERS PARA AUDITORÍA
-- ========================================

DELIMITER $$

-- Trigger para auditar cambios de estado
CREATE TRIGGER tr_loan_status_audit 
AFTER UPDATE ON loan_requests
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO loan_audit_log (
            loan_request_id, 
            old_status, 
            new_status, 
            changed_by, 
            change_reason,
            change_details
        ) VALUES (
            NEW.id,
            OLD.status,
            NEW.status,
            COALESCE(NEW.approved_by, 'SYSTEM'),
            NEW.rejection_reason,
            JSON_OBJECT(
                'old_amount', OLD.amount,
                'new_amount', NEW.amount,
                'old_interest_rate', OLD.interest_rate,
                'new_interest_rate', NEW.interest_rate,
                'automatic_validation', NEW.automatic_validation_completed
            )
        );
    END IF;
END$$

DELIMITER ;

-- ========================================
-- VISTAS PARA CONSULTAS FRECUENTES
-- ========================================

-- Vista para solicitudes con información del cliente
CREATE VIEW v_loan_requests_with_client AS
SELECT 
    lr.*,
    c.first_name,
    c.last_name,
    c.email,
    c.phone,
    c.base_salary,
    c.credit_score,
    c.employment_years,
    lt.display_name as loan_type_display,
    lt.automatic_validation,
    lt.max_salary_multiplier,
    (lr.amount / c.base_salary) as salary_multiplier,
    fn_requires_manual_review(lr.amount, c.base_salary, lr.loan_type) as requires_manual_review
FROM loan_requests lr
JOIN clients c ON lr.client_document = c.document
JOIN loan_types lt ON lr.loan_type = lt.type_code;

-- Vista para capacidad de endeudamiento por cliente
CREATE VIEW v_client_debt_capacity AS
SELECT 
    c.document,
    c.first_name,
    c.last_name,
    c.base_salary,
    COALESCE(SUM(CASE WHEN lr.status = 'APPROVED' THEN lr.monthly_payment ELSE 0 END), 0) as current_monthly_debt,
    (c.base_salary * 0.35) as max_debt_capacity,
    (c.base_salary * 0.35) - COALESCE(SUM(CASE WHEN lr.status = 'APPROVED' THEN lr.monthly_payment ELSE 0 END), 0) as available_capacity,
    COUNT(CASE WHEN lr.status = 'APPROVED' THEN 1 END) as active_loans,
    COUNT(CASE WHEN lr.status = 'PENDING_REVIEW' THEN 1 END) as pending_loans
FROM clients c
LEFT JOIN loan_requests lr ON c.document = lr.client_document
WHERE c.is_active = TRUE
GROUP BY c.document, c.first_name, c.last_name, c.base_salary;

-- ========================================
-- ÍNDICES ADICIONALES PARA PERFORMANCE
-- ========================================

-- Índices compuestos para consultas frecuentes
CREATE INDEX idx_loan_requests_client_status ON loan_requests(client_document, status);
CREATE INDEX idx_loan_requests_type_status ON loan_requests(loan_type, status);
CREATE INDEX idx_loan_requests_amount_status ON loan_requests(amount, status);
CREATE INDEX idx_clients_salary_active ON clients(base_salary, is_active);

-- ========================================
-- CONFIGURACIÓN FINAL
-- ========================================

-- Verificar estructura
SELECT 'LoadRequestService Database Schema Created Successfully' as status;
SELECT COUNT(*) as total_clients FROM clients;
SELECT COUNT(*) as total_loan_types FROM loan_types;
SELECT COUNT(*) as total_loan_requests FROM loan_requests;
SELECT COUNT(*) as approved_loans FROM loan_requests WHERE status = 'APPROVED';
