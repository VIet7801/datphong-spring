# Báo cáo tách hồ sơ cá nhân, quản trị User và hoàn thiện Permission

Ngày thực hiện: 22/10/2026.

Bản hiện hành đã triển khai quyền tra cứu audit cho ADMIN/SUPPORT có AUDIT_READ theo UC-19. Mục 11 ghi nhận phần code, migration và kiểm thử bổ sung cho SUPPORT; mục 10 giữ kết quả của lần triển khai User/Permission ban đầu.

## 1. Phạm vi và quyết định áp dụng

Đã sửa trực tiếp mã nguồn trong dự án có file pom.xml, không chỉ cung cấp ví dụ.

Căn cứ tài liệu Dac_ta_he_thong_dat_phong_spring.pdf:

- UC-03, trang 6–7: danh tính của /me lấy từ principal; DTO theo use case; chặn sửa trường quản trị; kiểm tra phạm vi dữ liệu, phiên bản và audit.
- UC-20, trang 21: quản trị tài khoản/quyền có permission cụ thể; cấp quyền tách API, DTO, audit; bảo vệ lịch sử và xử lý token khi khóa tài khoản.
- API contract gợi ý, trang 44–45: nhóm /api/v1/users/me và /api/v1/admin/....
- UC-19, trang 20: tra cứu có phân quyền, phân trang và ghi nhận truy cập audit nhạy cảm.

**Chính sách hiện hành: ADMIN hoặc SUPPORT có AUDIT_READ được tra cứu Audit Log theo UC-19.** ADMIN/SUPPORT thiếu AUDIT_READ và các vai trò khác bị từ chối. Các API quản trị User/Role/Permission vẫn yêu cầu ADMIN cùng permission tương ứng.

Phần Permission được triển khai cho hồ sơ, quản trị User, role, permission và Audit Log trong báo cáo này. Các phân hệ booking, payment, property... cần tiếp tục gắn permission và data scope theo use case riêng; sự tồn tại của một permission trong DB chưa tự bảo vệ các API khác.

## 2. Những thay đổi chính

| Trước | Sau |
|---|---|
| UserController dùng chung /users cho hồ sơ và quản trị | UserProfileController và AdminUserController riêng |
| UsersResquest chứa cả password, status, active, roleId | DTO riêng cho hồ sơ, đổi mật khẩu, tạo User, trạng thái, role |
| Nạp role thành GrantedAuthority | Nạp role và permission qua user_roles → role_permissions |
| ADMIN mặc nhiên gọi các thao tác User | Kiểm tra ROLE_ADMIN và permission của từng thao tác |
| Khóa tài khoản chỉ chặn khi đang khóa | Tăng tokenVersion; mở khóa không làm sống lại JWT cũ |
| RoleService trả entity và cho xóa trực tiếp | AdminRoleService trả DTO, kiểm tra sử dụng/ủy quyền, version, reason và audit |
| Chưa có API Permission hoạt động đầy đủ | API danh mục Permission và API gắn/thu hồi permission của role |
| Audit Log chỉ có nhánh cập nhật User minh họa | Ghi riêng các hành động hồ sơ, trạng thái, role, permission, đổi mật khẩu và tra cứu audit |
| Hai exception advice xử lý trùng | Một GlobalExceptionHandler thống nhất |

API xóa cứng User cũ đã được gỡ. Quản trị dùng trạng thái IN_ACTIVE/LOCKED để giữ liên kết booking, property và lịch sử.

## 3. API hiện có sau thay đổi

### 3.1. Hồ sơ cá nhân

| Method | URL | Điều kiện |
|---|---|---|
| GET | /api/v1/users/me | Đăng nhập, tài khoản hoạt động |
| PATCH | /api/v1/users/me | Chỉ sửa hồ sơ của principal |
| PATCH | /api/v1/users/me/password | Mật khẩu hiện tại đúng, version đúng |

Response cá nhân có id, username, fullName, email, phone, gender, avatar, version. Không trả role, trạng thái quản trị, password/hash, tokenVersion, secret 2FA.

PATCH hồ sơ chỉ nhận version, fullName, phone, gender, avatar. Các trường tùy chọn bị bỏ qua hoặc bằng null được giữ nguyên; API hiện chưa hỗ trợ dùng null để xóa phone/avatar. Đổi email/username cần quy trình riêng và không được nhận tại API này.

StrictRequest dùng JsonAnySetter để từ chối trường thừa. Ví dụ gửi roles, roleIds, roleId, status, active, password, id, ownerId... vào PATCH hồ sơ sẽ trả 400, không âm thầm bỏ qua.

### 3.2. Quản trị User

Mọi API dưới đây yêu cầu ROLE_ADMIN và permission tương ứng:

