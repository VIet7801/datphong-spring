# Đối chiếu dự án với đặc tả hệ thống đặt phòng v3.0

Ngày kiểm tra: 16/10/2026. Nguồn: [Dac_ta_he_thong_dat_phong_spring.pdf](../Dac_ta_he_thong_dat_phong_spring.pdf), 51 trang, 36 use case.
Các số trang dưới đây là số trang ghi trong PDF. Đánh giá áp dụng cho working tree tại lúc kiểm tra, không chỉ commit HEAD.

## 1. Kết luận

Dự án có nền tảng Identity/Security, API user, một phần quản lý/tìm phòng và kết nối MinIO.
Chưa đủ để nghiệm thu hệ thống đặt phòng theo đặc tả: chưa có luồng booking đang thực thi,
chưa có giá từng đêm/quote, payment/Fake Bank, refund và các worker nghiệp vụ.

Đánh giá theo luồng nghiệp vụ, không theo số entity/DTO: **6 use case có một phần triển khai;
30 use case chưa có luồng thực thi đáp ứng mô tả; chưa có use case nào đủ bằng chứng để kết luận
đã đáp ứng toàn bộ tiêu chí nghiệm thu.** Đây không phải phần trăm tiến độ hay điểm chấm dự án.

Phân loại:

- **Đáp ứng thành phần**: có hành vi cụ thể hoặc kiểm chứng tương ứng, nhưng không suy rộng thành hoàn thành cả use case.
- **Một phần**: đã có luồng API/service thực thi một số bước, còn thiếu điều kiện, ngoại lệ hoặc bị lỗi.
- **Chưa có luồng**: chỉ có entity/DTO/mapper, code bị comment hoặc chưa có API/service/worker thực hiện nghiệp vụ.

Không đánh trượt chỉ vì tên package hoặc đường dẫn API khác ví dụ trong PDF: mục 7 ghi API contract gợi ý.
Các hành vi, quyền truy cập, bất biến và yêu cầu kỹ thuật mới là nội dung đối chiếu chính.

## 2. Phạm vi và phương pháp

- Đọc nội dung 51 trang; kiểm tra trực quan các trang có bảng vai trò, use case, trạng thái, kiến trúc và nghiệm thu.
- Kiểm kê controller/service/repository, đọc các luồng đang hoạt động và kiểm tra mã bị comment.
- Đối chiếu phân quyền, DTO, truy vấn availability, mapping phòng, upload, cấu hình, migration và test.
- Chạy lại test trạng thái tài khoản và các probe nghiệp vụ bằng Mockito/MockMvc; không ghi dữ liệu nghiệp vụ vào DB/MinIO.
- Chỉ đọc metadata index của PostgreSQL local; không tạo account, phòng, booking, payment hay sửa schema trong lượt audit này.
- Không sửa logic sản phẩm. Báo cáo không chứng nhận frontend, triển khai production, hiệu năng hay đồng thời của booking.

Kiểm kê mã nguồn: **3 REST controller hoạt động, 11 mapping HTTP**:
1 login; 5 user; 5 room. Có 52 entity, 67 request/response DTO và 19 mapper, nhưng số lượng
class không chứng minh nghiệp vụ đã chạy. `BookingController` là class rỗng, không phải REST controller;
`RoleController` và `BookingServiceImpl` đang bị comment toàn bộ.

## 3. Những yêu cầu thành phần đã có

