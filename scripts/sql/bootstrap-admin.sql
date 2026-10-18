-- Bootstrap admin on an empty database. The supplied password is already BCrypt-hashed.
BEGIN;
INSERT INTO roles (code, name, description, created_at, updated_at, version)
SELECT 'ROLE_ADMIN', 'Admin', 'System administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ROLE_ADMIN');

INSERT INTO users (username, email, full_name, password_hash, status, active,
                   two_factor_enabled, token_version, verified_at, created_at, updated_at, version)
SELECT 'admin', 'admin@localhost', 'Admin',
       '$2a$10$YH38pBkM4YUlvPeZvHlyFOel.UW9jzsxfC.dE.lqOHpqUxDx.9PrK',
       'ACTIVE', TRUE, FALSE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM users u CROSS JOIN roles r
WHERE u.username = 'admin' AND r.code = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
COMMIT;
