-- Chạy bằng scripts/update-user-rbac.ps1 trong một transaction.
-- Chỉ bổ sung catalog/grant; không đổi user, mật khẩu, booking hoặc thu hồi quyền cũ.
-- Chạy lại không tạo trùng và không sinh event nếu không có thay đổi.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username ON users(username);
ALTER TABLE user_roles ALTER COLUMN assigned_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
DECLARE
    changed_roles integer;
    changed_permissions integer;
    changed_grants integer;
    admin_role_id bigint;
    operation_id text := gen_random_uuid()::text;
BEGIN
    INSERT INTO roles(code, name, description, created_at, updated_at, version)
    VALUES
        ('ROLE_ADMIN', 'Admin', 'Quản trị hệ thống', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('ROLE_CUSTOMER', 'Customer', 'Khách đặt phòng', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('ROLE_HOST', 'Host', 'Chủ cơ sở lưu trú', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('ROLE_SUPPORT', 'Support', 'Nhân viên hỗ trợ', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
    ON CONFLICT (code) DO NOTHING;
    GET DIAGNOSTICS changed_roles = ROW_COUNT;

    INSERT INTO permissions(code, name, description, created_at, updated_at, version)
    VALUES
        ('USER_READ', 'Tra cứu User', 'Xem danh sách và chi tiết User đã che dữ liệu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('USER_MANAGE', 'Quản trị User', 'Tạo tài khoản, sửa hồ sơ, khóa/mở khóa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('USER_ROLE_MANAGE', 'Cấp/thu hồi role của User', 'Chỉ trong phạm vi được ủy quyền', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('ROLE_READ', 'Tra cứu role', 'Xem role và các permission được gắn', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('ROLE_MANAGE', 'Quản trị role', 'Quản lý role và gắn/thu hồi permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('PERMISSION_READ', 'Tra cứu permission', 'Xem danh mục permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('PERMISSION_MANAGE', 'Quản trị permission', 'Quản lý danh mục permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
        ('AUDIT_READ', 'Tra cứu Audit Log', 'Đồng thời phải có ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
    ON CONFLICT (code) DO NOTHING;
    GET DIAGNOSTICS changed_permissions = ROW_COUNT;

    SELECT id INTO admin_role_id FROM roles WHERE code = 'ROLE_ADMIN' FOR UPDATE;
    INSERT INTO role_permissions(role_id, permission_id)
    SELECT admin_role_id, id FROM permissions
    WHERE code IN ('USER_READ', 'USER_MANAGE', 'USER_ROLE_MANAGE', 'ROLE_READ',
                   'ROLE_MANAGE', 'PERMISSION_READ', 'PERMISSION_MANAGE', 'AUDIT_READ')
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS changed_grants = ROW_COUNT;

    IF changed_roles + changed_permissions + changed_grants > 0 THEN
        INSERT INTO audit_logs(event_id, actor_id, actor_type, action, entity_type, entity_id,
                               before_state, after_state, reason, request_id, correlation_id,
                               occurred_at, outcome, created_at)
        VALUES (gen_random_uuid(), NULL, 'SYSTEM', 'RBAC_CATALOG_INITIALIZED', 'ROLE', admin_role_id,
                '{}'::jsonb, jsonb_build_object('rolesAdded', changed_roles,
                        'permissionsAdded', changed_permissions, 'adminGrantsAdded', changed_grants),
                'Khởi tạo permission cho API hồ sơ và quản trị User; audit chỉ ADMIN',
                operation_id, operation_id, CURRENT_TIMESTAMP, 'SUCCESS', CURRENT_TIMESTAMP);
    END IF;
END $$;
