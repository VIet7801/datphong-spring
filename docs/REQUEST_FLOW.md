# Một request đi qua hệ thống như thế nào

Cập nhật 23/10/2026. Xem [luồng A–Z và trade-off](BAO_CAO_USER_PERMISSION_AUDIT_LOG.md) để biết đầy đủ quy tắc, API và kiểm thử.

## 1. Request có JWT

~~~mermaid
flowchart TD
    A[Client gửi HTTP + Bearer JWT] --> B[RequestIdFilter: lấy/tạo X-Request-ID]
    B --> C[JwtAuthenticationFilter: chữ ký + hạn + userId]
    C --> D[DB: trạng thái, verified_at, tokenVersion, quyền hiện tại]
    D --> E[SecurityConfig: quyền theo URL]
    E --> F[Controller: JSON thành DTO + validation]
    F --> G[Service proxy: PreAuthorize + transaction]
    G --> H[Service: phạm vi, version, khóa, quy tắc nghiệp vụ]
    H --> I[Repository + Hibernate: SQL]
    I --> J[Audit cùng transaction khi có thay đổi]
    J --> K[Commit rồi trả DTO]
    K --> L[ApiResponse thành JSON]
~~~

JWT sai/hết hạn/đã thu hồi trả 401 ngay tại filter. Thiếu quyền trả 403. DTO sai trả 400. Version cũ trả 409. Lỗi MVC đi qua GlobalExceptionHandler; lỗi filter dùng JsonErrorWriter với cùng cấu trúc ApiResponse/ApiError và traceId.

Authentication chứa AccountPrincipal, không chứa entity User để client có thể sửa. Quyền luôn tải lại từ DB; claim roles chỉ hỗ trợ hiển thị/chẩn đoán.

## 2. Auth công khai

POST `/api/v1/auth/{register,verify,resend-verification,login,refresh,logout}` không cần access token. JWT filter bỏ qua đúng các endpoint này, vì access token hết hạn không được chặn refresh hoặc login.

~~~text
register:
DTO → rate limit → transaction(User + CUSTOMER + token hash + MailEvent + audit)
→ commit → 202 → worker gửi email ngoài transaction

verify:
DTO → rate limit → khóa User → mã mới nhất → kiểm hạn/attempt/hash
→ dùng mã + verified_at + ACTIVE nếu đang pending + thu hồi phiên + audit → commit

login:
DTO → rate limit → AuthenticationManager/BCrypt → khóa User + kiểm lại trạng thái/tokenVersion
→ lưu hash refresh + audit → cấp JWT/refresh → commit → JSON

refresh:
DTO → rate limit → hash → tìm userId → khóa User → kiểm token
→ xoay refresh cùng hạn family + audit → commit → cặp token mới

logout:
DTO → rate limit → tìm và khóa User → tokenVersion++ + thu hồi mọi refresh + audit → 204
~~~

`/api/auth` được giữ làm alias. Login dùng email hoặc username cũ, không gửi cả hai.

## 3. Ví dụ admin khóa tài khoản

~~~text
PATCH /api/v1/admin/users/34/status
Authorization: Bearer <adminJWT>
{"version":0,"status":"LOCKED","reason":"Kiểm tra tài khoản"}
~~~

1. Filter xác minh admin và nạp quyền mới nhất.
2. URL yêu cầu ROLE_ADMIN; service yêu cầu thêm USER_MANAGE.
3. Khóa ROLE_ADMIN chung, rồi khóa User 34.
4. Kiểm version, không tự khóa, phạm vi được quản lý và admin cuối.
5. Đổi status/active, tăng tokenVersion và thu hồi refresh.
6. Flush + audit trong cùng transaction; tính cảnh báo tài sản/booking; commit.
7. JWT/refresh cũ của User 34 bị từ chối. Mở khóa sau đó không phục hồi token cũ.

`version` phục vụ chống ghi đè dữ liệu; `tokenVersion` phục vụ thu hồi phiên. Đây là hai việc khác nhau. Refresh thông thường không tăng tokenVersion, còn thao tác thu hồi thì tăng.

## 4. Ranh giới trách nhiệm

| Thành phần | Việc cần làm |
|---|---|
| Controller/DTO | Hợp đồng HTTP và kiểm tra đầu vào |
| Security filter/config | Danh tính, phiên JWT, quyền URL |
| Service/PreAuthorize | Quyền thao tác, phạm vi, chuyển trạng thái, transaction |
| Repository/Hibernate | Truy vấn, khóa hàng, ánh xạ DB |
| AuditLogWriter | Nhật ký đã che dữ liệu, cùng transaction |
| VerificationMailService | Hàng đợi email, SMTP, retry; không giữ transaction lúc gửi |
| GlobalExceptionHandler | Mã HTTP và JSON lỗi thống nhất |

Không gọi repository trực tiếp từ client. Không tin userId/role/status gửi kèm JSON khi thao tác `/me`.

## 5. Hoàn tiền

Hủy booking và thanh toán đến muộn cùng gọi `RefundService.request` để tạo yêu cầu. Worker gọi `claim` và commit trước khi gửi sang Fake Bank, rồi gọi `complete` trong transaction mới để ghi kết quả. Cùng key chỉ hoàn một lần, timeout giữ trạng thái chưa rõ kết quả để thử lại bằng key cũ. Xem [luồng hoàn tiền và trade-off](LUONG_HOAN_TIEN.md).
