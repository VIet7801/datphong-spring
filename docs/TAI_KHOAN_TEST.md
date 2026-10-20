# Tài khoản test

Bộ dữ liệu `uc-demo-v1`, ngày tạo: 2026-10-20 (Asia/Ho_Chi_Minh).

Đăng nhập bằng **username**, gọi `POST /api/auth/login`. Tất cả 10 tài khoản đều `ACTIVE`, `active=true`, không bật 2FA.

| Role | Username | Email | Mật khẩu trước khi hash |
|---|---|---|---|
| ADMIN | admin | admin@localhost | `Admin@2026!Demo` |
| CUSTOMER | customer01 | customer01@demo.vivu.test | `Customer01@2026!` |
| CUSTOMER | customer02 | customer02@demo.vivu.test | `Customer02@2026!` |
| CUSTOMER | customer03 | customer03@demo.vivu.test | `Customer03@2026!` |
| HOST | host01 | host01@demo.vivu.test | `Host01@2026!` |
| HOST | host02 | host02@demo.vivu.test | `Host02@2026!` |
| HOST | host03 | host03@demo.vivu.test | `Host03@2026!` |
| SUPPORT | support01 | support01@demo.vivu.test | `Support01@2026!` |
| SUPPORT | support02 | support02@demo.vivu.test | `Support02@2026!` |
| SUPPORT | support03 | support03@demo.vivu.test | `Support03@2026!` |

Mật khẩu DB nằm ở `users.password_hash`, dùng BCrypt cost 12 và salt riêng từng tài khoản. File này được ghi **trước khi hash**.

`GUEST` là khách chưa đăng nhập, không tạo user/role riêng. Enum `USER` cũ không phải role nghiệp vụ trong đặc tả.

Admin bootstrap có sẵn được tái dùng và đổi mật khẩu đúng một lần khi seed. Bản ghi trước thay đổi được lưu tại `target/seed-audit/bootstrap-admin-before.json`; không commit file backup này. Chạy lại script không đổi mật khẩu.

```json
{"username":"customer01","password":"Customer01@2026!"}
```

Đây là thông tin cho DB phát triển/test. Khi cần kiểm tra LOCKED/PENDING_VERIFICATION/IN_ACTIVE, dùng một tài khoản test rồi khôi phục trạng thái sau khi thử.
