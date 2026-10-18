# Đối chiếu toàn dự án với đặc tả nghiệp vụ v3.0

Ngày rà soát: **16/10/2026**. Phạm vi: working tree hiện tại, bao gồm các thay đổi đã và chưa commit.

Nguồn yêu cầu: [PDF nghiệp vụ](Dac_ta_he_thong_dat_phong_spring.pdf), **51 trang, 36 use case**; các quyết định trong [ADR](DB_SPEC_ARCHITECTURE_DECISIONS.md) và [catalog entity](DB_SPEC_CORE_ENTITY_CATALOG.md). Số trang trong báo cáo là số trang PDF. Tên class, bảng và API gợi ý trong PDF không được dùng làm điều kiện đánh trượt nếu hành vi tương đương đã được triển khai.

## 1. Kết luận

Dự án đã có nền tảng model/schema khá rộng, nhưng phần xử lý nghiệp vụ còn thiếu nhiều. Hiện chưa thể chạy trọn luồng đăng ký → công bố phòng → tìm/quote → giữ chỗ → thanh toán → lưu trú → đánh giá theo PDF.

| Mức đánh giá theo toàn bộ use case | Số UC | Giải thích |
|---|---:|---|
| Đủ bằng chứng đáp ứng toàn bộ UC | 0 | Chưa có UC nào được kiểm chứng đủ luồng chuẩn, quyền, ngoại lệ và tiêu chí nghiệm thu |
| Có một phần xử lý đang hoạt động | 7 | UC-02, 03, 05, 06, 07, 08, 20 |
| Chưa có luồng nghiệp vụ thực thi | 29 | Có thể đã có entity/enum/DTO, nhưng chưa có service/API/worker thực hiện UC |

Đây là **phân loại độ phủ hành vi**, không phải phần trăm tiến độ. Có 74 entity không đồng nghĩa hoàn thành 74 tính năng. Những phần đã làm đúng được ghi nhận riêng, kể cả khi toàn UC chưa đạt.

## 2. Những gì đã kiểm tra và đã có

- Đọc lại 51 trang PDF, gồm luồng chuẩn/ngoại lệ UC-01 đến UC-36, state machine, mô hình dữ liệu, yêu cầu kỹ thuật và các kịch bản nghiệm thu. Kiểm tra trực quan bảng trạng thái trang 38.
- Kiểm kê controller, service, repository, mapper, DTO, entity/enum, security, exception, AOP, cấu hình, SQL/bootstrap và test. Phân biệt code hoạt động với class rỗng/code bị comment.
- Có **3 REST controller hoạt động, 12 mapping HTTP; 74 entity, 67 enum, 68 DTO và 20 mapper**. Repository có 7 interface JpaRepository, cùng các interface/implementation custom. Không bắt buộc tạo riêng repository hoặc endpoint CRUD cho mọi entity.
- PostgreSQL `localhost:5435/vivu` hiện có **74 bảng public, tất cả rỗng**. Đã đọc metadata constraint/index và chạy kiểm tra Hibernate mapping/SELECT từng entity. Không chạy DDL, không chèn dữ liệu nghiệp vụ hay upload MinIO trong lượt audit này.
- Bộ test mặc định: **19 test đạt, 3 test bỏ qua**, không failure/error. Kiểm tra mapping DB chỉ đọc chạy riêng: **1 test đạt**. Probe theo PDF: **11 test, 2 đạt, 9 assertion không đạt, 0 error**. Chi tiết tại [minh chứng](audit/VERIFICATION.md).

Các phần được ghi nhận:

| Phần đã có | Bằng chứng và giới hạn |
|---|---|
| Login bằng AuthenticationManager/BCrypt và JWT | [AuthenticationService](../src/main/java/com/vn/datphong/service/AuthenticationService.java), [SecurityConfig](../src/main/java/com/vn/datphong/config/SecurityConfig.java). Chưa có đăng ký/refresh/logout |
| Kiểm tra trạng thái tài khoản ở login và request JWT | [CustomUserDetailsService](../src/main/java/com/vn/datphong/service/Impl/CustomUserDetailsService.java), [JwtAuthenticationFilter](../src/main/java/com/vn/datphong/security/JwtAuthenticationFilter.java). 16 test trạng thái đạt, gồm PENDING_VERIFICATION, tài khoản khóa và đã xóa |
| ADMIN/chính chủ ở service user | [UserServiceImpl](../src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java#L21), [UserAuthorization](../src/main/java/com/vn/datphong/security/UserAuthorization.java). Chưa có permission granular và workflow quản trị quyền |
| Response user không trả password/secret 2FA | [UsersResponse](../src/main/java/com/vn/datphong/dto/response/UsersResponse.java), [UserMapper](../src/main/java/com/vn/datphong/mapper/UserMapper.java#L30). Không đồng nghĩa password được hash khi tạo user |
| Search có filter, phân trang, whitelist sort và điều kiện overlap nửa mở | [RoomServiceImp.list](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java#L54), [RoomRepository](../src/main/java/com/vn/datphong/repository/RoomRepository.java). Còn thiếu quyền public, visibility và nguồn tồn/giá chuẩn |
| API detail/quote và cộng giá từng đêm | `GET /api/rooms/{id}/detail-quote`, [RoomServiceImp.getDetailAndQuote](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java#L215). Đã dùng roomTypeId, BigDecimal, [checkIn,checkOut), báo thiếu giá; probe ba đêm giá khác nhau đạt |
| Khung MinIO upload/remove/presigned GET | [MinioServiceImpl](../src/main/java/com/vn/datphong/service/Impl/MinioServiceImpl.java). Chưa có lifecycle/finalize/metadata/cleanup hoàn chỉnh |
| Model/schema cho các phân hệ lõi và hỗ trợ | Có token, allocation, refund, webhook inbox, reconciliation, audit, chat, complaint, config, mail/reminder/report entities. Kiểm tra Hibernate mapping toàn bộ 74 entity đạt, chưa chứng minh state machine/service tương ứng |
| Các chốt DB quan trọng | Metadata xác nhận email unique chuẩn hóa, exclusion allocation chưa release, unique amenity code, CHECK ngày/tiền booking và reservation refund. Chưa có test cạnh tranh chứng minh toàn workflow |
| Dựng DB rỗng và quản lý DDL thủ công | [khoi-tao-db.cmd](../khoi-tao-db.cmd), V000/V001/V002, checksum, runner ghi phiên bản trong schema kỹ thuật; `ddl-auto: validate`. Khác Flyway theo ngoại lệ đã ghi ở ADR, không phải còn thiếu hoàn toàn bootstrap |

## 3. Các thiếu sót cần ưu tiên

### F01 — P0: Tạo user chưa hash mật khẩu

PDF UC-01/02, trang 4–6. [UserServiceImpl.create:44](../src/main/java/com/vn/datphong/service/Impl/UserServiceImpl.java#L44) gọi mapper rồi save; [UserMapper:23](../src/main/java/com/vn/datphong/mapper/UserMapper.java#L23) gán trực tiếp password đầu vào. Đổi tên cột thành `password_hash` không tự hash nội dung. Probe xác nhận giá trị đưa vào repository vẫn bằng password test. Đây là API tạo user dành ADMIN, chưa phải endpoint đăng ký công khai; dù vậy vẫn phải lưu hash đúng.

**Cần làm:** encode ở application service, tách DTO đăng ký/admin/self, chuẩn hóa email, gắn role hợp lệ từ server và kiểm thử đồng thời email trùng. Không dùng password đã encode giả để bỏ qua kiểm thử luồng tạo tài khoản.

### F02 — P0: Mutation phòng/media chưa kiểm tra HOST và owner scope

PDF UC-04/05/06, trang 7–9,45. [SecurityConfig](../src/main/java/com/vn/datphong/config/SecurityConfig.java) chỉ yêu cầu authenticated cho URL phòng; [RoomController](../src/main/java/com/vn/datphong/controller/RoomController.java) và RoomServiceImp không kiểm tra HOST/owner. Probe CUSTOMER gọi create nhận **201 thay vì 403**. Service persistence được giả lập trong probe: kết quả này chứng minh thiếu authorization, không chứng minh đã ghi phòng thật vào DB.

**Cần làm:** kiểm tra chức năng ở service và ownership qua quan hệ property thật, chặn sửa giá/ảnh/phòng của HOST khác; kiểm tra lại quyền trong query nhạy cảm. `roomImage.room.id == roomId` chỉ chứng minh media thuộc phòng, không chứng minh principal sở hữu phòng.

### F03 — P0: Chưa có workflow booking/inventory/payment/expiry

PDF UC-09→15,35/36. [BookingController](../src/main/java/com/vn/datphong/controller/BookingController.java#L17) rỗng; [BookingServiceImpl](../src/main/java/com/vn/datphong/service/Impl/BookingServiceImpl.java) bị comment toàn bộ. BookingService chỉ có interface. Chưa có application service ghi booking, nightly snapshot, allocation, history, idempotency và outbox trong một transaction.

Exclusion constraint đã có là nền tảng tốt, nhưng không tự tạo/giải phóng allocation hay giải quyết callback muộn. Chưa có state transition, thứ tự lock, scheduler expiry, signed webhook, đối soát và bù trừ để nghiệm thu luồng đặt phòng.

**Cần làm:** xây một luồng giữ chỗ hoàn chỉnh trước, kiểm thử 50–100 request cạnh tranh và rollback một booking night; sau đó mới nối Fake Bank và các nhánh phục hồi.

### F04 — P1: Sửa phòng không sửa entity

PDF UC-05, trang 8. [RoomServiceImp:114–121](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java#L111) gọi `req.setName(req.getName())` và tương tự cho các trường khác, rồi save room cũ. Probe đổi tên vẫn trả tên cũ.

**Cần làm:** cập nhật whitelist field trên entity, kiểm tra version, quyền, xung đột booking khi đóng bán/giảm sức chứa; invalidate cache sau commit.

### F05 — P1: Upload không lưu metadata/liên kết media

PDF UC-06, trang 8–9. [RoomServiceImp.uploadMedia](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java#L137) upload object rồi trả raw URL, không tạo MediaAsset hoặc RoomImage; probe xác nhận không gọi lưu RoomImage. Listing lại đọc ảnh từ RoomImageRepository nên upload chưa nối được vào nguồn hiển thị ảnh.

Còn thiếu kiểm tra quyền, lifecycle PENDING→READY, HEAD/checksum/MIME thực tế, giới hạn số ảnh, finalize, presigned view và orphan cleanup. `@Transactional` đang bao quanh stream MinIO. Xóa media lấy key từ URL và xóa object trước DB, chưa có DELETING/outbox phục hồi. Validation BusinessException trong khối try còn bị bọc thành RuntimeException, có thể trở thành HTTP 500 thay vì lỗi input.

**Cần làm:** lưu metadata theo ADR, tách transaction ngắn khỏi I/O, gắn asset với phòng và thực hiện cleanup/retry. Probe MockPart đạt chỉ chứng minh binding servlet, không chứng minh storage/lifecycle đúng.

### F06 — P1: Search dùng nguồn tồn/giá chưa đúng và chưa công khai

PDF UC-07, trang 9–10. GUEST search nhận **401 thay vì 200**. [RoomRepository.searchRoomsWithAvailability](../src/main/java/com/vn/datphong/repository/RoomRepository.java) không ép property PUBLISHED/room ACTIVE, không đọc allocation/sale blocks/closed_to_sale; loại HOLD mà không xét expiry nhưng lại bỏ qua PENDING_PAYMENT. BookingRepository có tập status khác và cũng không xét expiry; hiện chưa có booking service gọi để bảo vệ ghi tồn.

Filter giá dùng `rooms.pricePerNight` thay vì giá theo room_rates của kỳ lưu trú. Thiếu filter địa điểm/amenity, giới hạn size/date range/giá/số khách, timezone property, sort có id làm khóa phụ, cache và invalidation.

**Cần làm:** dùng cùng mô hình inventory/giá với booking, tách public query khỏi quản lý nội bộ. Nạp ảnh theo từng room có nguy cơ N+1. Với `open-in-view=false`, RoomMapper truy cập quan hệ LAZY trong list không có transaction/fetch phù hợp còn có nguy cơ LazyInitializationException; chưa tái hiện lỗi lazy bằng dataset thật trong lượt này.

### F07 — P1: Hồ sơ và quản trị user chưa hoàn chỉnh

PDF UC-03/20, trang 6–7,21. DTO chung [UsersResquest](../src/main/java/com/vn/datphong/dto/request/UsersResquest.java) nhận status/active/roleId và yêu cầu password ngay cả ở cập nhật hồ sơ. Service hiện chỉ sửa một số trường hồ sơ, không cấp role hay đổi status; không kết luận có mass assignment thành công chỉ vì DTO nhận field, nhưng contract DTO chưa đáp ứng PDF.

User đã có `@Version` nhưng request chưa mang version và chưa có xử lý VERSION_CONFLICT. Chưa có đổi/reset password, role/permission assignment, audit, phân trang user và cơ chế logout/revoke. CustomUserDetailsService chỉ tải role, chưa tải permission granular. RoleController bị comment; RoleService có CRUD nhưng chưa có route chạy hoặc method security riêng. UserService.delete gọi deleteById, chưa có quy trình bảo toàn đối tượng có lịch sử.

**Cần làm:** tách self/admin/role DTO và API, gắn principal, dùng optimistic version ở contract cập nhật, kiểm tra lịch sử trước xóa và audit thay đổi quyền/trạng thái.

### F08 — P1: Quote mới có một phần, chính sách và phụ phí chưa lấy từ dữ liệu

PDF UC-08/30, trang 10–11,30–31. Phần cộng giá từng đêm và query theo roomTypeId đã có, nhưng [RoomServiceImp:218–247](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java#L218) chưa kiểm tra property PUBLISHED/room type active/sale blocks. Dùng Room.capacity thay vì sức chứa chuẩn từ room type và LocalDate.now theo server. Probe tái hiện trả chi tiết property DRAFT, nhận 0 khách và chấp nhận cặp ngày thiếu một đầu.

Thuế/phí cố định **10%**. [RoomRateMapper:115–116](../src/main/java/com/vn/datphong/mapper/RoomRateMapper.java#L115) trả câu “15 phút” và chính sách “miễn phí trước 2 ngày, hoàn 50%” viết cứng, không đọc chính sách thật. Response chưa có breakdown từng stayDate, instant expiry, priceVersion/quote identity, cancellation revision/effective time, tiện ích/đánh giá đầy đủ. DTO có tên field không đồng nghĩa các giá trị đó được chốt đúng.

**Cần làm:** đọc chính sách/fee hiệu lực, trả breakdown/expiry/version có thể kiểm tra lại, validate ngày/khách/visibility và nối việc tính lại giá vào transaction booking. Quote không được coi là giữ tồn.

### F09 — P1: Script chuẩn hóa user cũ xung đột với model hiện tại

PDF UC-01, trang 4–5; ADR mục A.7 chọn bốn trạng thái. [normalize-user-status.sql](../scripts/sql/normalize-user-status.sql) vẫn UPDATE `PENDING_VERIFICATION → IN_ACTIVE`, CHECK đúng ba mã và đổi default thành IN_ACTIVE. [README](../README.md) vẫn hướng dẫn chạy script này và mô tả enum ba giá trị, trong khi UserStatus/User builder/baseline đã có PENDING_VERIFICATION.

**Hệ quả nếu chạy:** làm mất phân biệt chờ xác minh trên các hàng liên quan, đồng thời chặn insert PENDING_VERIFICATION trong luồng đăng ký mới. `ddl-auto:validate` không chứng minh CHECK chứa đúng tập mã enum. Đây là kết luận từ SQL/code, **chưa chạy script này** trong lượt audit.

**Cần làm:** ngừng coi hướng dẫn cũ là bước sau bootstrap mới, thiết kế migration phiên bản kế tiếp nếu phải chuyển dữ liệu cũ và đồng bộ README/test. Lượt rà soát không tự sửa SQL hay trạng thái tài khoản.

### F10 — P2: Chuẩn lỗi, trace, secrets và vận hành chưa đủ

PDF trang 40–46,49–50. Hai [controller advice](../src/main/java/com/vn/datphong/exception) xử lý trùng loại lỗi với body khác nhau; security dùng sendError; ApiResponse thiếu traceId; lỗi nghiệp vụ gom BUSINESS_ERROR, chưa ánh xạ đủ unique/conflict/lock errors. Handler tổng quát trả ex.getMessage, cần lọc chi tiết nội bộ trước trả client.

User.tokenVersion đã có nhưng chưa được JWT/service sử dụng. Chưa có rate limiter, correlation filter, audit writer/query API, retry/claim worker, Actuator/Micrometer/Prometheus/Grafana hay CI trong repo. Redis mới có config bean, chưa có cache nghiệp vụ. application.yaml/Compose vẫn có credential/secret mặc định trong source; chưa có cơ chế tách secret bắt buộc theo môi trường và rotation. Không chép giá trị bí mật vào báo cáo.

## 4. Ma trận đủ 36 use case và đầu việc còn lại

“Khung dữ liệu” dưới đây chỉ ghi nhận entity/DTO hiện có; không tính là service đang hoạt động. Các ca kiểm thử là điều kiện cần bổ sung, không phải kết quả đã đạt.

| UC / trang PDF | Mức | Hiện có | Thiếu để đáp ứng / ca nghiệm thu cần có |
|---|---|---|---|
| UC-01 / 4–5 — Đăng ký, xác minh | Chưa có luồng | User bốn trạng thái, VerificationToken, DTO đăng ký, unique email DB | Register/verify/resend, CUSTOMER mặc định, hash/chuẩn hóa email, token hash/expiry/attempts, outbox. Test email đồng thời, token dùng lại/hết hạn. F01/F09 |
| UC-02 / 5–6 — Login, refresh, logout | Một phần | Login/JWT/trạng thái, RefreshToken model | Login email chuẩn hóa, rate limit, refresh rotation/reuse, logout/revocation/correlation; hiện dùng username. Test rotation và phiên bị thu hồi |
| UC-03 / 6–7 — Hồ sơ/phạm vi quyền | Một phần | API user, ADMIN/chính chủ, DTO response an toàn, User.version | DTO self/admin riêng, đổi mật khẩu, cấp/thu quyền, version contract, audit, xử lý xóa có lịch sử. Test sai owner/mass assignment/version conflict. F07 |
| UC-04 / 7 — Property/publish | Chưa có luồng | Property, mapper, status/version/timezone/policy | API/service create/update/publish, owner từ principal, kiểm tra đủ giá/phòng/ảnh/chính sách, pause/archive, cache/audit. Test publish thiếu dữ liệu và sai HOST |
| UC-05 / 8 — Loại phòng/phòng/giá/lịch bán | Một phần | API Room create/update, RoomTypeEntity/RoomRate/RoomSaleBlock và mapper | HOST scope, gắn FK property/type, sửa entity đúng, CRUD rate/amenity/block, version, kiểm tra booking trước đóng bán. Test F02/F04 và quan hệ bắt buộc |
| UC-06 / 8–9 — Media | Một phần | Endpoint stream upload/delete, helpers MinIO, MediaAsset/RoomImage | PENDING/READY/finalize, metadata, ownership, kiểm tra tệp thật, private view/cleanup, I/O ngoài transaction. Test DB lỗi sau upload, object mồ côi. F05 |
| UC-07 / 9–10 — Search | Một phần | Filter ngày/giá/sức chứa, page/sort, overlap | Public visibility, inventory và rate chuẩn, địa điểm/amenity, expiry, timezone/size limits, cache/invalidation, fetch phù hợp. Test hold hết hạn, block, thiếu giá. F06 |
| UC-08 / 10–11 — Detail/quote | Một phần | Endpoint detail-quote, cộng BigDecimal từng đêm theo room type | Policy/fees động, breakdown ngày, expiry/version, visibility/capacity/timezone, sale blocks; không giữ kho. Test ba đêm khác giá đã đạt, còn F08 |
| UC-09 / 11–12 — Booking/hold | Chưa có luồng | Booking/Night/Allocation/History/Idempotency/Outbox, daterange và constraint | Service transaction, lock room, snapshots, idempotency/hash, error mapping. Test 100 request cùng đêm chỉ một thành công, rollback night và retry cùng key. F03 |
| UC-10 / 12–13 — Payment/webhook | Chưa có luồng | Payment many-to-one Booking, provider/reference fields, BankWebhookEvent | Intent service, gọi bank ngoài DB transaction, HMAC/replay/amount checks, inbox claim, state transitions, duplicate/late callback recovery |
| UC-11 / 13–14 — Expiry job | Chưa có luồng | Booking.expiresAt, allocation.release và status enums | Scheduler/lease hoặc SKIP LOCKED, recheck payment/trạng thái, giải phóng đúng một lần, history/outbox/metrics. Test hai scheduler đua webhook |
| UC-12 / 14–15 — Hủy/tính phí | Chưa có luồng | Booking cancellation snapshot/fee/refundable fields, policy/rules | Preview/confirm theo snapshot/timezone, lock/idempotency, quyết định release/refund và audit. Test trước/sau deadline, hủy chưa trả tiền, retry |
| UC-13 / 15–16 — Refund/đối soát | Chưa có luồng | Refund, reservation fields, ReconciliationCase | Lock payment và reserve hạn mức, worker/provider idempotency, callback/retry/reconcile, SUPPORT xử lý. Test tổng đã hoàn + đang giữ không vượt captured |
| UC-14 / 16–17 — HOST xử lý booking | Chưa có luồng | Booking/response/history | List/detail owner-scoped, masking khách, whitelist ghi chú, version/audit/notification. Test HOST khác và cấm sửa nghĩa vụ tiền |
| UC-15 / 17 — Check-in/out | Chưa có luồng | Booking actual timestamps/actors/status | Service chuyển trạng thái, kiểm tra property/time window/payment, idempotent history/outbox. Test nhận sai ngày, trả khi chưa nhận, thao tác đồng thời |
| UC-16 / 17–18 — Review | Chưa có luồng | Review/ReviewMedia, unique booking, rating CHECK, DTO/mapper | Eligibility COMPLETED/owner, kiểm duyệt, ẩn/khôi phục, phản hồi HOST, audit/cache. Mapper còn dùng VISIBLE cũ. Test hai review cùng booking |
| UC-17 / 18–19 — Notification/outbox | Chưa có luồng | Notification/OutboxEvent, dedupe/lease/retry fields | Producer cùng transaction, consumer claim/backoff, recipient-scoped list/read. Test crash sau tạo notification trước mark outbox không nhân đôi |
| UC-18 / 19–20 — Báo cáo cơ bản | Chưa có luồng | Response/mapper và model nguồn tiền/đêm | Aggregate SQL theo scope, phân biệt booking value/captured/refund/net, trục thời gian/asOf và drill-down. Test một booking nhiều night không nhân tiền |
| UC-19 / 20–21 — Audit | Chưa có luồng | AuditLog và LoggingAspect | Durable audit writer/masking cùng transaction, quyền/query/cursor/retention, request/event linkage. Logging thời gian không phải audit nghiệp vụ |
| UC-20 / 21 — Admin/quyền/danh mục | Một phần | ADMIN guard API user, role CRUD service, Permission/RolePermission/Amenity models | API role/permission/status/amenity, granular policy, revoke phiên, audit, pagination, bảo vệ lịch sử. RoleController hiện không hoạt động |
| UC-21 / 22–23 — Dashboard | Chưa có luồng | Nguồn dữ liệu booking/finance và AnalyticsRefreshState tùy chọn | KPI dictionary, owner-scoped aggregate, comparison period, Redis isolation/version, asOf/source/lag. Test HOST A không đọc cache HOST B |
| UC-22 / 23–24 — Booking analytics | Chưa có luồng | Booking sourceChannel/guestSegment snapshots | Multi-filter/grouping whitelist, time axis, count đúng grain, comparison/drill-down. Test một booking ba đêm = một booking, ba room-nights |
| UC-23 / 24–25 — Cash/refund analytics | Chưa có luồng | Payment.capturedAt/Refund fields và nightly snapshots | Aggregate facts riêng tránh nhân JOIN, net cash/partial refund/cutoff/asOf, invalidation. Test hủy/hoàn xuyên kỳ và callback muộn |
| UC-24 / 25 — Occupancy/ADR/RevPAR | Chưa có luồng | Rooms/allocations/blocks/rates/nights | Date spine, sellable/occupied definitions, mẫu số đúng, weighted ratios, timezone và cache. Test phòng bảo trì một phần kỳ, mẫu số 0 |
| UC-25 / 25–26 — Funnel/lỗi/hủy | Chưa có luồng | UserBehaviorLog/FunnelEventType, event-time/ingested-time fields | Event producers/dedupe/session attribution, SQL funnel/percentile, failure classification, coverage/masking. HI/HIE/HIA cũ chưa có định nghĩa |
| UC-26 / 26–27 — Query/export/cache | Chưa có luồng | ReportExport, metadata aggregate tùy chọn, Redis config | Query whitelist/bind/limits, scope-aware keys, cache invalidation/fallback/stampede, export worker/CSV safety/TTL/audit. Fact tables chỉ cần theo workload |
| UC-27 / 27–28 — Conversation | Chưa có luồng | Conversation/Participant, unique active key và version | Create/reuse, owner/participant xác minh từ DB, rate limit, first message+outbox, inbox/cursor, xử lý đổi chủ property |
| UC-28 / 28–29 — Message/complaint | Chưa có luồng | Message purpose/content/status/seq, ReadReceipt, ComplaintTicket, dedupe key có sender | Send/read APIs, seq concurrency, participant scope, sanitize/media, complaint SLA/escalation/audit. Test gửi trùng key khác payload và read race |
| UC-29 / 29–30 — Hẹn check-in | Chưa có luồng | CheckinRequest, proposed/counter/agreed timestamps/version | Propose/respond/accept/close, khóa/version, liên kết message, actual check-in riêng, NO_SHOW và cập nhật reminder. Test hẹn khác giờ thực tế |
| UC-30 / 30–31 — Policy/config | Chưa có luồng | ConfigDefinition/Value/ChangeLog, scope/type/version | Effective policy resolution, validation/floor, admin MFA cho thay đổi nhạy cảm, audit/cache, public policy summary. TENANT chưa được phép ghi nếu chưa xác minh tenant |
| UC-31 / 31–32 — Email template | Chưa có luồng | EmailTemplate/Version, allowed/required variables, locale/version | Admin preview/publish/test send, immutable version, safe renderer/HTML escaping/fallback. Test thiếu biến và publish mới khi mail cũ còn queue |
| UC-32 / 33 — Email giao dịch | Chưa có luồng | MailEvent/DeliveryAttempt, event type và snapshot/dedupe fields | Outbox producer, mail worker/gateway/SMTP fake, renderer, retry classification. Test SMTP timeout không rollback booking/refund và không nhân business event |
| UC-33 / 34–35 — Nhắc check-in | Chưa có luồng | ScheduledReminder với windowCode/policyVersion/dueAt/cutoff | Tính lịch theo timezone/giờ hẹn, DB claim, dedupe, cancel/recalculate, backlog metrics. Test nhiều instance, DST, booking hủy trước nhắc |
| UC-34 / 35 — Email operations | Chưa có luồng | MailEvent/Attempt/DeadLetter | SUPPORT search/resend có quyền/audit/idempotency, backoff/DLQ/UNKNOWN, masking, cảnh báo. Test timeout sau gửi và hai SUPPORT resend cùng lúc |
| UC-35 / 35–37 — Fake Bank độc lập | Chưa có luồng | Phía Booking có Payment/Refund/inbox/reconciliation models | Service/schema bank riêng, accounts/transactions/double-entry ledger, HMAC/outbox và fault simulator. Wallet không thay bank ledger. Test debit trùng, thiếu tiền, lost/late webhook |
| UC-36 / 37–38 — Saga/bù trừ | Chưa có luồng | State/fields/idempotency/reservation của Booking/Payment/Refund | Orchestration, lock order, UNKNOWN/reconcile, compensation, outbox và fault injection xuyên hai transaction boundary. Test bank captured + local DB fail + refund idempotent |

## 5. Mô hình, enum và nguồn sự thật

Các lựa chọn sau đã được ADR cho phép, không yêu cầu tạo thêm bảng chỉ để giống tên PDF:

- Phòng vật lý lõi là `rooms`; RoomUnit/RoomCalendar giữ tương thích, không làm kho độc lập. RoomType enum cũ không thay RoomTypeEntity.
- Giá lõi là RoomRate theo room type/ngày; việc search còn dùng pricePerNight và quote còn lấy chính sách giả là lỗi tích hợp, không phải thiếu thêm entity giá.
- Một hàng Payment biểu diễn một intent/giao dịch logic, nhiều Payment thuộc một Booking; không bắt buộc tách payment_intents. BankWebhookEvent là inbox tương ứng payment events.
- Refund là giao dịch hoàn tiền chuẩn; RefundRequest cũ không tạo workflow hoàn tiền lõi song song. OtpVerification/SystemSetting/SupportTicket cũng phải theo lộ trình tương thích, không thành nguồn chuẩn thứ hai.
- Config.allowedScopes và policy snapshots có thể là JSON/value object, không buộc biến mọi giá trị thành entity/enum. Tenant chưa có mô hình xác minh thật.
- AI, VIP, loyalty, voucher, wallet, FX, collections và các extension được giữ theo ADR nhưng không dùng để thay điều kiện của 36 UC hoặc làm phụ thuộc bắt buộc của booking lõi.
- Enum có đủ mã và có Javadoc chỉ giúp hiểu model. Còn phải triển khai transition, quyền và side effect; không tự đồng nhất SUCCESS/SUCCEEDED, VISIBLE/PUBLISHED hoặc các mã cũ khác.
- Constraint DB chỉ bảo vệ được điều kiện tại tầng dữ liệu. Ownership, tổng reservation nhiều refund, snapshot đúng thời điểm, tương thích property/type/room, tenant scope và nghĩa từng transition còn cần service + transaction + test.

## 6. Yêu cầu kỹ thuật, ngoại lệ và giới hạn bằng chứng

| Yêu cầu PDF | Hiện tại / đầu việc |
|---|---|
| Java 21 + Spring Boot 3.x — trang 40/50 | pom.xml và Dockerfile dùng Java 17/Boot 4.1.1. ADR xác định khác biệt còn cần phê duyệt, chưa ghi nhận ngoại lệ stack đã được duyệt. Chọn thống nhất stack hoặc bổ sung ngoại lệ có căn cứ, không tự đổi trong lượt audit |
| Flyway — trang 40/49 | ADR A.12 chấp thuận SQL thủ công thay Flyway. Đã có V000 hoặc V001, sau đó V002, checksum/version và một lệnh chạy; không đánh dấu thiếu bootstrap như báo cáo cũ. Runner khởi tạo không phải công cụ migrate DB đã có dữ liệu |
| PostgreSQL là nguồn đúng, lock/constraint — trang 38–41 | Mapping và constraint quan trọng đã có; chưa có luồng ghi inventory hoặc concurrency/fault tests để khẳng định không bán trùng |
| Redis cache/rate limit — trang 10/27/40/43 | Mới có bean/config/Compose; chưa có cache key scope, TTL, invalidation/fallback hay rate limiter |
| MinIO lifecycle — trang 9/41 | Có helper storage, còn F05. Chưa thử MinIO thật hoặc cleanup khi DB down trong lượt này |
| Audit/metrics/tracing — trang 20–21/40–46 | Có model audit và logging aspect, chưa có writer/query/correlation, Actuator/Micrometer hoặc stack Prometheus/Grafana |
| Integration tests/Testcontainers — trang 40/45–46/49–50 | Có test mapping trên PostgreSQL local và schema export. Chưa có Testcontainers dependency, fixture nghiệp vụ cô lập và test luồng quyền/tồn/tiền thật |
| Hiệu năng/phục hồi — trang 45–51 | Chưa có EXPLAIN ANALYZE, dataset/load test p95/p99, cache isolation/stampede, chaos/reconciliation/scheduler nhiều instance. Không suy ra SLA từ build pass |
| Bàn giao — trang 49–50 | Có ADR/catalog/README/SQL/Swagger; cần sửa hướng dẫn user status cũ, bổ sung API-role matrix đầy đủ, state/transaction diagrams, fixtures riêng và runbook backup/restore/recovery |

Chưa kiểm chứng FE, dịch vụ nằm ngoài repository, triển khai production, Docker startup trong môi trường này hoặc tính tương thích trên máy đồng đội. DB hiện rỗng nên SELECT/mapping pass không chứng minh câu truy vấn, số liệu tài chính hoặc quyền đúng trên dữ liệu thực tế. Ba test persistence mở rộng bị skip ở lượt mặc định; chỉ test mapping SELECT được bật chạy riêng, không gộp thành đã chạy cả ba.

## 7. Thứ tự triển khai đề xuất

1. **Chặn lỗi an toàn và xung đột bàn giao:** F01/F02/F09, DTO self/admin, error mapping cơ bản. Nghiệm thu: password lưu hash, CUSTOMER/HOST sai scope bị chặn, PENDING_VERIFICATION được giữ đúng và đăng ký không tự cấp role.
2. **Hoàn thiện catalog/search/quote/media:** F04/F05/F06/F08, property publish, rates/policy/block. Nghiệm thu: HOST công bố dữ liệu hợp lệ, GUEST tìm/xem, ba đêm giá khác nhau được giải thích, upload nối metadata, không lộ property DRAFT.
3. **Booking/inventory một luồng hoàn chỉnh:** UC-09/11, snapshot/idempotency/history/outbox. Nghiệm thu: 100 request cạnh tranh chỉ một booking giữ phòng, rollback night không để allocation, retry cùng key trả cùng kết quả.
4. **Fake Bank/payment/refund/saga:** UC-10/12/13/35/36. Nghiệm thu: double-entry cân bằng, debit/refund không trùng, callback muộn không xác nhận booking hết hạn, local DB lỗi được reconcile/bù trừ.
5. **Stay và dịch vụ theo sự kiện:** UC-14→17/19/27→34. Messaging/policy/email có thể triển khai theo module, nhưng chỉ tính hoàn tất khi nối được quyền, transaction/outbox, worker và test.
6. **Analytics/admin/operations:** UC-18/20→26 và monitoring. Chốt KPI grain/time axis trước query/cache; nghiệm thu bằng test dữ liệu nhiều kỳ, scope isolation, EXPLAIN và số đo tải.

Không cần viết thêm toàn bộ entity một lần nữa. Ưu tiên nối các model đã có thành các use case có thể chạy và kiểm chứng.

## 8. Tài liệu và khả năng tái hiện

- [Minh chứng kiểm thử và lệnh chạy](audit/VERIFICATION.md).
- [11 probe nghiệp vụ](audit/SpecificationAuditTest.java), giữ ngoài source set mặc định để chưa làm suite chuẩn thất bại bởi các thiếu sót đã biết.
- [EntitySchemaTest](../src/test/java/com/vn/datphong/persistence/EntitySchemaTest.java) và [UserAccountStatusTest](../src/test/java/com/vn/datphong/security/UserAccountStatusTest.java).
- Log/XML/metadata lượt này: `target/spec-audit/`, `target/surefire-reports/*spec-audit*.xml`; đây là output build có thể bị xóa bởi clean, bảng kết quả trong tài liệu là bản ghi bền vững.

Lượt rà soát chỉ bổ sung/cập nhật tài liệu và probe audit. Không sửa logic sản phẩm, cấu hình chạy, SQL, enum hay DB.
