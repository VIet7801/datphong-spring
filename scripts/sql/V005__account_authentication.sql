-- UC-01/02/03/20. Chạy cùng V003/V004 trong cap-nhat-rbac.cmd; không đổi trạng thái tài khoản cũ.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized ON users(lower(btrim(email)));
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user_active ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_family_active ON refresh_tokens(family_id) WHERE revoked_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_verification_tokens_latest ON verification_tokens(user_id, purpose, id DESC);

-- Chỉ tạo mẫu mới; chạy lại không sửa mẫu/phiên bản đang được quản trị.
WITH inserted AS (
    INSERT INTO email_templates(template_code, locale, status, active_version, version, created_at, updated_at)
    VALUES ('EMAIL_VERIFICATION', 'vi-VN', 'ACTIVE', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (template_code, locale) DO NOTHING
    RETURNING id
)
INSERT INTO email_template_versions(template_id, version_no, subject, html_body, text_body,
                                    allowed_variables, required_variables, published_at, created_at)
SELECT id, 1, 'Xác minh tài khoản Đặt phòng',
       '<p>Mã xác minh: <strong>{{token}}</strong></p><p>Hết hạn sau {{expiresMinutes}} phút.</p>',
       'Mã xác minh tài khoản: {{token}}. Mã hết hạn sau {{expiresMinutes}} phút và chỉ dùng một lần. Nếu không yêu cầu đăng ký, hãy bỏ qua email này.',
       '{"token":"string","expiresMinutes":"number"}'::jsonb,
       '{"token":"string","expiresMinutes":"number"}'::jsonb,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM inserted;
