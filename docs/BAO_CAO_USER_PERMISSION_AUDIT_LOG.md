# Luồng admin/user A–Z

Cập nhật: 23/10/2026. Phạm vi: backend UC-01, UC-02, UC-03, UC-20; giữ chính sách tra cứu audit của UC-19.

**Đã sửa schema Booking và bổ sung RefundServiceImpl. Ứng dụng đã qua kiểm thử khởi động, Swagger và tài liệu API trả HTTP 200; chi tiết ở mục 9.**

Báo cáo mô tả phần tài khoản đã triển khai, chưa phải nghiệm thu trọn vẹn cả bốn UC. UC-20 còn thiếu quản trị amenities/cache; UC-01 còn cần thống nhất mã lỗi email trùng khi đăng ký đồng thời; UC-02 còn có JWT secret mặc định trong source.

## 1. Quy tắc đã triển khai

| Use case / trang đặc tả | Hành vi |
|---|---|
| UC-01, tr. 4–5 | Chuẩn hóa email, chống trùng ở DB, BCrypt, CUSTOMER mặc định, chờ xác minh, mã một lần, gửi email có retry. |
| UC-02, tr. 5–6 | Giới hạn theo IP/tài khoản, kiểm tra trạng thái, JWT ngắn hạn, refresh xoay vòng, đăng xuất, log thành công/thất bại. |
| UC-03, tr. 6–7 | `/me` lấy danh tính từ principal; DTO giới hạn trường; kiểm tra version; đổi mật khẩu cần mật khẩu cũ và thu hồi phiên. |
| UC-20, tr. 21 | ADMIN + permission cụ thể; đổi trạng thái/role riêng; bảo vệ admin cuối; audit cùng transaction; giữ tài sản/booking. |
| UC-19, tr. 20 | ADMIN hoặc SUPPORT có AUDIT_READ mới tra cứu audit; ghi nhận lần tra cứu. |

Nguồn: [đặc tả dự án](Dac_ta_he_thong_dat_phong_spring.pdf). Các quy tắc dưới đây mô tả code đã viết; kết quả chạy ở mục 9.

## 2. Luồng từ đăng ký đến sử dụng hệ thống

### A. Đăng ký

1. Client gửi tên, email, mật khẩu, nhập lại mật khẩu, đồng ý điều khoản; username/phone/gender tùy chọn.
2. DTO chuẩn hóa email bằng trim + lowercase, kiểm tra dữ liệu và từ chối trường ngoài hợp đồng, kể cả role/status/active.
3. Service kiểm tra trùng; unique index `lower(btrim(email))` chặn cả hai yêu cầu đăng ký đồng thời.
4. Lưu BCrypt; tạo user `PENDING_VERIFICATION`, `active=false`, chỉ gắn `ROLE_CUSTOMER`. Không gửi username thì dùng email làm username nội bộ.
5. Tạo mã ngẫu nhiên 256 bit. `verification_tokens` giữ SHA-256 của mã, hạn dùng, số lần thử.
6. Trong cùng transaction, lưu email vào `mail_events` và ghi audit. Commit xong trả **202**.
7. Worker lấy email đã đến hạn, commit bước nhận việc, rồi gọi SMTP **ngoài transaction DB**. Thành công đánh dấu SENT; lỗi SMTP chuyển RETRY, không xóa tài khoản.

Admin tạo tài khoản cũng đi qua bước 3–7: CUSTOMER và chờ xác minh. Muốn cấp role khác phải gọi API role riêng.

### B. Xác minh hoặc gửi lại mã

1. Client gửi email + mã bằng POST; mã không nằm trong URL/access log.
2. Khóa hàng User, lấy mã mới nhất. Mã hết hạn, đã dùng hoặc đã thu hồi bị từ chối.
3. Mỗi mã sai tăng `attempts`; đủ 5 lần thì thu hồi. Các thay đổi này vẫn commit dù trả **400**.
4. Đúng mã: đánh dấu đã dùng, ghi `verified_at`. Chỉ `PENDING_VERIFICATION` chuyển ACTIVE; LOCKED/IN_ACTIVE giữ nguyên.
5. Thu hồi phiên cũ và ghi audit cùng transaction. Nếu audit lỗi, việc xác minh cũng rollback.
6. Gửi lại mã cách lần phát trước ít nhất 60 giây; mã trước bị thu hồi. Email không tồn tại/đã xác minh nhận cùng thông báo để hạn chế dò tài khoản.

### C. Đăng nhập