| Method | URL | Permission | Request |
|---|---|---|---|
| GET | /api/v1/admin/users | USER_READ | keyword, status, page, size |
| GET | /api/v1/admin/users/{id} | USER_READ | id |
| POST | /api/v1/admin/users | USER_MANAGE | AdminUserCreateRequest |
| PATCH | /api/v1/admin/users/{id}/profile | USER_MANAGE | AdminProfileUpdateRequest |
| PATCH | /api/v1/admin/users/{id}/status | USER_MANAGE | UserStatusUpdateRequest |
| PUT | /api/v1/admin/users/{id}/roles | USER_ROLE_MANAGE | UserRolesUpdateRequest |

- Danh sách có page, size, hasNext, items; page bắt đầu từ 0; size từ 1 đến 100.
- Tìm keyword theo username hoặc fullName; có thể lọc status.
- DTO quản trị che email thành a***@example.com, phone thành ***567.
- Tạo User: mật khẩu BCrypt, mặc định ROLE_CUSTOMER và IN_ACTIVE/active=false. Body tạo không nhận role/status; quản trị kích hoạt hoặc cấp role qua API riêng.
- Mật khẩu tạo mới/đổi: 12–72 ký tự và tối đa 72 byte UTF-8 cho BCrypt.
- Thao tác quản trị thay đổi dữ liệu phải có reason; cập nhật phải gửi version hiện tại.
- PUT roles thay thế toàn bộ tập role, không phải cộng thêm. Lấy ID thật từ API danh mục, không suy luận ID từ tên role.
- ADMIN không tự thay đổi trạng thái/role của chính mình.
- Không chuyển sang PENDING_VERIFICATION qua API quản trị trạng thái; trạng thái đó thuộc luồng đăng ký/xác minh.

### 3.3. Role và Permission

| Method | URL | Permission |
|---|---|---|
| GET | /api/v1/admin/roles | ROLE_READ |
| GET | /api/v1/admin/roles/{id} | ROLE_READ |
| POST | /api/v1/admin/roles | ROLE_MANAGE |
| PATCH | /api/v1/admin/roles/{id} | ROLE_MANAGE |
| PUT | /api/v1/admin/roles/{id}/permissions | ROLE_MANAGE |
| DELETE | /api/v1/admin/roles/{id} | ROLE_MANAGE |
| GET | /api/v1/admin/permissions | PERMISSION_READ |
| GET | /api/v1/admin/permissions/{id} | PERMISSION_READ |
| POST | /api/v1/admin/permissions | PERMISSION_MANAGE |
| PATCH | /api/v1/admin/permissions/{id} | PERMISSION_MANAGE |
| DELETE | /api/v1/admin/permissions/{id} | PERMISSION_MANAGE |

Các API này cũng yêu cầu ROLE_ADMIN. Danh sách được phân trang. POST trả 201; DELETE thành công trả 204.

- Role mới dùng mã ROLE_..., ví dụ ROLE_REVIEWER.
- Mã role không được trùng mã permission, kể cả ROLE_READ/ROLE_MANAGE của catalog.
- Permission mới dùng mã viết hoa, ví dụ USER_READ; không được bắt đầu bằng ROLE_.
- Code của role/permission không được đổi qua PATCH; chỉ sửa name, description.
- DELETE nhận JSON version và reason.
- Role hệ thống hoặc role đang gắn với User không được xóa.
- Permission hệ thống hoặc permission đang gắn với role không được xóa.
- PUT permissions thay thế toàn bộ tập permission; [] có nghĩa thu hồi tất cả permission của role đủ điều kiện.
- Không sửa tập permission của role đang gắn với chính người thực hiện.
- Chỉ được cấp/thu hồi những permission thuộc phạm vi bản thân đang có; kiểm tra cả phần thêm và phần bỏ.
- Khi cấp/thu hồi role cho User, cũng kiểm tra permission trong các role thay đổi.

Danh mục permission mới không tự tạo chức năng trong chương trình. Ví dụ tạo FEATURE_READ chưa tự tạo API đọc feature. Muốn đưa một quyền nghiệp vụ mới vào vận hành cần thêm kiểm tra trong Service và migration cấp quyền gốc cho role quản trị được ủy quyền; API không tự cấp quyền mới cho chính người tạo catalog.

### 3.4. Audit Log

URL chính:

    GET /api/v1/admin/audit-logs

Giữ URL cũ làm alias:

    GET /api/admin/audit-logs

Hai URL dùng cùng Controller/Service, cùng yêu cầu ROLE_ADMIN hoặc ROLE_SUPPORT và đồng thời có AUDIT_READ. Chỉ GET của hai URL audit được mở cho SUPPORT; các URL quản trị khác vẫn yêu cầu ADMIN.

Giữ bộ lọc hiện tại: actorId, entityType, entityId, action, outcome, from, to, page, size.

