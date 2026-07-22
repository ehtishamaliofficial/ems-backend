-- =============================================================================
-- Seed Data — Roles & Default Super Admin
-- =============================================================================

-- Default roles
INSERT INTO roles (name, description) VALUES ('SUPER_ADMIN', 'Full system access — manage everything')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('ADMIN', 'Administrative access — manage users and configuration')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('HR_MANAGER', 'Human Resources — manage employee records and payroll')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('DEPARTMENT_HEAD', 'Department lead — manage department employees')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name, description) VALUES ('EMPLOYEE', 'Standard employee — self-service access')
    ON CONFLICT (name) DO NOTHING;

-- Default Super Admin user
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (erp_no, username, email, password_hash, first_name, last_name, is_active, is_email_verified, created_by)
VALUES (
    'ERP-000001',
    'superadmin',
    'superadmin@nadra.gov.pk',
    '$2a$10$ENcA01D9hJsmGCv0i3QoFuVjJZBL5.xe9fUguaqx1X/AvO7kKUVva',
    'System',
    'Administrator',
    TRUE,
    TRUE,
    'SYSTEM'
) ON CONFLICT (username) DO NOTHING;

-- Assign SUPER_ADMIN role to default admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'superadmin' AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