1. Client gửi email + mật khẩu. Username cũ vẫn được hỗ trợ; mỗi request chỉ chọn một loại danh tính.
2. Redis giới hạn theo IP và danh tính. Sau đó Spring Security so mật khẩu BCrypt.
3. Chỉ tài khoản đồng thời có `status=ACTIVE`, `active=true`, `verified_at != null` được đăng nhập.
4. Khóa User và đối chiếu `tokenVersion` lần nữa, tránh đổi mật khẩu/khóa tài khoản chen giữa bước xác thực và cấp phiên.
5. Tạo JWT và refresh token. JWT mang userId bất biến, tokenVersion, role và hạn dùng; refresh là chuỗi ngẫu nhiên, DB chỉ giữ hash.
6. Trả accessToken, refreshToken, thời hạn và thông tin quyền để client hiển thị. Log đăng nhập không chứa mật khẩu/token.

Sai mật khẩu, tài khoản không tồn tại, bị khóa hoặc chưa xác minh đều trả **401** chung.

### D. Gọi API có bảo vệ

`RequestIdFilter → JwtAuthenticationFilter → SecurityConfig → Controller/validation → Service/PreAuthorize → Repository → transaction/audit → DTO/JSON`.

- Filter kiểm chữ ký/hạn JWT, tải user và quyền hiện tại từ DB, đối chiếu tokenVersion.
- Quyền trong JWT không quyết định quyền truy cập. Thu hồi permission ở DB có hiệu lực từ request tiếp theo.
- Không xác thực hoặc phiên cũ: **401**. Có danh tính nhưng thiếu quyền/phạm vi: **403**.
- `X-Request-ID` hợp lệ được giữ, nếu thiếu/sai định dạng thì tạo UUID. Response, log và audit dùng cùng mã đối chiếu.

### E. Làm mới phiên

1. Client gửi refresh token, kể cả khi access token đã hết hạn.
2. Tìm userId bằng hash, khóa User rồi mới nạp token. Mọi thao tác thu hồi phiên dùng cùng khóa này.
3. Token còn hạn/chưa dùng: tạo token tiếp theo trong cùng family, thu hồi token trước và ghi `replaced_by`.
4. Hạn family giữ nguyên từ lúc login: refresh không kéo dài vô hạn.
5. Dùng lại token đã xoay trong family còn sống: thu hồi **mọi phiên** của user và trả 401.
6. Token thuộc family đã đóng chỉ bị từ chối; không được phá phiên login mới.

Client phải gộp các yêu cầu refresh đồng thời thành **một yêu cầu**. Hai yêu cầu dùng cùng token bị coi là reuse; phiên sẽ bị thu hồi.

### F. Hồ sơ và đổi mật khẩu

- GET/PATCH `/users/me`: userId lấy từ principal, không nhận userId từ client.
- PATCH chỉ cho fullName, phone, gender, avatar cùng version. Email/username/role/status/password không sửa qua API này.
- Version cũ trả 409; không có thay đổi thực thì không tạo audit giả.
- Đổi mật khẩu cần mật khẩu cũ đúng, mật khẩu mới khác, tối thiểu 12 ký tự và tối đa 72 byte UTF-8.
- Lưu hash mới + tăng tokenVersion + thu hồi tất cả refresh + audit trong một transaction. Sau thành công client phải đăng nhập lại.

### G. Admin quản lý user

1. Kiểm tra ROLE_ADMIN và permission theo thao tác ở cả URL/service.
2. Kiểm tra phạm vi: không can thiệp admin khác có permission ngoài phạm vi của người gọi; không cấp/thu hồi role vượt quyền được ủy quyền.
3. Thay đổi trạng thái/role lấy khóa quản trị chung rồi khóa User; kiểm tra version.
4. Cấm tự đổi trạng thái/role; cấm loại bỏ admin hoạt động cuối cùng. Admin chưa xác minh không được tính là admin có thể sử dụng.
5. ACTIVE bắt buộc đã xác minh. Admin không được đưa tài khoản về PENDING_VERIFICATION.
6. Khi có thay đổi trạng thái/role: tăng tokenVersion, thu hồi mọi refresh, flush và audit. Audit lỗi thì rollback toàn bộ.
7. GET chi tiết và response thay đổi cảnh báo số cơ sở sở hữu/booking đang xử lý để admin xem tác động. Giữ nguyên dữ liệu và quan hệ sở hữu; không tự hủy booking/chuyển chủ.

### H. Đăng xuất

Gửi refresh token hiện hành → khóa User → tăng tokenVersion → thu hồi mọi refresh → audit → **204**. JWT trên mọi thiết bị bị từ chối từ request tiếp theo. Token không tồn tại/đã thu hồi/hết hạn cũng trả 204; gọi lại không làm mất phiên đăng nhập mới.

