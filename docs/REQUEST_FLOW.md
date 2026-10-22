# Luồng request từ FE đến response

Cập nhật theo mã nguồn ngày 22/10/2026. Xem [báo cáo User, Permission và Audit Log](BAO_CAO_USER_PERMISSION_AUDIT_LOG.md) để có đầy đủ API, quy tắc, migration và cách thử trên Swagger.

## 1. Sơ đồ chung

~~~mermaid
flowchart TD
    A[FE hoặc Swagger gửi HTTP] --> B[Spring Security filter chain]
    B --> C[JwtAuthenticationFilter kiểm tra JWT]
    C --> D[CustomUserDetailsService tải trạng thái, role, permission từ DB]
    D --> E[Kiểm tra tokenVersion và tạo Authentication]
    E --> F[SecurityConfig kiểm tra URL]
    F --> G[Controller nhận và validate DTO]
    G --> H[Proxy kiểm tra PreAuthorize tại Service]
    H --> I[Service kiểm tra phạm vi, version, chính sách]
    I --> J[Repository và Hibernate đọc hoặc ghi DB]
    J --> K[AuditLogWriter ghi cùng transaction khi có thay đổi]
    K --> L[Commit transaction và trả DTO]
    L --> M[Controller bọc ApiResponse]
    M --> N[Spring MVC serialize JSON, trả status và body]
~~~

Sơ đồ mô tả request có JWT và nhánh cập nhật thành công. API đăng nhập công khai có luồng riêng ở mục 2. Với GET thông thường không có bước ghi thay đổi; riêng tra cứu audit có ghi nhật ký truy cập.

DatphongApplication.main() chỉ khởi động ứng dụng. SecurityConfig cấu hình filter chain khi khởi động; các filter mới xử lý từng request.

## 2. Đăng nhập

    POST /api/auth/login

~~~json
{
  "username": "tai-khoan-da-co",
  "password": "mat-khau-nguoi-dung-nhap"
}
~~~

1. JWT filter không thấy Bearer token thì cho request đi tiếp.
2. SecurityConfig cho phép URL login công khai.
3. AuthenticationController nhận LoginRequest và validate.
4. AuthenticationService gọi AuthenticationManager.authenticate().
5. DaoAuthenticationProvider gọi UserDetailsService.
6. CustomUserDetailsService tìm User, nạp role và permission qua RolePermissionRepository.
7. AccountPrincipal chứa danh tính, tokenVersion, trạng thái và authority. Đối tượng này chỉ dùng nội bộ.
8. Provider kiểm tra tài khoản hoạt động và đối chiếu BCrypt.
9. JwtTokenProvider tạo token có subject=username, roles, tokenVersion và thời hạn.
10. Controller trả ApiResponse chứa accessToken, tokenType.

Claim roles chỉ chứa role. Permission dùng quyết định truy cập được tải từ DB trên từng request, không lấy từ claim do client gửi.

Không log password, JWT hoặc secret 2FA. Với permitAll, một Bearer token sai vẫn có thể bị JWT filter từ chối trước khi vào Controller; khi login nên gửi request không có token cũ.

## 3. Đọc hồ sơ cá nhân

    GET /api/v1/users/me
    Authorization: Bearer <accessToken>

1. Filter xác minh JWT, đọc username.
2. Tải trạng thái/role/permission hiện tại từ DB.
3. Kiểm tra active=true, status=ACTIVE và tokenVersion khớp token.
4. Đặt Authentication vào SecurityContext.
5. URL yêu cầu đăng nhập.
6. UserProfileController gọi UserProfileService.getMyProfile().
7. Service yêu cầu isAuthenticated().
8. CurrentUser.require() xác định User từ Authentication.
9. UserViewMapper.profile() tạo UserProfileResponse, không trả trường quản trị hay secret.
10. Controller bọc ApiResponse; Spring MVC ghi JSON.

Không có targetUserId trong body hoặc URL /me. Bởi vậy người gọi không đổi ID để đọc hồ sơ của người khác qua API này.

## 4. Sửa hồ sơ cá nhân

    PATCH /api/v1/users/me

~~~json
{
  "version": 0,
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567"
}
~~~

Lấy version hiện tại từ GET /me trước khi gửi. Đây chỉ là ví dụ body.

1. Các bước xác thực giống mục 3.
2. Spring đọc ProfileUpdateRequest. StrictRequest từ chối trường không được phép.
3. Bean Validation kiểm tra version, độ dài, số điện thoại.
4. Service lấy User từ principal và so sánh version.
5. ChangeTracker chỉ cập nhật trường khác giá trị hiện tại.
6. Hibernate flush SQL, tăng version.
7. AuditLogWriter.write() ghi USER_PROFILE_UPDATED với dữ liệu đã che.
8. Transaction commit cả User và audit; lỗi một phần làm rollback cả hai.
9. Trả profile và version mới.

Không có thay đổi thật thì không tăng version và không ghi event thay đổi. PATCH đổi mật khẩu dùng endpoint riêng /api/v1/users/me/password, bắt buộc kiểm tra mật khẩu hiện tại và thu hồi JWT cũ bằng tokenVersion.