| Yêu cầu trong PDF | Bằng chứng hiện tại | Giới hạn kết luận |
|---|---|---|
| UC-02: AuthenticationManager/Provider + PasswordEncoder + access JWT (tr.5-6) | [AuthenticationService](../src/main/java/com/vn/datphong/service/AuthenticationService.java), [SecurityConfig](../src/main/java/com/vn/datphong/config/SecurityConfig.java), [JwtTokenProvider](../src/main/java/com/vn/datphong/security/JwtTokenProvider.java) | Có login bằng username và JWT; chưa có toàn bộ vòng đời phiên/refresh/logout; PDF yêu cầu luồng email |
| UC-02/20: chặn tài khoản không hoạt động/khóa (tr.5-6,21) | [CustomUserDetailsService](../src/main/java/com/vn/datphong/service/Impl/CustomUserDetailsService.java), [JwtAuthenticationFilter](../src/main/java/com/vn/datphong/security/JwtAuthenticationFilter.java) | 14 test đạt với password hash và dữ liệu giả; không đồng nghĩa các luồng tạo user đều lưu hash đúng |
| UC-03: response user không chứa password/secret 2FA (tr.6) | [UsersResponse](../src/main/java/com/vn/datphong/dto/response/UsersResponse.java), [UserMapper](../src/main/java/com/vn/datphong/mapper/UserMapper.java), [UserController](../src/main/java/com/vn/datphong/controller/UserController.java) | DTO đã loại hai trường; vẫn dùng chung một DTO cho admin/self, chưa tách trường quản trị theo scope |
| UC-03: kiểm tra chính chủ ở service (tr.6-7) | [UserServiceImpl](../src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java), [UserAuthorization](../src/main/java/com/vn/datphong/security/UserAuthorization.java) | Có ADMIN/chính chủ; chưa có permission granular, audit và version conflict cho user |
| UC-07: kiểm tra ngày, một số filter, sort whitelist, phân trang (tr.9-10) | [RoomServiceImp.list](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java), [RoomRepository](../src/main/java/com/vn/datphong/repository/RoomRepository.java) | Thiếu nhiều điều kiện công khai/tồn kho/giá và validation; khách chưa đăng nhập hiện bị 401 |
| UC-06: thư viện MinIO và thao tác stream upload/remove/presigned GET (tr.8-9) | [MinioServiceImpl](../src/main/java/com/vn/datphong/service/Impl/MinioServiceImpl.java) | Helper đã có; chưa đủ pipeline metadata, finalize, cleanup, quyền sở hữu và cache |
| Mục 6.1: môi trường Postgres/Redis/MinIO, JPA, Swagger (tr.40) | [Compose](../docker-compose.yml), [pom.xml](../pom.xml), các config | Có cấu hình nền; Redis chưa được dùng cho cache/rate limit nghiệp vụ; chưa đủ monitoring |
| UC-01: unique email chuẩn hóa tại DB (tr.4-5) | DB local có unique index `uq_users_email_normalized` trên `lower(btrim(email))` | Xác nhận metadata local; chưa có migration versioned trong repository để dựng lại index này từ DB rỗng |

Trạng thái tài khoản hiệu lực hiện tại: chỉ `active=true` và `status=ACTIVE` được xác thực;
`IN_ACTIVE`, `LOCKED`, `active=false/null` bị chặn. JWT cũ bị từ chối trong thời gian tài khoản
bị chặn; mở lại tài khoản có thể cho token cũ còn hạn dùng tiếp vì chưa có token version/denylist.

## 4. Các phát hiện cần ưu tiên

### F01 - Luồng tạo user hiện lưu mật khẩu nguyên văn

PDF: UC-01 bước 4 (tr.4), UC-02 (tr.5), yêu cầu hash mật khẩu.
[UserServiceImpl.create](../src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java) gọi mapper rồi save;
[UserMapper.toEntity](../src/main/java/com/vn/datphong/mapper/UserMapper.java) gán `.password(req.getPassword())`.
Không có bước encode trong luồng này. Probe đã xác nhận giá trị truyền vào repository vẫn là chuỗi đầu vào.
Việc có bean BCryptPasswordEncoder hoặc helper UserImportMapper hash password không bảo vệ luồng `/users/add`.

Ưu tiên: sửa trước khi cho phép tạo tài khoản thực tế; không nhầm việc ẩn password trong response với bảo vệ password khi lưu.

### F02 - User thường có thể gọi API tạo/sửa/xóa media phòng

PDF: UC-04/05/06, HOST + owner scope (tr.7-9,45).
[SecurityConfig](../src/main/java/com/vn/datphong/config/SecurityConfig.java) chỉ yêu cầu authenticated cho các URL phòng;
[RoomController](../src/main/java/com/vn/datphong/controller/RoomController.java) và
[RoomServiceImp](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java) chưa có kiểm tra HOST/owner.
Probe dùng principal ROLE_CUSTOMER gọi create nhận **201**, trong khi chính sách đặc tả yêu cầu từ chối **403**.
Đây là kiểm chứng HTTP/security với service giả; không khẳng định đã ghi phòng thật vào DB.

Ưu tiên: chặn theo role/permission và kiểm tra owner scope từ DB trước mọi mutation phòng/media.