## 3. Trạng thái và phiên

| Tác nhân | Chuyển trạng thái | Điều kiện |
|---|---|---|
| Đăng ký/admin tạo user | → PENDING_VERIFICATION, active=false | CUSTOMER mặc định |
| Xác minh đúng | PENDING_VERIFICATION → ACTIVE, active=true | Mã mới nhất, còn hạn, chưa dùng |
| Xác minh khi bị hạn chế | LOCKED/IN_ACTIVE giữ nguyên | Chỉ ghi verified_at |
| Admin | → LOCKED hoặc IN_ACTIVE, active=false | Quyền, phạm vi, version, không phải admin cuối |
| Admin | → ACTIVE, active=true | Đã xác minh, đủ các kiểm tra quản trị |
| Admin | → PENDING_VERIFICATION | Từ chối |

| Mặc định | Giá trị |
|---|---|
| Access JWT | 15 phút; không vượt hạn family |
| Refresh family | 7 ngày tuyệt đối từ login |
| Mã xác minh | 20 phút, tối đa 5 lần sai |
| Gửi lại mã | Sau ít nhất 60 giây |
| Rate limit mỗi hành động | 50/IP và 10/danh tính trong 5 phút |
| Email worker | Mỗi 10 giây, tối đa 10 email/lượt; lease 60 giây |
| Retry SMTP | 15 × số lần lỗi giây, tối đa 300 giây; dừng khi mã hết hạn |

Các mốc xác thực/audit dùng Instant/UTC. Giờ nhận/trả phòng địa phương thuộc nghiệp vụ khác, không đổi sang UTC tùy tiện.

## 4. API và quyền

Các đường dẫn dưới đây có tiền tố `/api/v1`. Auth còn alias `/api/auth`; `/refresh-token` là alias của `/refresh`.

| Method / đường dẫn | Quyền / kết quả |
|---|---|
| POST /auth/register | Công khai; 202 |
| POST /auth/verify | Công khai; 200 hoặc 400 |
| POST /auth/resend-verification | Công khai; 200 chung |
| POST /auth/login | Công khai; 200 + cặp token |
| POST /auth/refresh | Refresh hợp lệ; 200 + cặp token mới |
| POST /auth/logout | Refresh trong body; 204 |
| GET /users/me | Đã xác thực |
| PATCH /users/me | Đã xác thực; version |
| PATCH /users/me/password | Đã xác thực; mật khẩu cũ + version |
| GET /admin/users, /admin/users/{id} | ADMIN + USER_READ |
| POST /admin/users | ADMIN + USER_MANAGE |
| PATCH /admin/users/{id}/profile, /{id}/status | ADMIN + USER_MANAGE |
| PUT /admin/users/{id}/roles | ADMIN + USER_ROLE_MANAGE |
| GET /admin/roles | ADMIN + ROLE_READ |
| Tạo/sửa/xóa role, PUT role/{id}/permissions | ADMIN + ROLE_MANAGE |
| GET /admin/permissions | ADMIN + PERMISSION_READ |
| Tạo/sửa/xóa permission | ADMIN + PERMISSION_MANAGE |
| GET /admin/audit-logs | ADMIN/SUPPORT + AUDIT_READ |

Danh sách admin phân trang, che email/phone; không trả entity/hash/secret. Không có API xóa cứng user. Role hệ thống/đang sử dụng và permission còn gắn role có ràng buộc bảo vệ.

## 5. Vì sao chọn cách này — trade-off

| Lựa chọn | Được gì | Chấp nhận |
|---|---|---|
| Logout/đổi thông tin nhạy cảm thu hồi mọi thiết bị | Một cơ chế tokenVersion, ít trạng thái và dễ kiểm tra | Chưa có đăng xuất riêng từng thiết bị |
| Tải user/quyền từ DB mỗi request | Khóa tài khoản và thu hồi permission có hiệu lực ngay ở request sau | Tăng lượt đọc DB; chưa cache |
| Refresh một lần, khóa User chung | Chặn phát sinh phiên sau khi đã thu hồi và phát hiện token bị dùng lại | Client phải tuần tự hóa refresh; lỗi mạng sau commit có thể buộc login lại |
| Family hết hạn tuyệt đối | Không duy trì phiên vĩnh viễn | Sau 7 ngày phải login lại dù vẫn sử dụng |
| Hàng đợi email trong DB hiện có | Không cần message broker; SMTP lỗi không làm mất đăng ký | Gửi ít nhất một lần: mất kết quả sau SMTP có thể gửi lặp; mã vẫn một lần |
| Token xác minh chỉ hash trong bảng token | Lộ bảng token không cho dùng ngay mã | MailEvent phải tạm giữ mã để worker gửi; xóa payload khi SENT/CANCELLED/hết hạn; cần hạn chế quyền đọc DB |
| Token trả JSON, không dùng cookie xác thực | Hợp với REST/Swagger hiện có, không phát sinh luồng cookie/CSRF riêng | Client trình duyệt nên giữ token trong bộ nhớ, dùng HTTPS; tải lại trang cần login. Chưa có frontend giữ phiên qua reload |
| Redis lỗi trả 503 cho thao tác auth | Không vô tình bỏ giới hạn khi hạ tầng hỏng | Auth tạm gián đoạn khi Redis ngừng |
| Không tự đánh dấu user cũ đã xác minh | Không giả định quyền sở hữu email | User cũ, kể cả admin thiếu verified_at, cần gửi lại mã và xác minh một lần |