## 5. ADMIN tra danh sách User

    GET /api/v1/admin/users?page=0&size=20
    Authorization: Bearer <accessToken>

1. Filter tải lại User, role, permission và kiểm tra tokenVersion.
2. SecurityConfig yêu cầu ROLE_ADMIN cho nhóm /api/v1/admin/**.
3. AdminUserController.search() nhận bộ lọc và phân trang.
4. Proxy yêu cầu ROLE_ADMIN và USER_READ tại AdminUserService.search().
5. Repository truy vấn một Slice, sắp id giảm dần.
6. UserViewMapper.admin() che email/phone, bỏ dữ liệu xác thực.
7. Response tại data có page, size, hasNext, items.

Ví dụ cấu trúc response, không phải dữ liệu DB:

~~~json
{
  "success": true,
  "message": "Success",
  "data": {
    "page": 0,
    "size": 20,
    "hasNext": false,
    "items": []
  },
  "error": null,
  "timestamp": "2026-10-22T00:00:00Z"
}
~~~

Ví dụ FE với biến token đã nhận từ login:

~~~javascript
const response = await fetch("http://localhost:8080/api/v1/admin/users?page=0&size=20", {
  headers: { Authorization: "Bearer " + token }
});
if (!response.ok) throw new Error("HTTP " + response.status);
const body = await response.json();
const users = body.data.items;
~~~

Nếu dùng Axios, danh sách nằm tại response.data.data.items.

## 6. ADMIN cập nhật trạng thái hoặc role

- PATCH /api/v1/admin/users/{id}/status: ROLE_ADMIN + USER_MANAGE.
- PUT /api/v1/admin/users/{id}/roles: ROLE_ADMIN + USER_ROLE_MANAGE.
- Body có version và reason; mỗi thao tác có DTO riêng.
- Service lấy khóa quản trị, kiểm tra tự sửa quyền, phạm vi ủy quyền và admin cuối cùng.
- Cập nhật User, tăng tokenVersion và ghi audit cùng transaction.
- Tài khoản bị tác động phải đăng nhập lại. Mở khóa không làm JWT cũ hợp lệ trở lại.

Đổi permission của role có API PUT /api/v1/admin/roles/{id}/permissions. Quyền mới được nạp ở request kế tiếp. Chặn sửa permission của role đang gắn với chính người thao tác.

## 7. Chỉ ADMIN tra Audit Log

Hai URL cùng chính sách:

    GET /api/v1/admin/audit-logs
    GET /api/admin/audit-logs

Filter yêu cầu ROLE_ADMIN; Service yêu cầu thêm AUDIT_READ. SUPPORT có AUDIT_READ vẫn nhận 403 theo yêu cầu hiện tại.

Service validate bộ lọc, truy vấn Slice, tạo response và ghi AUDIT_LOG_SEARCHED. Không đếm request HTTP.

## 8. Nhánh lỗi

| Trường hợp | Nơi xử lý | HTTP |
|---|---|---|
| Không đăng nhập | Filter chain / AuthenticationEntryPoint | 401 |
| JWT sai/hết hạn, tokenVersion cũ, tài khoản không hoạt động | JWT filter | 401 |
| Sai mật khẩu login | DaoAuthenticationProvider / advice | 401 |
| Thiếu role/permission hoặc vượt phạm vi ủy quyền | Filter chain hoặc proxy Service | 403 |
| Trường thừa, dữ liệu sai, tham số sai | MVC / GlobalExceptionHandler | 400 |
| User hoặc API không tồn tại | Service hoặc MVC / advice | 404 |
| Version cũ | Service hoặc JPA / advice | 409 |
| Trùng username/email, role/permission không thể xóa | Service hoặc JPA / advice | 409 |

GlobalExceptionHandler là advice MVC duy nhất cho các lỗi trên. Lỗi từ filter dùng sendError nên body có thể khác ApiResponse.

ApiResponse là body; HTTP status do Controller/ResponseEntity/framework/handler quyết định.

## 9. Đặt breakpoint trong IntelliJ

| Vị trí | Cần quan sát |
|---|---|
| JwtAuthenticationFilter.doFilterInternal | URI, nhánh từ chối, tokenVersion |
| CustomUserDetailsService.loadUserByUsername | User, role và permission hiện tại |
| AuthenticationService.login | Kết quả authenticate; chỉ tạo token sau thành công |
| UserProfileController hoặc AdminUserController | DTO/bộ lọc đã validate |
| CurrentUser.require | Username lấy từ Authentication |
| UserProfileService/AdminUserService/AdminRoleService/PermissionService | Quyền đã qua proxy; version, phạm vi, thay đổi thực tế |
| RbacPolicy | Tự sửa quyền, ủy quyền, admin cuối cùng |
| AuditLogWriter.write | Actor, action, entity, before/after đã che |
| UserViewMapper | Trường nào được trả cho từng đối tượng |
| ApiResponse / GlobalExceptionHandler | Body trước khi Spring serialize |

Thiếu permission thì breakpoint trong thân phương thức Service bị chặn sẽ không được chạm tới. Spring Data tự tạo implementation của Repository; đặt breakpoint ở dòng gọi repository trong Service là đủ để học luồng.