### F03 - Luồng booking chưa tồn tại trong ứng dụng đang chạy

PDF: UC-09 đến UC-15 (tr.11-17), UC-35/36 (tr.35-38).
[BookingController](../src/main/java/com/vn/datphong/controller/BookingController.java) rỗng;
[BookingServiceImpl](../src/main/java/com/vn/datphong/service/Impl/BookingServiceImpl.java) bị comment toàn bộ.
Chưa có handler tạo hold, callback payment, job expiry, hủy/hoàn tiền, check-in/out.
Entity Booking, BookingNight, Payment, RefundRequest, OutboxEvent và IdempotencyRecord chưa tạo ra workflow thực thi.
Do đó chưa thể kiểm chứng invariant không bán trùng hoặc E2E đặt -> trả tiền -> lưu trú.

### F04 - API sửa phòng chưa sửa entity

PDF: UC-05 (tr.8).
Trong [RoomServiceImp.update](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java), các dòng
`req.setName(req.getName())`, `req.setPricePerNight(...)`... sửa lại chính request,
không cập nhật `room`. Sau đó repository save entity cũ.
Probe đổi tên từ `Original name` sang `Updated name` vẫn nhận tên cũ.

Ngoài ra, [RoomMapper.toEntity](../src/main/java/com/vn/datphong/mapper/RoomMapper.java) chưa ánh xạ
propertyId/roomTypeId/roomUnitId thành quan hệ và chưa xác minh ownership.

### F05 - Upload chưa lưu metadata để phòng hiển thị ảnh

PDF: UC-06 bước 3,5,6,8,9 (tr.9).
[RoomServiceImp.uploadMedia](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java) upload object rồi trả URL;
không tạo/lưu RoomImage hoặc media_asset. Probe xác nhận không có lời gọi `roomImageRepository.save(...)`.
Trong khi đó danh sách phòng lại nạp ảnh từ `findByRoomIdOrdered`.
Chưa có PENDING -> READY/finalize, kiểm tra object thực tế, orphan cleanup hay invalidation.
`@Transactional` đang bao quanh việc truyền file qua MinIO, trái yêu cầu không giữ transaction DB trong lúc upload dài.

Không kết luận binding multipart hỏng chỉ từ annotation `@RequestBody Part`;
việc nhận multipart/Part cần đọc cùng kết quả probe và giới hạn MockMvc ở mục 8.

### F06 - Search chưa đúng phạm vi công khai và tồn kho

PDF: UC-07 (tr.9-10).
Probe gọi `/api/rooms` không có token nhận **401**, trong khi GUEST phải được tìm công khai.
[RoomRepository.searchRoomsWithAvailability](../src/main/java/com/vn/datphong/repository/RoomRepository.java):

- Chưa lọc property PUBLISHED, room active, lịch bảo trì/đóng bán.
- Chặn HOLD nhưng không kiểm tra hold đã hết hạn; chưa tính PENDING_PAYMENT còn hiệu lực như đặc tả.
- Có điều kiện overlap dạng `[checkIn, checkOut)`, nhưng chưa phải cơ chế bảo đảm tồn khi ghi booking.
- Chưa tính giá mỗi đêm từ room_rates và chưa kiểm tra đủ giá cho mọi đêm.

Service chưa giới hạn size/range giá/số khách đầy đủ, dùng LocalDate.now() theo server thay vì timezone property.
Nạp media theo từng phòng có nguy cơ N+1; chưa có số đo SQL/EXPLAIN để xác nhận hiệu năng.

### F07 - Quản trị tài khoản/quyền chưa có workflow đầy đủ

PDF: UC-03/20 (tr.6-7,21).
Đã có ADMIN guard và DTO, nhưng chưa có API cấp/thu role, permission granular, lý do và audit.
`UsersResquest` vẫn nhận roleId/status/active trong cùng DTO dùng cho sửa hồ sơ;
đặc tả yêu cầu DTO không cho nhận các trường quản trị, không chỉ bỏ qua chúng.
`UserMapper.toEntity()` chưa gán roleId vào user, nên request có roleId không bảo đảm tài khoản mới có role.
`UserServiceImpl.update()` chỉ sửa field hồ sơ; chưa có luồng ADMIN khóa/mở khóa tài khoản.
`delete()` gọi deleteById trực tiếp, chưa có quy trình bảo toàn đối tượng đã có lịch sử theo đặc tả.