Trong mã hiện tại, from và to vẫn bắt buộc; khoảng tìm kiếm phải lớn hơn 0 và tối đa 31 ngày. Nếu có entityId thì phải có entityType. Response vẫn dùng Slice, không có tổng số request hoặc thống kê GET/POST/PUT.

Swagger đã dùng ParameterObject để hiển thị các ô bộ lọc. Mỗi lần tra cứu thành công ghi AUDIT_LOG_SEARCHED, chỉ lưu bộ lọc và phân trang, không sao chép nội dung các log đã đọc.

## 4. Permission đi vào Spring Security như thế nào?

Quan hệ dữ liệu:

    users → user_roles → roles → role_permissions → permissions

Luồng nạp:

1. CustomUserDetailsService tải User theo username đã xác thực.
2. Lấy role của User, ví dụ ROLE_ADMIN.
3. RolePermissionRepository truy vấn permission của tất cả role thuộc User.
4. Chuyển role và permission thành GrantedAuthority, loại trùng.
5. Tạo AccountPrincipal chứa userId, tokenVersion và các authority.
6. Service dùng PreAuthorize kiểm tra các authority cần thiết.

Ví dụ authority của ADMIN:

    ROLE_ADMIN
    USER_READ
    USER_MANAGE
    USER_ROLE_MANAGE
    ROLE_READ
    ROLE_MANAGE
    PERMISSION_READ
    PERMISSION_MANAGE
    AUDIT_READ
    ... những permission đã có từ trước

Không có cơ chế ADMIN tự bỏ qua permission. ROLE_ADMIN chỉ thỏa điều kiện vai trò; Service vẫn kiểm tra permission.

Để chống một dòng permission sai giả dạng role, phần nạp từ chối permission bắt đầu bằng ROLE_, trừ hai mã catalog đã quy ước ROLE_READ và ROLE_MANAGE. Hai mã này chỉ là permission, được loại khỏi claim roles và không được dùng làm mã role mới. Một permission có code ROLE_ADMIN trong dữ liệu cũ không biến SUPPORT thành ADMIN.

## 5. Luồng xử lý một request

### 5.1. Luồng chung

~~~mermaid
flowchart TD
    A[Swagger hoặc FE gửi Bearer JWT] --> B[JwtAuthenticationFilter kiểm tra chữ ký và hạn]
    B --> C[Tải User, trạng thái, role và permission từ DB]
    C --> D[So sánh tokenVersion]
    D --> E[SecurityConfig kiểm tra quyền theo URL]
    E --> F[Controller nhận DTO và validate]
    F --> G[Proxy kiểm tra PreAuthorize tại Service]
    G --> H[Service kiểm tra principal, phạm vi, version, chính sách]
    H --> I[Cập nhật nghiệp vụ trong transaction]
    I --> J[AuditLogWriter lưu event trong cùng transaction]
    J --> K[Commit và trả response DTO]
~~~

Lỗi xác thực trả 401; đủ danh tính nhưng thiếu quyền trả 403. Validation trả 400; không tồn tại trả 404; version cũ/trùng dữ liệu/vi phạm điều kiện xóa trả 409.

### 5.2. Người dùng sửa hồ sơ của mình

1. Gọi GET /api/v1/users/me và lấy version.
2. Gửi PATCH chỉ với các trường hồ sơ cần sửa và version đó.
3. CurrentUser xác định User từ Authentication; không dùng id trong body.
4. Service so sánh version và các giá trị thực tế.
5. ChangeTracker cập nhật các trường thay đổi và tạo before/after đã che dữ liệu.
6. Flush để cập nhật version trước khi tạo response.
7. AuditLogWriter ghi USER_PROFILE_UPDATED trong transaction đang có.
8. Commit; response chứa version mới.

Nếu body chỉ lặp lại giá trị cũ, không tăng version và không tạo audit thay đổi giả. Nếu audit ghi thất bại, cập nhật hồ sơ cũng rollback, kể cả khi Hibernate đã flush SQL cập nhật User.

### 5.3. ADMIN khóa/mở khóa User

1. Kiểm tra ROLE_ADMIN + USER_MANAGE.
2. Lấy khóa ghi trên dòng ROLE_ADMIN để tuần tự hóa các thao tác quản trị nhạy cảm.
3. Tải User, kiểm tra version, không tự khóa mình và bảo vệ ADMIN hoạt động cuối cùng.
4. Đổi status và đồng bộ active.
5. Tăng users.token_version.
6. Ghi USER_STATUS_CHANGED với actor, User đích, trạng thái trước/sau, reason.
7. Commit.
8. Request tiếp theo bằng JWT cũ bị từ chối. Sau mở khóa, JWT cũ vẫn bị từ chối; phải đăng nhập lấy token mới.

