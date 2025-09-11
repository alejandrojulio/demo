-- Consultas para diagnosticar el problema de autenticación

USE crediyadb;

-- 1. Verificar si el usuario existe
SELECT 
    u.id,
    u.email,
    u.password,
    u.role_id,
    r.name as role_name,
    CASE WHEN u.password IS NULL THEN 'NO PASSWORD' ELSE 'HAS PASSWORD' END as password_status
FROM users u
LEFT JOIN role r ON u.role_id = r.id
WHERE u.email = 'admin@crediya.com';

-- 2. Verificar todos los usuarios
SELECT 
    u.email,
    r.name as role_name,
    CASE WHEN u.password IS NULL THEN 'NO PASSWORD' ELSE 'HAS PASSWORD' END as password_status,
    LENGTH(u.password) as password_length
FROM users u
LEFT JOIN role r ON u.role_id = r.id
ORDER BY u.email;

-- 3. Verificar estructura de la tabla users
DESCRIBE users;

-- 4. Verificar roles
SELECT * FROM role ORDER BY id;