### F08 - Các lựa chọn hiện tại khác đặc tả và cần được ghi nhận

1. **Role đăng ký:** PDF tr.1,4 yêu cầu GUEST xem công khai, tài khoản đăng ký nhận CUSTOMER.
   Gợi ý trước đó trong hội thoại về mặc định ROLE_GUEST không khớp PDF và không nên dùng làm contract đăng ký.
2. **Trạng thái xác minh:** PDF tr.4 yêu cầu PENDING_VERIFICATION -> ACTIVE.
   Theo yêu cầu mới của chủ dự án, enum hiện chỉ có ACTIVE/IN_ACTIVE/LOCKED.
   Cần mô hình hóa trạng thái xác minh riêng hoặc thống nhất thay đổi đặc tả;
   audit không tự ý khôi phục enum đã được yêu cầu bỏ.
3. **Stack:** PDF tr.40,50 ghi Java 21 + Spring Boot 3.x; pom/Dockerfile hiện Java 17 + Boot 4.1.1.
   JBR 25 dùng để biên dịch trong IDE không thay đổi Java target 17 của Maven.
4. **Migration:** PDF yêu cầu Flyway versioned. Hiện dùng ddl-auto:update và script normalize-user-status thủ công;
   chưa tái tạo toàn bộ schema/index/PostgreSQL enum types từ repo trên DB rỗng.
5. **API path:** khác ví dụ `/api/v1/...` không tự động là lỗi; cần thống nhất contract FE nhưng PDF cho phép tên class/API khác.

## 5. Ma trận 36 use case

