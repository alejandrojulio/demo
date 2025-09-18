-- ========================================
-- SCRIPT DE SINCRONIZACIÓN DE CLIENTES
-- Sincroniza datos de usuarios desde auth_db a loans_db
-- ========================================

-- NOTA: Este script debe ejecutarse cuando se necesite sincronizar
-- datos de clientes entre las dos bases de datos

-- ========================================
-- FUNCIÓN PARA SINCRONIZAR CLIENTE INDIVIDUAL
-- ========================================

-- Usar la base de datos de préstamos
USE loans_db;

-- Procedure para sincronizar un cliente específico desde auth_db
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS sp_sync_client_from_auth(
    IN p_auth_user_id BIGINT,
    IN p_document VARCHAR(20),
    IN p_email VARCHAR(255),
    IN p_first_name VARCHAR(100),
    IN p_last_name VARCHAR(100),
    IN p_phone VARCHAR(20),
    IN p_base_salary DECIMAL(15,2)
)
BEGIN
    DECLARE client_exists INT DEFAULT 0;
    
    -- Verificar si el cliente ya existe
    SELECT COUNT(*) INTO client_exists
    FROM clients 
    WHERE auth_user_id = p_auth_user_id OR document = p_document;
    
    IF client_exists > 0 THEN
        -- Actualizar cliente existente
        UPDATE clients 
        SET 
            email = p_email,
            first_name = p_first_name,
            last_name = p_last_name,
            phone = p_phone,
            base_salary = p_base_salary,
            updated_at = CURRENT_TIMESTAMP
        WHERE auth_user_id = p_auth_user_id OR document = p_document;
        
        SELECT CONCAT('Cliente actualizado: ', p_document) as result;
    ELSE
        -- Insertar nuevo cliente
        INSERT INTO clients (
            auth_user_id, document, email, first_name, last_name, 
            phone, base_salary, is_active
        ) VALUES (
            p_auth_user_id, p_document, p_email, p_first_name, p_last_name,
            p_phone, p_base_salary, TRUE
        );
        
        SELECT CONCAT('Cliente creado: ', p_document) as result;
    END IF;
END$$

DELIMITER ;

-- ========================================
-- SINCRONIZACIÓN MANUAL DE TODOS LOS USUARIOS
-- (Para uso inicial o migración completa)
-- ========================================

-- NOTA: Estos INSERTs deben ejecutarse manualmente después de
-- verificar que los usuarios existen en auth_db

-- Sincronizar usuarios iniciales (deben coincidir con auth_db)
INSERT INTO clients (auth_user_id, document, email, first_name, last_name, phone, base_salary, credit_score, employment_years)
VALUES
(3, '45678901', 'ana.perez@email.com', 'Ana', 'Pérez Silva', '+573012345678', 2800000.00, 720, 3),
(4, '56789012', 'juan.rodriguez@email.com', 'Juan', 'Rodríguez', '+573012345679', 4200000.00, 680, 5),
(5, '67890123', 'laura.gonzalez@email.com', 'Laura', 'González', '+573012345680', 5500000.00, 750, 7),
(6, '78901234', 'carlos.martinez@email.com', 'Carlos', 'Martínez', '+573012345681', 3200000.00, 690, 2),
(7, '89012345', 'maria.lopez@email.com', 'María', 'López', '+573012345682', 6800000.00, 800, 10)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    phone = VALUES(phone),
    base_salary = VALUES(base_salary),
    updated_at = CURRENT_TIMESTAMP;

-- ========================================
-- VERIFICACIÓN DE SINCRONIZACIÓN
-- ========================================

-- Query para verificar sincronización
SELECT 
    'Sincronización completada' as status,
    COUNT(*) as total_clients_synced
FROM clients 
WHERE is_active = TRUE;

-- Query para mostrar diferencias (si las hay)
SELECT 
    auth_user_id,
    document,
    email,
    CONCAT(first_name, ' ', last_name) as full_name,
    base_salary,
    created_at
FROM clients 
ORDER BY auth_user_id;