### 5.4. ADMIN cấp/thu hồi role và permission

1. Lấy danh mục để xác định ID thực tế và version của User/role đích.
2. Gọi PUT riêng cho User roles hoặc role permissions.
3. Kiểm tra permission của thao tác và phạm vi được ủy quyền.
4. Chặn tự sửa role của mình hoặc tập permission của role mình đang giữ.
5. Tính phần thay đổi; dữ liệu không đổi thì không tạo event thay đổi.
6. Cập nhật liên kết trong transaction; tăng version của User/role đích.
7. Ghi USER_ROLES_CHANGED hoặc ROLE_PERMISSIONS_CHANGED.
8. Đổi role của User làm tokenVersion tăng, yêu cầu đăng nhập lại.
9. Đổi permission của role được áp dụng ở request tiếp theo vì JWT filter nạp quyền hiện tại từ DB.

### 5.5. ADMIN/SUPPORT tra cứu Audit Log

1. Filter xác thực JWT; matcher GET riêng cho hai URL audit cho phép ROLE_ADMIN hoặc ROLE_SUPPORT trước quy tắc /admin/**.
2. Service yêu cầu thêm AUDIT_READ.
3. Validate bộ lọc và khoảng thời gian.
4. Repository truy vấn trang log, sắp occurredAt giảm dần rồi id giảm dần.
5. Mapper tạo AuditLogResponse.
6. Writer lưu event AUDIT_LOG_SEARCHED trong cùng transaction.
7. Commit rồi trả danh sách. Nếu lưu nhật ký truy cập thất bại, request không trả một lần truy cập thành công thiếu dấu vết.

## 6. Transaction, version và dữ liệu nhạy cảm

- Service thay đổi dữ liệu có Transactional; writer có Propagation.MANDATORY.
- Writer không tự mở transaction riêng. Lỗi ghi audit làm rollback cả thay đổi nghiệp vụ.
- Không bắt rồi bỏ qua lỗi ghi audit.
- User/Role/Permission dùng trường Version hiện có.
- Cập nhật liên kết permission cũng đánh dấu role thay đổi để version tăng.
- Lý do thao tác có giới hạn 500 ký tự.
- fullName, email, phone, avatar trong snapshot thay đổi được che.
- Không ghi password, password hash, token hoặc secret 2FA vào các event mới.
- Đổi mật khẩu chỉ ghi passwordChanged=true.
- RequestId/correlationId hiện được tạo bằng UUID cho từng event; chưa có filter truyền traceId xuyên nhiều dịch vụ.
- Chưa ghi ATTEMPT/FAILED cho mọi yêu cầu bị từ chối. Các event SUCCESS chỉ tồn tại khi transaction commit.

## 7. Dữ liệu RBAC và cách cập nhật database

Migration bổ sung:

    scripts/sql/V003__user_rbac_catalog.sql
    scripts/sql/V004__support_audit_read.sql

Công cụ chạy:

    scripts/java/RbacCatalogTool.java
    scripts/update-user-rbac.ps1
    cap-nhat-rbac.cmd

Chạy tại thư mục chứa pom.xml:

~~~powershell
.\cap-nhat-rbac.cmd
.\cap-nhat-rbac.cmd -VerifyOnly
~~~

Nếu chưa cấu hình JDK:

~~~powershell
.\cap-nhat-rbac.cmd -JavaHome "C:\Program Files\Java\jdk-17"
~~~

Công cụ dùng cấu hình SPRING_DATASOURCE_* hoặc mặc định từ application.yaml, không in mật khẩu ra log. Cần chạy sau khi đã có cấu trúc bảng; công cụ này không thay thế script khởi tạo schema.

File .cmd chạy PowerShell với ExecutionPolicy Bypass chỉ cho tiến trình công cụ, theo cách các script Windows khác trong dự án đang dùng; không đổi execution policy của máy.

Migration:

1. Bổ sung index unique trên username để chặn tạo trùng kể cả khi request chạy đồng thời.
2. Bảo đảm assigned_at của user_roles có default CURRENT_TIMESTAMP khi JPA chèn liên kết.
3. Bổ sung 4 role chuẩn nếu còn thiếu.
4. Bổ sung 8 permission của nhóm API này nếu còn thiếu.
5. Gắn 8 permission này cho ROLE_ADMIN, giữ các grant đã có.
6. Ghi một audit SYSTEM khi thực sự bổ sung catalog/grant.
7. Chạy lại không tạo trùng, không thêm audit khi catalog/grant đã đủ.

cap-nhat-rbac.cmd hiện chạy V003 rồi V004 trong cùng transaction. V004 bổ sung AUDIT_READ cho ROLE_SUPPORT nếu thiếu, cập nhật mô tả permission theo UC-19 và ghi AUDIT_ACCESS_POLICY_UPDATED khi có thay đổi thực tế. Nếu thêm liên kết quyền mới, version của role SUPPORT cũng tăng. V003 đã áp dụng được giữ nguyên.

Không xóa User, đổi mật khẩu, đổi User role hoặc xóa permission cũ. Nếu dữ liệu cũ có username trùng, tạo unique index sẽ thất bại và transaction rollback; công cụ không tự chọn xóa tài khoản nào.

Lưu ý: chạy lại migration sẽ bổ sung lại các permission nền tảng còn thiếu của ROLE_ADMIN và AUDIT_READ của ROLE_SUPPORT. Việc này là thao tác bảo trì có chủ đích, không tự chạy mỗi lần backend khởi động. VerifyOnly kiểm tra cả hai role, in MISSING_REQUIRED và SUPPORT_MISSING_REQUIRED.

Kết quả áp dụng và kiểm chứng thực tế được ghi ở mục 10.

## 8. Kiểm thử bằng Swagger

1. Khởi động lại backend bằng code mới.
2. Mở http://localhost:8080/swagger-ui/index.html.
3. Gọi POST /api/auth/login bằng tài khoản đã có. Có thể xem tài khoản dữ liệu mẫu tại TAI_KHOAN_TEST.md.
4. Copy accessToken vào Authorize, không thêm chữ Bearer.
5. Với CUSTOMER, gọi GET /api/v1/users/me để lấy version.
6. Gọi PATCH /api/v1/users/me:

~~~json
{
  "version": 0,
  "fullName": "Nguyễn Văn A",
  "phone": "0901234567"
}
~~~

Thay 0 bằng version thực tế. Sau thành công dùng version mới cho lần cập nhật tiếp theo.

7. Thử thêm "status":"ACTIVE" hoặc "roleIds":[1] vào body trên: phải nhận 400.
8. Đổi sang token ADMIN; gọi GET /api/v1/admin/users.
9. Chọn id thật trong danh sách, gọi GET /api/v1/admin/users/{id} và lấy version.
10. Gọi PATCH /api/v1/admin/users/{id}/status:

~~~json
{
  "version": 0,
  "status": "LOCKED",
  "reason": "Tạm khóa theo yêu cầu hỗ trợ"
}
~~~

11. Gọi GET /api/v1/admin/roles và /api/v1/admin/permissions để lấy ID, code, version.
12. Muốn thử gắn permission, tạo role thử chưa gắn với mình; dùng PUT /roles/{id}/permissions. Gửi đầy đủ tập ID mong muốn cùng version và reason.
13. Tra audit bằng entityType=USER, entityId là User vừa sửa và khoảng from/to bao phủ thời điểm thao tác.
14. Đổi sang token SUPPORT có AUDIT_READ và gọi lại: nhận 200 và AUDIT_LOG_SEARCHED ghi đúng actor SUPPORT. SUPPORT thiếu AUDIT_READ vẫn nhận 403; SUPPORT gọi API quản trị User/Role/Permission cũng nhận 403.

JWT phát hành trước thay đổi này không có tokenVersion sẽ bị từ chối. Hãy đăng nhập lại sau khi khởi động code mới. Khi đổi mật khẩu, trạng thái hoặc role, tài khoản bị tác động cũng cần đăng nhập lại.

## 9. Danh sách file chính để đọc code

| File/nhóm | Vai trò |
|---|---|
| controller/UserProfileController.java | API cá nhân |
| controller/AdminUserController.java | API quản trị User |
| controller/AdminRoleController.java | API role và role-permission |
| controller/AdminPermissionController.java | API catalog Permission |
| controller/AuditLogController.java | API audit chính và alias |
| service/UserProfileService.java | Hồ sơ từ principal, đổi mật khẩu |
| service/AdminUserService.java | Danh sách, tạo, hồ sơ, trạng thái, role của User |
| service/AdminRoleService.java | Role, liên kết permission, kiểm tra sử dụng |
| service/PermissionService.java | Catalog Permission |
| service/support/RbacPolicy.java | Ủy quyền, tự sửa quyền, admin cuối cùng, khóa đồng thời |
| service/support/ChangeTracker.java | Version, phát hiện thay đổi và che dữ liệu |
| service/Impl/CustomUserDetailsService.java | Nạp role và permission |
| security/AccountPrincipal.java | userId, tokenVersion, authority nội bộ |
| security/CurrentUser.java | Tải User từ Authentication |
| security/JwtTokenProvider.java | Phát hành JWT có tokenVersion |
| security/JwtAuthenticationFilter.java | Nạp trạng thái/quyền mới và từ chối token đã thu hồi |
| config/SecurityConfig.java | Bảo vệ nhóm URL quản trị và bật method security |
| service/AuditLogWriter.java | Lưu audit trong transaction của nghiệp vụ |
| service/Impl/AuditLogServiceImpl.java | ADMIN/SUPPORT có AUDIT_READ; audit việc tra cứu |
| mapper/UserViewMapper.java | Response riêng cho cá nhân và quản trị |
| dto/request/StrictRequest.java | Chặn trường ngoài DTO |
| exception/GlobalExceptionHandler.java | Response lỗi thống nhất |
| security/UserRbacIntegrationTest.java trong src/test | Kiểm tra thật với PostgreSQL/JPA/JWT/MockMvc |

Các file UserController, UserService, UserServiceImpl, UserMapper và RoleService/RoleServiceImpl cũ đã được thay bằng các lớp theo use case trên. GlobalExeptionHander bị gỡ vì xử lý trùng với GlobalExceptionHandler.

DTO cũ UsersResquest/UsersResponse và một số mapper/DTO bài học còn lại không còn được các API User mới sử dụng.

Các URL /users/findAll, /users/detail/{id}, /users/update/{id}, /users/add, /users/delete/{id} không còn hoạt động; FE/Swagger cần chuyển sang URL mới. Riêng URL audit cũ được giữ làm alias có cùng bảo vệ.

## 10. Kết quả lần triển khai User/Permission ban đầu

Mục này là kết quả trước khi mở quyền SUPPORT. Kết quả cập nhật theo UC-19 được ghi tại mục 11.

### 10.1. Database phát triển đã cập nhật

Đã chạy cap-nhat-rbac.cmd trên database cục bộ được cấu hình của dự án:

| Dữ liệu | Trước | Sau |
|---|---:|---:|
| User | 10 | 10 |
| Role | 4 | 4 |
| Permission | 20 | 25 |
| Role-permission | 37 | 42 |
| Audit Log | 391 | 392 |

5 permission bổ sung: USER_READ, USER_ROLE_MANAGE, ROLE_READ, PERMISSION_READ, PERMISSION_MANAGE. Các permission USER_MANAGE, ROLE_MANAGE, AUDIT_READ đã có và được tái sử dụng.

ROLE_ADMIN đã có đủ 8 permission của nhóm API mới. Event tăng thêm là RBAC_CATALOG_INITIALIZED với actorType SYSTEM. Tài khoản, mật khẩu và liên kết User-role hiện có được giữ nguyên.

Đã chạy lại migration lần thứ hai: số dòng cả 5 bảng trên giữ nguyên và không sinh thêm audit. Đã chạy -VerifyOnly: MISSING_REQUIRED=[] và RBAC_VERIFIED.

Log bằng chứng nằm trong target/rbac-audit/catalog-apply.log, catalog-second-apply.log và catalog-verify.log.

### 10.2. Kiểm thử

Lệnh kiểm tra:

~~~powershell
$env:DATPHONG_RBAC_INTEGRATION = 'true'
.\mvnw.cmd test
~~~

UserRbacIntegrationTest dùng PostgreSQL thật với schema rbac_test_<UUID>, JPA, transaction, JWT filter, method security và MockMvc. Dữ liệu test được tạo trong schema riêng và xóa schema sau kiểm thử.

Những hành vi được kiểm tra:

- /me chọn đúng principal, không lộ trường quản trị hoặc secret.
- Từ chối trường quản trị gửi kèm với dữ liệu đúng kiểu, không chỉ bắt lỗi kiểu JSON.
- Cập nhật đúng User, che dữ liệu audit, bỏ qua thay đổi giả, phát hiện version cũ.
- Lỗi insert audit rollback cả cập nhật hồ sơ đã flush.
- Từ chối gọi Service trực tiếp khi thiếu permission.
- Nạp lại permission hiện tại cho request bằng JWT đã phát hành.
- Danh sách User có phân trang, lọc, che dữ liệu và giới hạn size.
- Tạo User lưu BCrypt, trạng thái IN_ACTIVE và role mặc định.
- Khóa/mở khóa, đổi role và đổi mật khẩu thu hồi token cũ.
- Chặn tự sửa quyền, bảo vệ ADMIN cuối cùng và giới hạn ủy quyền.
- CRUD role/permission, liên kết permission, version, audit và điều kiện xóa.
- Không nhầm ROLE_MANAGE thành vai trò và chặn mã role trùng permission.
- Permission sai ROLE_ADMIN trong dữ liệu cũ không nâng quyền SUPPORT.
- Trong bản ban đầu, ca kiểm thử audit kỳ vọng chỉ ADMIN có AUDIT_READ được truy cập; kỳ vọng này đã được đổi trong bản SUPPORT ở mục 11.
- Migration idempotent và API User cũ không còn được công bố.

Kết quả chạy toàn bộ bộ kiểm thử:

| Lớp kiểm thử | Tổng | Đạt | Bỏ qua | Lỗi/Thất bại |
|---|---:|---:|---:|---:|
| DatphongApplicationTests | 1 | 1 | 0 | 0 |
| EntitySchemaTest | 5 | 2 | 3 | 0 |
| UserAccountStatusTest | 17 | 17 | 0 | 0 |
| UserRbacIntegrationTest | 31 | 31 | 0 | 0 |
| Tổng | 54 | 51 | 3 | 0 |

Maven kết thúc BUILD SUCCESS. 3 ca bỏ qua là các kiểm tra schema có sẵn yêu cầu DATPHONG_SCHEMA_VALIDATE=true; biến này không được bật trong lần chạy. Toàn bộ 31 ca RBAC mới đã thực sự chạy với PostgreSQL.

DatphongApplicationTests đã khởi tạo thành công cấu hình Spring Boot thực của dự án. Kiểm thử mới đi xuyên JWT filter, URL authorization, validation, method security, Service, JPA và transaction.

Bằng chứng: target/rbac-audit/final-test.log và các báo cáo XML/TXT trong target/surefire-reports. Lỗi constraint reject_profile_test xuất hiện trong log là tình huống cố ý của ca kiểm tra rollback audit; ca đó đã đạt.

Sau khi lấy code mới, khởi động lại backend và đăng nhập lại trên Swagger. Không cần chạy lại migration trên database cục bộ này; các máy/database khác cần chạy cap-nhat-rbac.cmd một lần.

## 11. Đã triển khai quyền tra cứu Audit Log cho SUPPORT theo UC-19

### 11.1. Đối chiếu yêu cầu và đặc tả

UC-19, trang 20 của đặc tả, cho phép SUPPORT/ADMIN tra cứu audit khi có quyền phù hợp. Yêu cầu trước đây “chỉ dành cho Admin” đã làm phạm vi truy cập hẹp hơn UC-19.

Lần cập nhật này đã sửa trực tiếp mã nguồn và áp dụng migration trên DB để triển khai chính sách UC-19.

Quy tắc làm việc từ lần rà soát này: nếu một yêu cầu khác đặc tả, phải nêu rõ UC/trang, khác ở đâu và ảnh hưởng trước khi triển khai. Quy tắc đã được lưu trong AGENTS.md của dự án.

### 11.2. Kết quả kiểm tra DB hiện tại

Đã kiểm tra và chạy cap-nhat-rbac.cmd áp dụng V003 → V004 trên DB cục bộ:

- Role ROLE_SUPPORT có id=11, version=0.
- Role này đã được gán AUDIT_READ, id permission=54.
- Các permission khác của role: SUPPORT_READ, COMPLAINT_MANAGE, REFUND_RECONCILE, MAIL_OPERATE.

Liên kết AUDIT_READ của SUPPORT đã tồn tại từ dữ liệu mẫu, nên V004 giữ nguyên liên kết này và các quyền còn lại, cập nhật mô tả permission theo UC-19, đồng thời ghi AUDIT_ACCESS_POLICY_UPDATED. Hai lớp chặn SUPPORT trong code đã được sửa.

Kết quả thực tế: users=10, roles=4, permissions=25, role_permissions=42 giữ nguyên; audit_logs tăng từ 392 lên 393. Không đổi User, mật khẩu hoặc role của User. V004 chỉ tăng version của role nếu thực sự thêm liên kết quyền mới.

Đã chạy lại migration lần hai trên DB cục bộ: các số lượng giữ nguyên, audit_logs vẫn là 393. Sau đó chạy -VerifyOnly thành công với MISSING_REQUIRED [], SUPPORT_MISSING_REQUIRED [] và RBAC_VERIFIED. Log tương ứng: target/rbac-audit/support-catalog-second-apply.log và target/rbac-audit/support-catalog-verify.log.

ID/version có thể khác giữa các máy hoặc thay đổi sau này; khi thao tác phải lấy lại bằng API danh mục.

### 11.3. Migration cho các máy khác

Công cụ RbacCatalogTool đã được sửa để chạy V003 rồi V004 trong cùng một transaction. DB đã có V003 cũng dùng cùng lệnh; không cần chạy lại khởi tạo schema hoặc dữ liệu mẫu.

~~~powershell
git pull
.\cap-nhat-rbac.cmd
.\cap-nhat-rbac.cmd -VerifyOnly
~~~

V004 tự bổ sung AUDIT_READ cho ROLE_SUPPORT nếu chưa có và giữ các grant hiện có. Sau kiểm chứng phải thấy:

~~~text
MISSING_REQUIRED []
SUPPORT_MISSING_REQUIRED []
RBAC_VERIFIED
~~~

Grant áp dụng cho mọi User mang ROLE_SUPPORT. V003 được giữ nguyên để các máy đã áp dụng vẫn có lịch sử nhất quán. Chạy lại không nhân đôi liên kết, không tăng version hoặc thêm audit nếu không có thay đổi thực tế.

### 11.4. Hai URL audit đã được mở trong SecurityConfig

Đã sử dụng import:

~~~java
import org.springframework.http.HttpMethod;
~~~

Trong authorizeHttpRequests, quy tắc GET dành cho audit đã được đặt trước quy tắc tổng quát của /admin:

~~~java
.requestMatchers(
        HttpMethod.GET,
        "/api/v1/admin/audit-logs",
        "/api/admin/audit-logs"
).hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPPORT")
.requestMatchers("/api/v1/admin/**", "/api/admin/**")
.hasAuthority("ROLE_ADMIN")
.anyRequest().authenticated()
~~~

Các permitAll dành cho login/Swagger nằm trước đoạn này.

Spring xét thứ tự các matcher; nếu vẫn đặt /admin/** yêu cầu ADMIN ở trước, SUPPORT sẽ bị chặn trước khi vào Controller.

Quy tắc riêng này chỉ mở GET của Audit Log. Các API quản trị User, role, permission tiếp tục yêu cầu ADMIN.

### 11.5. Kiểm tra quyền hiện hành tại AuditLogServiceImpl

Annotation đã được cập nhật:

~~~java
@PreAuthorize(
        "hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPPORT') and hasAuthority('AUDIT_READ')"
)
~~~

Điều kiện sau thay đổi:

| Danh tính/quyền | Kết quả |
|---|---|
| Chưa đăng nhập | 401 |
| ADMIN có AUDIT_READ | Được tra cứu |
| ADMIN thiếu AUDIT_READ | 403 |
| SUPPORT có AUDIT_READ | Được tra cứu |
| SUPPORT thiếu AUDIT_READ | 403 |
| CUSTOMER/HOST chỉ có AUDIT_READ | 403 |

Event AUDIT_LOG_SEARCHED mới có reason “Tra cứu Audit Log” để mô tả đúng cả hai vai trò. Actor thực tế lấy từ principal và được lưu trong audit; kiểm thử xác nhận actor SUPPORT được ghi đúng.

AuditLogController dùng chung hai URL hiện tại. Swagger hiển thị nhóm “Audit Log” và mô tả ADMIN/SUPPORT có AUDIT_READ.

### 11.6. Kiểm thử bản SUPPORT

- ADMIN/SUPPORT có AUDIT_READ truy cập thành công cả hai URL; log ghi đúng actor và reason.
- SUPPORT bị thu hồi AUDIT_READ nhận 403 ngay cả khi dùng JWT phát hành trước đó.
- ADMIN thiếu AUDIT_READ, CUSTOMER/HOST có AUDIT_READ đều bị từ chối.
- SUPPORT có các permission quản trị vẫn bị từ chối ở /api/v1/admin/users, /roles và /permissions.
- Gọi AuditLogService trực tiếp vẫn chịu kiểm tra role và permission.
- Request không thành công nếu không ghi được nhật ký truy cập AUDIT_LOG_SEARCHED.
- V004 cấp lại quyền còn thiếu, giữ các quyền khác và tăng version của role khi thêm grant.
- Chạy lại V003/V004 không tạo trùng, không phát sinh audit thừa.

Kết quả chạy toàn bộ bộ kiểm thử sau cập nhật SUPPORT:

| Lớp kiểm thử | Tổng | Đạt | Bỏ qua | Lỗi/Thất bại |
|---|---:|---:|---:|---:|
| DatphongApplicationTests | 1 | 1 | 0 | 0 |
| EntitySchemaTest | 5 | 2 | 3 | 0 |
| UserAccountStatusTest | 17 | 17 | 0 | 0 |
| UserRbacIntegrationTest | 44 | 44 | 0 | 0 |
| Tổng | 67 | 64 | 3 | 0 |

Maven kết thúc BUILD SUCCESS. Toàn bộ 44 ca RBAC đã chạy trên PostgreSQL với schema test riêng. 3 ca bỏ qua là các kiểm tra schema cũ yêu cầu DATPHONG_SCHEMA_VALIDATE=true, không phải các ca kiểm tra quyền SUPPORT.

Log kiểm thử: target/rbac-audit/support-audit-test.log. Log migration: target/rbac-audit/support-catalog-apply.log. Các lỗi constraint reject_profile_test/reject_audit_search_test trong log là tình huống cố ý để kiểm tra rollback; các ca này đều đạt.

Sau khi triển khai code mới, khởi động lại backend. Permission được nạp từ DB ở từng request; thay đổi permission của role không bắt buộc đăng nhập lại nếu JWT hiện tại còn hợp lệ. Nếu thay đổi role của một User hoặc tokenVersion, User đó phải đăng nhập lại.