| UC | Trang | Yêu cầu | Mức hiện tại | Bằng chứng/khoảng thiếu chính |
|---|---|---|---|---|
| UC-01 | 4-5 | Đăng ký, xác minh | Chưa có luồng | Có RegisterRequest/OtpVerification nhưng không có register/verify service/API; chưa chuẩn hóa email/verification/outbox; F01/F08 |
| UC-02 | 5-6 | Login, refresh, logout | Một phần | Login/JWT/trạng thái có; thiếu refresh rotation, logout/revocation, rate limit, correlationId; dùng username thay email |
| UC-03 | 6-7 | Hồ sơ, phạm vi quyền | Một phần | DTO/ADMIN/chính chủ có; thiếu DTO riêng, user version, đổi mật khẩu, role assignment/audit; F07 |
| UC-04 | 7 | Tạo và publish property | Chưa có luồng | Có Property/PropertyMapper, không có publish workflow kiểm tra phòng/giá/ảnh/chính sách |
| UC-05 | 8 | Loại phòng, room unit, rates/calendar | Một phần | Có API Room create/update và filter; update sai, thiếu HOST/owner và quản lý rates/calendar/đóng bán; F02/F04 |
| UC-06 | 8-9 | Upload/finalize/xóa ảnh | Một phần | Có MinIO helpers và endpoint upload/delete; thiếu metadata/lifecycle/ownership/cleanup; F05 |
| UC-07 | 9-10 | Search theo filter/ngày | Một phần | Có filter/ngày/page/sort/overlap; thiếu public visibility, rate per night, cache, expiry chính xác; F06 |
| UC-08 | 10-11 | Chi tiết và báo giá từng đêm | Chưa có luồng | Có DTO quote và helper getById, chưa có API detail/quote hay service tính giá theo từng đêm |
| UC-09 | 11-12 | Booking và hold | Chưa có luồng | Controller rỗng, service comment; không có transaction giữ kho/idempotency/snapshot; F03 |
| UC-10 | 12-13 | Payment/callback | Chưa có luồng | Entity/DTO Payment không kèm API/service/webhook chạy được |
| UC-11 | 13-14 | Job hold expiry | Chưa có luồng | Không có scheduler/worker claim hold hết hạn |
| UC-12 | 14-15 | Hủy và tính phí snapshot | Chưa có luồng | Không có cancellation preview/confirm/state transition |
| UC-13 | 15-16 | Refund và reconcile | Chưa có luồng | Có RefundRequest entity/mapper, chưa có xử lý refund/provider/reconciliation |
| UC-14 | 16-17 | HOST xử lý booking | Chưa có luồng | Không có danh sách/chi tiết booking theo owner scope |
| UC-15 | 17 | Check-in/out | Chưa có luồng | Có field/state, không có use case chuyển trạng thái theo thời gian/quyền |
| UC-16 | 17-18 | Review/moderation | Chưa có luồng | Entity/DTO/mapper; không có eligibility, API/service, kiểm duyệt |
| UC-17 | 18-19 | Notification/outbox | Chưa có luồng | Có OutboxEvent/Notification, không có producer/consumer/read API hoạt động |
| UC-18 | 19-20 | Báo cáo cơ bản | Chưa có luồng | Có response/mapper nhưng chưa có query aggregate/API theo scope |
| UC-19 | 20-21 | Audit/tra cứu sự cố | Chưa có luồng | LoggingAspect ghi thời gian/lỗi không phải audit bền vững actor/before-after/reason/correlation |
| UC-20 | 21 | Admin users/roles/catalog | Một phần | User list/create guard và chặn tài khoản khóa có; thiếu admin status API/role/permission/audit; RoleController bị comment |
| UC-21 | 22-23 | Dashboard theo scope | Chưa có luồng | Không có analytics service/query/cache/KPI dictionary |
| UC-22 | 23-24 | Booking analytics đa chiều | Chưa có luồng | Không có SQL aggregate, comparison period, drill-down |
| UC-23 | 24-25 | Doanh thu/cash/refund | Chưa có luồng | Không có financial facts/query theo captured/refunded time |
| UC-24 | 25 | Occupancy/ADR/RevPAR | Chưa có luồng | Không có date spine/occupancy aggregation/report |
| UC-25 | 25-26 | Funnel/cancel/payment failures | Chưa có luồng | Không có event pipeline hay funnel analytics |
| UC-26 | 26-27 | Dynamic report/export/Redis | Chưa có luồng | Không có query whitelist/report jobs/cache isolation/TTL/invalidation |
| UC-27 | 27-28 | Conversation | Chưa có luồng | Entity Conversation, chưa có API participant/scope/idempotency |
| UC-28 | 28-29 | Message/complaint | Chưa có luồng | Entity/DTO/mapper, chưa có persistence workflow/seq/read receipt/ticket API |
| UC-29 | 29-30 | Đề nghị giờ check-in | Chưa có luồng | Không có checkin_request workflow/API/approve/counter-propose |
| UC-30 | 30-31 | Password/system policy | Chưa có luồng | SystemSetting đơn giản, không có policy service/version/effectiveAt; validation độ dài đang hard-code |
| UC-31 | 31-32 | Template email versioned | Chưa có luồng | Không có template/version/preview/publish/renderer whitelist |
| UC-32 | 33 | Email giao dịch | Chưa có luồng | Không có mail worker/provider/outbox delivery/dedup |
| UC-33 | 34-35 | Reminder check-in | Chưa có luồng | Không có dueAt policy/reminder scheduler/claim/dedup |
| UC-34 | 35 | Mail retry/DLQ/monitoring | Chưa có luồng | Không có mail events/delivery attempts/resend API |
| UC-35 | 35-37 | Fake Bank độc lập | Chưa có luồng | Không có app/schema/ledger/debit/webhook simulator riêng |
| UC-36 | 37-38 | Saga/compensation | Chưa có luồng | Không có orchestration, reconciliation hay fault injection xuyên hai transaction boundary |

## 6. Yêu cầu kỹ thuật và vận hành

