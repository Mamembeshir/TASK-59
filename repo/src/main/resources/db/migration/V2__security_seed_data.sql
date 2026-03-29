INSERT INTO users (username, password_hash, display_name, active)
VALUES
('sysadmin', '$2b$12$gUowh6c7hpTkCTeRDA60Eu3euqk/YsMI/qE3P.kJZJCxfEoSH3alK', 'System Administrator', TRUE),
('registrar', '$2b$12$rxeBX0C6fscx2rOmSAJb/uwD28Mu60WiivBrkk34GqHA0WFtGP/W6', 'Registrar Finance Clerk', TRUE),
('instructor', '$2b$12$YpoiA9GtQZnBRABxJR1yW.qmFLIuo7gSDW/D0gfco3OlgTYLLwo0q', 'Instructor', TRUE),
('inventory', '$2b$12$1BINMNG9/6ay/bwZ27X9/ONKbDKN43sIZtalBRf4eX5LIBlQHTzgu', 'Inventory Manager', TRUE),
('approver', '$2b$12$lY8F.KI2rx5HIMgUinHvpurm/f3/2CID3Xj53/O4kMaZDJMXttJA2', 'Procurement Approver', TRUE),
('store', '$2b$12$isEergTB1kHHND3BESxX8ea6czB1G.aVi0HnvMkU/b6N9/Ki9j8Ui', 'Store Manager', TRUE),
('student1', '$2b$12$TwHCm3.ULpq59ATdyLCm9.6bqvLDcvtBqLaVyaCZ8FFWhvRE5qq3C', 'Student User', TRUE)
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'SYSTEM_ADMIN'
WHERE u.username = 'sysadmin'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'REGISTRAR_FINANCE_CLERK'
WHERE u.username = 'registrar'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'INSTRUCTOR'
WHERE u.username = 'instructor'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'INVENTORY_MANAGER'
WHERE u.username = 'inventory'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'PROCUREMENT_APPROVER'
WHERE u.username = 'approver'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'STORE_MANAGER'
WHERE u.username = 'store'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'STUDENT'
WHERE u.username = 'student1'
ON DUPLICATE KEY UPDATE user_id = user_id;

INSERT INTO internal_api_clients (client_key, client_secret_hash, description, active)
VALUES ('local-sync-client', '$2b$12$7nyOtuM77lY29IiHJE4ByO5kNcFcwcN1NUgQoZUbVmmihojP4GXnu', 'Local LAN internal client', TRUE)
ON DUPLICATE KEY UPDATE description = VALUES(description), active = VALUES(active);