Bảng token chỉ lưu hash; không ghi token/mật khẩu/hash vào audit/log. Dữ liệu email hàng đợi là ngoại lệ có chủ đích ở bảng trên. Xác minh email không thay thế MFA; chưa thêm MFA vào phạm vi này.

## 6. Tổ chức code

- `AuthenticationController/Service`: hợp đồng HTTP, giới hạn và điều phối.
- `AccountRegistrationService`: tạo user, phát/gửi lại/xác minh mã.
- `AccountSessionService`: mở, xoay, thu hồi phiên; dùng lại cho đổi mật khẩu/admin.
- `VerificationMailService`: nhận việc, gọi SMTP ngoài transaction, ghi kết quả/retry.
- `AdminUserService + RbacPolicy`: trạng thái, role, phạm vi, admin cuối.
- Repository chỉ truy vấn/khóa; DTO không cho client ghi trường quản trị.
- Dùng lại User, VerificationToken, RefreshToken, MailEvent, EmailTemplateVersion; không thêm bảng hoặc tầng framework riêng.

## 7. Cập nhật DB và chạy thử

Từ thư mục có pom.xml, với DB đã khởi tạo baseline V000/V002:

~~~powershell
.\cap-nhat-schema.cmd
.\cap-nhat-rbac.cmd
.\cap-nhat-rbac.cmd -VerifyOnly
docker compose up -d mailpit
.\mvnw.cmd spring-boot:run
~~~

V005 thêm index và mẫu email vi-VN; launcher áp dụng V003–V005 trong transaction. Chạy lại không sửa template đã tùy chỉnh, không đổi user/mật khẩu/trạng thái cũ. Nếu dữ liệu cũ trùng email sau trim/lower thì migration thất bại và rollback; phải xử lý tài khoản trùng trước, không tự gộp.

V006 bổ sung cột runtime và 4 bảng Fake Bank/webhook để khớp 78 entity. Không sửa baseline đã có checksum, không drop/truncate dữ liệu. Đây là đồng bộ cấu trúc, chưa phải đối soát số tiền hoàn lịch sử. Bộ giả lập dùng khóa webhook ngẫu nhiên dùng chung trong một tiến trình; có thể cấu hình khóa cố định bằng APP_FAKEBANK_WEBHOOK_SECRET khi cần dùng chung giữa các tiến trình.