| Nội dung PDF | Đối chiếu hiện tại |
|---|---|
| PostgreSQL là nguồn sự thật, constraints/locks bảo đảm tồn (tr.38-41) | Có JPA/repository, các entity @Version; chưa có workflow ghi booking được bảo vệ và kiểm thử đồng thời |
| Giá snapshot BigDecimal/NUMERIC (tr.2,11,39) | Booking/BookingNight có field BigDecimal/snapshot; chưa có logic tính/chốt snapshot. Room còn cả pricePerNight Long và basePrice BigDecimal |
| Flyway/schema từ DB rỗng (tr.40,49) | Chưa có Flyway; một script chuẩn hóa user status không thay thế bootstrap migration đầy đủ |
| Redis cache-aside/rate limit/invalidation/fallback (tr.10,27,40,43) | Có RedisConfig/templates, chưa thấy sử dụng nghiệp vụ ngoài cấu hình; không coi container Redis là cache đã triển khai |
| MinIO private/presigned/metadata/cleanup (tr.9) | Có helper presigned GET, nhưng upload trả raw URL; chưa có pipeline lifecycle và cleanup |
| Actuator/Micrometer/Prometheus/Grafana (tr.40,45-46,50) | Chưa có dependency/config/service/dashboard tương ứng trong repo |
| Error JSON có traceId/code/message/details (tr.45,49) | Có ApiResponse/error, nhưng hai advice trùng handler, filter dùng sendError; không có traceId thống nhất; Exception handler trả ex.getMessage() |
| Secrets ngoài source (tr.6,40,46) | Có env placeholders/.env.example nhưng vẫn có fallback credential/JWT secret trong application.yaml; chưa có rotation policy |
| Testcontainers/PostgreSQL thật cho lock/constraint (tr.40,46,49) | Chưa có dependency/test tương ứng; status tests dùng mock repository, không chứng minh transaction/constraint |
| E2E/performance/recovery (tr.45-46,50-51) | Chưa có bằng chứng load test, EXPLAIN, p95/p99, reconciliation/fault injection, scheduler nhiều instance |
| Docker/local docs (tr.49) | Có Compose PostgreSQL/Redis/MinIO và hướng dẫn chạy backend IDE; việc bỏ service app là lựa chọn phát triển đã được yêu cầu, không tự coi là lỗi nghiệp vụ |

## 7. Thứ tự xử lý đề xuất

1. **Chốt contract Identity:** CUSTOMER/GUEST, xác minh với enum ba giá trị đã chọn, email/username,
   DTO đăng ký/self/admin, hash password, quản trị role và status. Chặn mọi mutation phòng theo HOST/owner.
2. **Hoàn thiện catalog/search:** sửa update entity, gắn đúng property/room type/unit, publish/rates/calendar,
   public search, quote từng đêm và media lifecycle. Tạo versioned migration để dựng từ DB rỗng.
3. **Xây vertical slice booking:** PostgreSQL lock/constraint + idempotency + nightly/cancellation snapshot + expiry,
   chứng minh bằng test 50-100 request cạnh tranh trước khi thêm payment.
4. **Fake Bank/payment/refund:** service boundary riêng, signed callback, replay/late handling, reconciliation,
   compensation và outbox. Sau đó check-in/out/review và các nhánh vận hành.
5. **Các phân hệ mở rộng:** messaging, analytics/Redis, email/policy/reminder, audit và monitoring;
   dùng tiêu chí nghiệm thu tương ứng trong PDF thay vì chỉ thêm entity/API.

## 8. Minh chứng kiểm thử và giới hạn

Kết quả và cách chạy probe được ghi trong [audit/VERIFICATION.md](audit/VERIFICATION.md).
Mã probe: [audit/SpecificationAuditTest.java](audit/SpecificationAuditTest.java).
Đã chạy 14 test trạng thái tài khoản: tất cả đạt. Sáu probe theo đặc tả: 1 đạt (binding Servlet Part),
5 failure tái hiện F01, F02, F04, F05 và F06; không có test error. Lệnh probe trả BUILD FAILURE vì các assertion này.
Probe nằm ngoài `src/test` để các kiểm tra đang phát hiện thiếu sót chưa xử lý không làm thay đổi test suite mặc định.

Các probe service dùng repository/MinIO giả; các probe HTTP dùng SecurityFilterChain thật và service giả.
Chúng chứng minh hành vi mapping/authorization ở các boundary được kiểm tra, không chứng minh dữ liệu
đã persist thành công trên PostgreSQL thật hoặc object đã được upload vào MinIO thật.
`UserAccountStatusTest` dùng JWT ký thực với khóa tạm cho test, nhưng repository vẫn giả.

Không chạy toàn bộ `DatphongApplicationTests.contextLoads` vì cấu hình hiện tại trỏ DB local với `ddl-auto:update`;
đó không phải integration test được cô lập. Chưa dựng DB sạch, chưa thử API ghi vào DB thật,
chưa kiểm thử FE, không có số đo throughput/latency và chưa audit từng cột của toàn bộ 52 entity.

Vì các khoảng thiếu ở booking và integration, chưa thể kết luận hoàn thành bất kỳ luồng nghiệm thu xuyên phân hệ
trong mục 12.1/12.4 của PDF, dù một số thành phần Identity đã có test đạt.
