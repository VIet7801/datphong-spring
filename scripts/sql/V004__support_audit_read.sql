-- UC-19: ADMIN/SUPPORT được tra cứu audit khi có AUDIT_READ.
-- Chạy sau V003, trong cùng transaction của scripts/update-user-rbac.ps1.
-- Không sửa V003 đã được áp dụng trên các máy khác.
DO $$
DECLARE
    support_role_id bigint;
    audit_permission_id bigint;
    previous_description text;
    previous_grant boolean;
    added_grants integer;
    updated_permissions integer;
    policy_description constant text := 'Tra cứu Audit Log theo UC-19; yêu cầu ROLE_ADMIN hoặc ROLE_SUPPORT';
    operation_id text := gen_random_uuid()::text;
BEGIN
    -- Cùng thứ tự khóa với các Service thay đổi RBAC.
    PERFORM id FROM roles WHERE code = 'ROLE_ADMIN' FOR UPDATE;
    SELECT id INTO support_role_id FROM roles WHERE code = 'ROLE_SUPPORT' FOR UPDATE;
    SELECT id, description INTO audit_permission_id, previous_description
    FROM permissions WHERE code = 'AUDIT_READ' FOR UPDATE;

    IF support_role_id IS NULL OR audit_permission_id IS NULL THEN
        RAISE EXCEPTION 'Apply V003 before V004: ROLE_SUPPORT/AUDIT_READ is missing';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM role_permissions
        WHERE role_id = support_role_id AND permission_id = audit_permission_id
    ) INTO previous_grant;

    INSERT INTO role_permissions(role_id, permission_id)
    VALUES (support_role_id, audit_permission_id)
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS added_grants = ROW_COUNT;

    IF added_grants > 0 THEN
        UPDATE roles SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id = support_role_id;
    END IF;

    UPDATE permissions
    SET description = policy_description, version = version + 1, updated_at = CURRENT_TIMESTAMP
    WHERE id = audit_permission_id AND description IS DISTINCT FROM policy_description;
    GET DIAGNOSTICS updated_permissions = ROW_COUNT;

    IF added_grants + updated_permissions > 0 THEN
        INSERT INTO audit_logs(event_id, actor_id, actor_type, action, entity_type, entity_id,
                               before_state, after_state, reason, request_id, correlation_id,
                               occurred_at, outcome, created_at)
        VALUES (gen_random_uuid(), NULL, 'SYSTEM', 'AUDIT_ACCESS_POLICY_UPDATED', 'ROLE', support_role_id,
                jsonb_build_object('role', 'ROLE_SUPPORT', 'auditReadGranted', previous_grant,
                                   'permissionDescription', previous_description),
                jsonb_build_object('role', 'ROLE_SUPPORT', 'auditReadGranted', true,
                                   'permissionDescription', policy_description),
                'Áp dụng UC-19: cấp AUDIT_READ cho SUPPORT',
                operation_id, operation_id, CURRENT_TIMESTAMP, 'SUCCESS', CURRENT_TIMESTAMP);
    END IF;
END $$;