Mailpit dùng SMTP localhost:1025 và hộp thư thử tại [localhost:8025](http://localhost:8025). Môi trường khác cấu hình SMTP_HOST/PORT/USERNAME/PASSWORD/AUTH/STARTTLS. Các giới hạn xác thực có thể cấu hình bằng `app.auth.*`; TTL JWT dùng JWT_EXPIRATION_MS.

JWT trước thay đổi dùng username làm subject sẽ không còn được chấp nhận. Client phải đăng nhập lại để nhận JWT dùng userId.

## 8. Ví dụ một vòng hoàn chỉnh

~~~json
POST /api/v1/auth/register
{"fullName":"Nguyen Van A","email":"a@example.test","password":"Example-password-2026",
 "confirmPassword":"Example-password-2026","termsAccepted":true}

POST /api/v1/auth/verify
{"email":"a@example.test","token":"ma-nhan-trong-email"}

POST /api/v1/auth/login
{"email":"a@example.test","password":"Example-password-2026"}

GET /api/v1/users/me
Authorization: Bearer <accessToken>

POST /api/v1/auth/refresh
{"refreshToken":"refreshToken-hien-hanh"}

POST /api/v1/auth/logout
{"refreshToken":"refreshToken-moi-nhat"}
~~~

Mỗi lần refresh thành công phải thay cả hai token ở client. Không gửi lại refresh cũ. Các response auth đặt Cache-Control: no-store. Hợp đồng đăng ký mới yêu cầu confirmPassword và termsAccepted; client cũ cần cập nhật.

## 9. Kiểm chứng và giới hạn

Kết quả chạy lại ngày 23/10/2026, bật cả kiểm thử PostgreSQL/Redis và validate schema:

| Nhóm | Kết quả |
|---|---|
| EntitySchemaTest | 6/6 đạt: metadata, DB hiện tại, baseline + V006/V007, round-trip booking/payment/outbox/daterange/JSONB |
| CancellationPolicySnapshotTest | 4/4 đạt: JSON mới, định dạng cũ, JSON seed, tính phí không đổi |
| Nhóm admin/user/auth | 97/97 đạt, gồm Redis và SMTP thực tế |
| DatphongApplicationTests | 1/1 đạt: máy chủ HTTP thật, Swagger và /v3/api-docs trả 200 không cần đăng nhập |
| RefundServiceIntegrationTest trong lượt toàn bộ | 21/21 đạt: PostgreSQL + Fake Bank thật, đồng thời, idempotency, timeout, rollback, retry và chuyển trạng thái |
| Lượt chạy toàn bộ Maven test | 129/129 đạt, 0 error, 0 failure, 0 skip |
| Lượt kiểm tra cuối sau khi bổ sung chặn yêu cầu WALLET gửi sang bank | 23/23 đạt: 22 ca hoàn tiền và 1 ca khởi động/Swagger, 0 error/failure/skip |
| Migration V006 và V007 | Thành công; giữ nguyên số dòng trong 78 bảng hiện có khi áp dụng V007 |

Không còn lỗi thiếu bean RefundService hay lỗi schema Booking trong kiểm thử khởi động.

Các điểm schema đã sửa:

- Booking lưu cancellation_policy_snapshot bằng JSONB. Snapshot mới là JSON object; vẫn đọc được định dạng v1 cũ và JSON của bộ seed. Giữ nguyên công thức tính phí/hoàn.
- Các thuộc tính Java user/checkinDate/checkoutDate/guestsCount/totalPrice ánh xạ về customer_id/check_in/check_out/guest_count/total_amount trong catalog.
- RoomAllocation lưu stay bằng daterange [checkIn, checkOut), đọc hai ngày bằng formula; cập nhật HELD/CONFIRMED/RELEASED để phù hợp exclusion constraint.
- Outbox lưu JSONB và các trường event_id/payload_version; lịch sử booking có event_id, actor_type, occurred_at.
- Payment ánh xạ FAKE_BANK trong API sang FAKE_BANK_DEBIT trong DB, giữ metadata bắt buộc và cờ is_open theo trạng thái.
- Migration V006 bổ sung host_note và các cột/bảng runtime còn thiếu. Đã áp dụng bằng transaction, kiểm tra giữ nguyên số dòng của mọi bảng có sẵn.

Ba hàm hoàn tiền đã có implementation thật và tác vụ nền thực thi. Luồng hủy booking và thanh toán đến muộn dùng chung service, ghi outbox cùng transaction, không gọi bank khi đang giữ khóa booking. Chi tiết từng bước và trade-off: [Luồng hoàn tiền](LUONG_HOAN_TIEN.md).

Lệnh kiểm tra (PowerShell):

~~~powershell
$env:DATPHONG_RBAC_INTEGRATION='true'
$env:DATPHONG_SCHEMA_VALIDATE='true'
$env:DATPHONG_REFUND_INTEGRATION='true'
.\mvnw.cmd test
~~~

Test tích hợp tạo schema UUID riêng và xóa schema sau khi chạy. Kiểm thử khởi động dùng scheduler giả để không chạy job làm thay đổi DB demo; các service và máy chủ HTTP vẫn là implementation thật. Kiểm thử hoàn tiền gọi worker thật trong schema riêng.

Log lượt chạy toàn bộ: `target/refund-final-tests.log`; lượt kiểm tra cuối: `target/refund-final-check.log`; báo cáo JUnit: `target/surefire-reports/`. Đối chiếu sau kiểm thử: 78 bảng public giữ nguyên số dòng, hai index V007 đã tồn tại (`target/refund-schema-verification.json`). SMTP đã được thử qua máy chủ loopback; Mailpit Compose chưa chạy vì phiên làm việc chưa có Docker CLI.

Tham khảo kỹ thuật: [RFC 9700 — refresh token rotation](https://www.rfc-editor.org/rfc/rfc9700.html), [Spring Boot — gửi email](https://docs.spring.io/spring-boot/reference/io/email.html), [Mailpit — Docker](https://mailpit.axllent.org/docs/install/docker/).
