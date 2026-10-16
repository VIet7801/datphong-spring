# Quyết định kiến trúc Database — hệ thống đặt phòng Spring

**Ngày chốt đề xuất:** 16/10/2026  
**Nguồn đã đọc:** `datphong 16-10.zip`: `docs/DB_SPEC_COMPARISON.md`, `docs/SPEC_COMPLIANCE_AUDIT.md`, `Dac_ta_he_thong_dat_phong_spring.pdf` (v3.0, 51 trang), các JPA entity/enums và `application.yaml`.  
**Phạm vi xác minh:** đọc file trong ZIP và báo cáo metadata PostgreSQL đã ghi trong `DB_SPEC_COMPARISON.md`; **không kết nối được PostgreSQL đang chạy trên máy người dùng và không tự kiểm chứng số dòng, FK, index, constraint thực tế**. Số lượng 62/38/24/12 bên dưới dựa theo báo cáo đó. Đây là **ADR/target design**, không phải chứng nhận DB đã chuyển đổi.

**Rà soát bổ sung 16/10/2026:** đối chiếu lại trực tiếp PDF hiện nằm tại `docs/Dac_ta_he_thong_dat_phong_spring.pdf`, đặc biệt UC-13, UC-28→36 và trang 39→44. Bản cập nhật này chỉ thay đổi tài liệu; không chạy DDL, không sửa entity/config/code và không tái xác minh DB thực tế. Danh sách thuộc tính, quan hệ, enum và constraint đích được ghi tại [DB_SPEC_CORE_ENTITY_CATALOG.md](DB_SPEC_CORE_ENTITY_CATALOG.md). Các tên trường/enum bổ sung trong catalog là lựa chọn thiết kế để thực hiện yêu cầu, không phải khẳng định PDF bắt buộc từng tên class/cột.

## A. Quyết định bắt buộc

1. **Giữ toàn bộ 9 bảng mục 3.3 và cho phép tính năng mở rộng**, không xóa bảng hay entity của chúng. Nhưng API/flow của 36 UC **không được phụ thuộc** AI, VIP, loyalty, voucher, wallet, FX hoặc collections. Triển khai extension sau khi kiểm thử luồng lõi.
2. **Phòng vật lý chuẩn = `rooms.id`.** `room_types` là chủng loại/sức chứa. Bảng `room_units` hiện dư nghĩa với `rooms`; giữ vật lý và entity để bảo toàn dữ liệu/khả năng tương thích, nhưng **không làm nguồn đặt phòng thứ hai**. Chặn ghi mới vào `room_units` cho core; nếu UI gọi là “RoomUnit”, service tạo/cập nhật `rooms`. Nếu dữ liệu thực tế chứng minh `room_units` mới là phòng vật lý chính, cần ADR thay thế và migrate **đồng bộ tất cả** FK (`bookings`, rates, calendar, allocations) trước khi triển khai; không đảo mô hình từng phần. Quyết định hiện tại tối thiểu hóa sửa FK vì `Booking.room` và `RoomCalendar.room` đều đã dùng `rooms`.
3. **Giá bán chuẩn = `room_rates` theo `room_type_id + stay_date`.** Không dùng `rooms.price_per_night`, `rooms.base_price` hay `room_calendar.price_override` để tính quote/booking. Các cột cũ tạm để tương thích và dọn sau khi kiểm tra dữ liệu. Giá thiếu một đêm thì báo không đủ giá; snapshot vào `booking_nights` không thay đổi khi HOST sửa giá. Quy tắc đóng bán: `room_rates.closed_to_sale` cho **toàn loại phòng/ngày**; `room_sale_blocks` cho **phòng vật lý/khoảng ngày**. `room_calendar` chỉ là giao diện/read-model tổng hợp trong giai đoạn chuyển tiếp, không được là nguồn giá/tồn độc lập.
4. **Tồn kho chuẩn = `room_allocations` + transaction trên PostgreSQL.** Một allocation còn hiệu lực cho mỗi booking/phòng/khoảng `[check_in, check_out)`; `PENDING_PAYMENT` chưa hết hạn, `CONFIRMED`, `CHECKED_IN` chiếm kho. Giải phóng allocation theo transition idempotent khi `EXPIRED/CANCELLED` (hoặc chính sách hủy cụ thể). Khóa `rooms` theo `PESSIMISTIC_WRITE`, kiểm tra khoảng chồng lấn và ghi allocation trong **cùng transaction**, mọi đường ghi kho dùng cùng thứ tự lock. Nên có exclusion constraint PostgreSQL trên allocation active theo daterange để làm lớp bảo vệ bổ sung khi đã chuẩn hóa `released_at/is_active`, **không** đặt `now()` vào partial constraint/index. Test 100 request cạnh tranh, chỉ một đặt thành công.
5. **Thanh toán chuẩn = `payments` (một booking có nhiều intent theo thời gian).** Thay `Payment.booking @OneToOne` bằng `@ManyToOne`; chọn một hàng `payments` biểu diễn một logical payment intent và cũng ghi nhận kết quả giao dịch của intent đó, `payments.id` là paymentIntentId. Retry HTTP/timeout cùng intent phải dùng lại hàng, idempotency key và request hash cũ; không tạo payment mới cho mỗi lần gọi mạng. Chỉ tạo intent mới khi intent trước đã kết thúc/hết hiệu lực được xác minh và booking vẫn cho phép thanh toán (UC-36, trang 38); không tạo mới khi intent cũ `PROCESSING/UNKNOWN/RECONCILING`, hoặc booking đã được thanh toán đủ. Lock booking khi kiểm tra/tạo intent, chặn nhiều intent mở đồng thời. Unique idempotency theo `(provider, merchant_id, idempotency_key)`, provider reference theo `(provider, merchant_id, provider_reference)` khi đã có giá trị; không dùng key mới để né trạng thái chưa rõ. Chuẩn hóa `SUCCEEDED` (không dùng đồng thời `SUCCESS`), thêm `UNKNOWN/RECONCILING` cho timeout chưa rõ kết quả. Không đánh FAILED hoặc giải phóng hold do timeout đơn thuần; giữ/reconcile đến cutoff rõ ràng theo chính sách expiry, callback muộn phải bù trừ nếu không thể xác nhận booking an toàn. `bank_webhook_events` là inbox tương ứng payment events; không bắt buộc tách thêm bảng `payment_intents` nếu mô hình này đáp ứng đầy đủ contract PDF.
6. **Hoàn tiền chuẩn = `refunds`** chứa giao dịch thực thi gắn payment và booking, amount/status/idempotency/provider reference. `refund_requests` chỉ dùng nếu sau này cần **quy trình yêu cầu/phê duyệt riêng**; không được làm bản sao giao dịch `refunds`. Đề xuất ngừng dùng entity `RefundRequest` hiện tại cho flow UC-13/36, thay bằng entity `Refund`; giữ mã legacy tạm thời khi đang chuyển đổi. Theo UC-13 bước 02 (trang 15), kiểm tra **tổng đã hoàn thành công + hạn mức đang dành cho refund chưa kết thúc + khoản mới <= captured amount**, bằng lock payment và transaction lưu reservation trước khi gọi Bank. Refund `REQUESTED/PROCESSING/UNKNOWN/RECONCILING` và ca chưa rõ kết quả tiếp tục giữ hạn mức; chỉ giải phóng khi xác minh chắc chắn không hoàn và không thể hoàn muộn. Callback success chuyển phần đã giữ sang đã hoàn trong cùng transaction, không cộng cả hai; retry dùng cùng refund/key. Chỉ kiểm tra `SUM(SUCCEEDED)` là không đủ khi nhiều refund đang xử lý song song.
7. **Xác minh = `verification_tokens`, refresh = `refresh_tokens`.** Không tạo token thật dạng plaintext trong DB. Gộp chức năng `OtpVerification` cũ vào `VerificationToken`; `otp_verifications` không được là một nguồn token thứ hai. PDF UC-01 nêu rõ `PENDING_VERIFICATION`: để khớp tuyệt đối, bổ sung trạng thái này vào `UserStatus`. Nếu nhóm/mentor **đã chốt cứng đúng 3 trạng thái** (`ACTIVE/IN_ACTIVE/LOCKED`), có thể dùng `IN_ACTIVE + emailVerifiedAt IS NULL` để biểu diễn pending (`emailVerifiedAt` ánh xạ cột `verified_at`), nhưng phải ghi nhận đó là cách ánh xạ khác thuật ngữ PDF và kiểm thử đầy đủ. Chọn **4 trạng thái** trong thiết kế đích, không tự sửa các test 3 trạng thái khi chưa migrate.
8. **Cấu hình chuẩn = `config_definitions` + `config_values` + `config_change_logs`.** `system_settings` là legacy/read-only hoặc được migrate về `config_values`, không cho phép hai nơi cùng được write. Cấu hình được version, có effectiveAt, scope và audit (UC-30).
9. **Phản ánh UC-28 = `complaint_tickets`** gắn conversation/booking/actor; có loại vấn đề, mức độ, deadline phản hồi theo policy, người được phân công, resolution và lịch sử trạng thái. Lịch sử complaint/check-in request chọn lưu append-only trong `audit_logs` có entityType/entityId, actor, from/to, reason, occurred time và correlationId; không cần thêm bảng history riêng nếu truy vấn được đầy đủ. `support_tickets` được giữ làm **generic helpdesk extension**, không dùng để xử lý song song cùng complaint. **Chat** dùng `conversations`, `conversation_participants`, `messages`, `read_receipts`: unique `(conversation_id, seq)` và **`(conversation_id, sender_id, client_message_id)`** theo UC-28 trang 29; cùng key khác payload trả conflict. Quyền truy vấn theo participant hiện tại/property owner hoặc SUPPORT được phân công; không chỉ kiểm tra đã từng có FK participant. Tách mục đích message `NORMAL/COMPLAINT/CHECKIN_REQUEST` khỏi dạng nội dung `TEXT/IMAGE/...`.
10. **MinIO metadata chuẩn = `media_assets`** quản lý key/checksum/size/status/lifecycle. `room_images` chỉ là quan hệ gắn media với phòng, thứ tự ảnh, cover; chuyển dần `url` thành FK `asset_id`. Không lấy object URL làm trạng thái upload.
11. **Fake Bank là ranh giới giao dịch độc lập.** `bank_accounts`, `bank_transactions`, `bank_ledger_entries`, `bank_webhook_outbox` thuộc schema/service Fake Bank; `wallets` và `wallet_transactions` là **ví trong hệ thống Booking tùy chọn**, không thay ledger Fake Bank. Phía Booking dùng `bank_webhook_events` để nhận và dedupe, `reconciliation_cases` để đối soát, `outbox_events` để phát tác vụ sau commit.
12. **Không bắt buộc Flyway theo thống nhất với mentor**, nhưng bắt buộc giữ SQL schema/version thủ công trong Git, có thứ tự áp dụng, dữ liệu seed, checksum/ghi nhận schema version và tài liệu cách chạy PostgreSQL từ DB rỗng. Không sử dụng lâu dài `ddl-auto:update` như công cụ quản lý schema; sau khi đồng bộ entity/schema chuyển sang `validate`. **Đặc tả trang 40/49 có yêu cầu Flyway**: quyết định không dùng phải được ghi là **ngoại lệ đã được mentor chấp thuận**, không thể tự gọi là tuân thủ nguyên văn 100%.

### Tương thích stack kỹ thuật với đặc tả (ngoài DB nhưng ảnh hưởng triển khai)

- `pom.xml` trong ZIP khai báo **Spring Boot `4.1.1` và Java `17`**, trong khi PDF v3.0 trang 40/50 ghi **Java 21 + Spring Boot 3.x**. Đây là **khác biệt có thật** cần trình mentor phê duyệt. Quyết định để tuân thủ nguyên văn: thống nhất **Java 21 + một phiên bản Spring Boot 3.x còn được nhóm hỗ trợ**, chạy lại test/build trên cả hai máy; nếu mentor chấp nhận 4.x/17 thì ghi ADR ngoại lệ rõ ràng thay vì coi đã đúng spec. Không nên đổi version ngay trên DB đang chạy mà không có nhánh thử nghiệm/kiểm thử hồi quy.
- Đặc tả cũng cần test concurrency trên PostgreSQL thật, không coi Swagger UI hoặc `mvn clean package` pass là xác nhận inventory/payment đúng.

## B. Ma trận xử lý 24 bảng DB chưa có mapping (theo báo cáo)

**Chú giải:** `BẮT BUỘC` = để hoàn thành 36 UC; `ĐIỀU KIỆN` = giữ nhưng mapping có thể chỉ qua join/JDBC hoặc dùng read model; `PHẦN SAU` = yêu cầu UC nhưng không phải luồng lõi đầu tiên.

| Bảng | Chốt xử lý | Mức | Liên quan |
|---|---|---|---|
| `refresh_tokens` | Tạo `RefreshToken`, lưu hash/expiry/revoked/family hoặc rotation | BẮT BUỘC | UC-02 |
| `verification_tokens` | Tạo `VerificationToken`, gộp OTP cũ, giới hạn attempts/reuse | BẮT BUỘC | UC-01/32 |
| `media_assets` | Tạo `MediaAsset`, quản lý upload/finalize/cleanup và private object key | BẮT BUỘC | UC-06 |
| `room_allocations` | Tạo `RoomAllocation`, inventory duy nhất, lock/overlap | BẮT BUỘC | UC-07/09/11/24 |
| `room_sale_blocks` | Tạo `RoomSaleBlock`, khóa bán phòng vật lý theo khoảng ngày | BẮT BUỘC | UC-05/24 |
| `room_type_amenities` | Dùng `@JoinTable` nếu chỉ gồm 2 FK; không cần entity riêng | ĐIỀU KIỆN | UC-05 |
| `refunds` | Tạo `Refund`, nguồn sự thật hoàn tiền thực thi | BẮT BUỘC | UC-13/36 |
| `bank_webhook_events` | Tạo `BankWebhookEvent`, xác minh HMAC/dedupe eventId/processedAt | BẮT BUỘC | UC-10/35/36 |
| `reconciliation_cases` | Tạo `ReconciliationCase`, lỗi lệch và retry/recovery | BẮT BUỘC | UC-35/36 |
| `audit_logs` | Tạo `AuditLog` hoặc JDBC writer; cấm log bí mật | BẮT BUỘC | UC-19 |
| `conversation_participants` | Tạo `ConversationParticipant` vì có joinedAt/leftAt | PHẦN SAU | UC-27/28 |
| `read_receipts` | Tạo `ReadReceipt` hoặc gộp participant có kế hoạch schema rõ; chọn **tạo riêng** để khớp schema hiện có | PHẦN SAU | UC-28 |
| `complaint_tickets` | Tạo `ComplaintTicket`: type/severity/responseDueAt/assignee/resolution/version; lịch sử append-only qua AuditLog | PHẦN SAU | UC-28 |
| `checkin_requests` | Tạo `CheckinRequest`: proposedAt/agreedCheckinAt/requester/responder/version; lịch sử qua AuditLog, giờ hẹn tách check-in thực tế | PHẦN SAU | UC-29 |
| `config_definitions` | Tạo `ConfigDefinition`, whitelist key/type/scope | PHẦN SAU | UC-30 |
| `config_values` | Tạo `ConfigValue`, effectiveAt/version/scope | PHẦN SAU | UC-30 |
| `config_change_logs` | Tạo `ConfigChangeLog`, lịch sử thay đổi | PHẦN SAU | UC-30 |
| `email_templates` | Tạo `EmailTemplate` theo templateCode/locale; status DRAFT/ACTIVE/ARCHIVED và active version | PHẦN SAU | UC-31 |
| `email_template_versions` | Tạo `EmailTemplateVersion`, allowed variables theo event, publish immutable; HTML escape/fallback/preview | PHẦN SAU | UC-31 |
| `mail_events` | Tạo `MailEvent`, snapshot/recipient/version, dedupe logical email; UNKNOWN/DLQ/retry, gửi sau commit | PHẦN SAU | UC-32/34 |
| `mail_delivery_attempts` | Tạo `MailDeliveryAttempt` lưu mỗi retry/result | PHẦN SAU | UC-34 |
| `scheduled_reminders` | Tạo `ScheduledReminder`, unique `(booking_id, reminder_type, window_code, policy_version)`, dueAt/cutoff/DB claim | PHẦN SAU | UC-33 |
| `report_exports` | Tạo `ReportExport` (hoặc JDBC), export async với status | PHẦN SAU | UC-26 |
| `analytics_refresh_state` | Giữ bảng, có thể dùng JDBC/upsert thay entity | ĐIỀU KIỆN | UC-18/25/26 |

## C. Ma trận 38 bảng đã có mapping

### C1. Nhóm bắt buộc (24)

| Bảng | Quyết định đích |
|---|---|
| `users` | Giữ, password chỉ từ hash; normalized email unique; emailVerifiedAt, tokenVersion; 1 status chuẩn |
| `roles` | Giữ, RBAC có dữ liệu |
| `permissions` | Giữ, RBAC có dữ liệu |
| `user_roles` | Giữ join; nếu cần `assigned_at` chính xác và lịch sử thì tạo `UserRole` |
| `role_permissions` | Giữ khóa ghép và unique |
| `properties` | Giữ, owner/timezone/status, scope theo property |
| `room_types` | Giữ danh mục loại phòng, không thay bằng enum `RoomType` |
| `rooms` | **Phòng vật lý duy nhất được booking**; unique `(property_id, room_code)`; status vận hành, không lưu `OCCUPIED` toàn cục |
| `room_units` | Giữ dữ liệu legacy/compatibility; không là inventory song song với `rooms` |
| `amenities` | Giữ danh mục động |
| `room_amenities` | Giữ join, có thể refactor thành JoinTable sau khi đối chiếu schema |
| `room_rates` | **Giá chuẩn duy nhất** theo `(room_type_id, stay_date, currency)` phù hợp model đang có |
| `cancellation_policies` | Giữ chính sách và snapshot lúc booking |
| `cancellation_policy_rules` | Giữ mốc ngày/tỷ lệ; không để policy_json thành nguồn tính tiền thứ hai |
| `bookings` | Giữ bookingId, roomId, customerId, checkIn/out, **1 expiresAt**, **1 totalAmount**, status/version; actualCheckIn/actualCheckOut, actor và checkinVerificationMethod; sourceChannel và snapshot cần cho báo cáo |
| `booking_nights` | Giữ snapshot từng đêm, unique booking/date |
| `booking_status_history` | Giữ state transition/actor/reason/correlationId |
| `payments` | Giữ; **chuyển 1-to-1 sang many-to-one booking**; một hàng/intent, retry tái sử dụng, intent mới chỉ sau khi intent trước kết thúc hợp lệ |
| `idempotency_records` | Giữ `(scope, idempotency_key)` unique, request hash/result; scope phải bao gồm actor + operation, tránh key của hai user xung đột |
| `outbox_events` | Giữ, job claim/retry/dedupe sau commit |
| `reviews` | Giữ, bookingId unique; chỉ review sau hoàn thành |
| `notifications` | Giữ, dedupe recipient/event |
| `conversations` | Giữ, thêm bookingId/propertyId/type/status/lastMessageSeq |
| `messages` | Giữ, thêm seq/clientMessageId/type purpose/body/assetId; scope qua conversation |

### C2. Nhóm 5 bảng hỗ trợ

| Bảng | Quyết định đích |
|---|---|
| `room_images` | Giữ bảng gắn ảnh, bổ sung `asset_id` FK → `media_assets`; `url` chỉ legacy |
| `room_calendar` | Giữ bảng hiện có **chỉ đọc / projection**, không cho làm nguồn tồn hoặc giá riêng |
| `system_settings` | Giữ vật lý để chuyển dữ liệu, sau đó read-only/deprecated; đọc policy từ `config_*` |
| `host_profiles` | Giữ mở rộng hồ sơ HOST, không thay `properties.owner_id` |
| `review_media` | Giữ tính năng ảnh/video review; ánh xạ tới `media_assets` khi triển khai |

### C3. Nhóm 9 bảng **giữ nguyên như tính năng thêm** (theo yêu cầu)

| Bảng | Giữ để làm gì | Điều kiện để không phá core |
|---|---|---|
| `ai_recommendations` | Gợi ý cá nhân hóa | Chỉ gợi ý kết quả; không tự hứa còn phòng |
| `dynamic_pricing_suggestions` | Gợi ý giá cho HOST | Chỉ chuyển thành `room_rates` sau khi HOST xác nhận; không tự ghi giá booking |
| `currency_exchange_rates` | Hiển thị tỷ giá quy đổi | Tiền gốc của booking/payment/refund vẫn VND; nếu có FX phải snapshot/nguồn/timestamp |
| `host_vip_packages` | Gói dịch vụ HOST | Độc lập logic quyền sở hữu/đặt phòng |
| `loyalty_ranks` | Hạng thành viên | Không mặc định làm thay đổi giá snapshot |
| `point_history` | Nhật ký điểm | Không coi điểm là tiền ngân hàng; giao dịch điểm phải bất biến/idempotent |
| `collections` | Bộ sưu tập/yêu thích | Không mang ý nghĩa giữ phòng |
| `collection_items` | Liên kết room vào collection | FK/unique `(collection_id, room_id)` theo thiết kế, không xóa phòng đang có booking |
| `voucher_usages` | Lượt sử dụng voucher | Phải có bảng `vouchers`; chống dùng trùng, snapshot giảm giá và tính quota trong transaction |

## D. 12 bảng có entity trong Java nhưng chưa thấy trong PostgreSQL báo cáo

| Bảng | Quyết định tạo bảng/mapping |
|---|---|
| `refund_requests` | **Chưa tạo như bảng nghiệp vụ lõi**; refactor luồng UC-13/36 sang `refunds`. Chỉ tạo sau nếu có nhu cầu duyệt refund riêng; tránh dual-write |
| `support_tickets` | **Tính năng thêm** helpdesk tổng quát; UC-28 xử lý tại `complaint_tickets`. Có thể tạo sau khi bảo đảm workflow không trùng |
| `otp_verifications` | **Không tạo nguồn thứ hai**; chuyển mã sang `verification_tokens`, retire entity cũ |
| `user_behavior_logs` | **Tạo** để hỗ trợ funnel UC-25; eventId/session/correlation/eventTime/ingestedAt/property/sourceChannel, dedupe từng bước; không dùng riêng booking để suy diễn đầy đủ funnel |
| `co_hosts` | **Tạo như mở rộng** nếu muốn giữ @Entity; quyền ủy quyền chỉ có hiệu lực sau khi service kiểm tra scope |
| `kyc_documents` | **Tạo như mở rộng**, private MinIO assets/authorization/review status |
| `dispute_evidence` | **Tạo như mở rộng** bằng FK complaint/refund/media nếu phù hợp schema chốt |
| `host_vip_subscriptions` | **Tạo như mở rộng** với `host_vip_packages` và unique kỳ hợp lệ |
| `vouchers` | **Tạo trước `voucher_usages`** để FK không bị thiếu; nghiệp vụ discount bật sau |
| `wallets` | **Tạo như mở rộng** nếu giữ entity, không làm Fake Bank account |
| `wallet_transactions` | **Tạo như mở rộng** gắn wallet, invariant balance/idempotency khi bật |
| `withdrawal_requests` | **Tạo như mở rộng** nhưng chưa triển khai payout, đối soát, kiểm soát rút tiền |

**Lưu ý thực thi:** Có 2 lựa chọn hợp lệ khi chuyển sang `ddl-auto: validate`: (i) tạo bảng đúng schema cho các entity extension còn được Spring/Hibernate scan, nhưng khóa API chưa xong; hoặc (ii) chưa scan/map entity của feature chưa triển khai. **Không được** để Hibernate `update` lặng lẽ tạo bảng trên máy A còn máy B không có rồi xem đó là thiết kế đã thống nhất. Đối với ba entity trùng domain (`OtpVerification`, `RefundRequest`, `SupportTicket`), ưu tiên bỏ khỏi mapping nghiệp vụ sau khi refactor, không tự tạo ba nguồn ghi khác nhau.

## E. Các hợp đồng dữ liệu và bất biến cần chốt

| Phạm vi | Dữ liệu/constraint đích | Test nghiệm thu |
|---|---|---|
| Identity | unique lower(trim(email)); passwordHash non-null; mã xác minh hash + expires + usedAt + attempts | 2 đăng ký cùng email, OTP hết hạn/dùng lại, trạng thái chưa xác minh |
| Property/room | unique `(property_id, room_code)`; propertyId của room/type nhất quán; owner scope | 2 HOST không thể sửa chéo; đổi phòng không thay data booking |
| Price | unique `(room_type_id, stay_date, currency)`; amount `NUMERIC` >= 0; đầy đủ từng đêm | quote 3 đêm khớp tổng snapshot; đổi rate sau booking không đổi tổng |
| Inventory | `check_in < check_out`; một room không được có 2 allocation active overlap; release idempotent | 100 parallel request cùng room, chỉ 1 booking giữ được phòng |
| Booking | `booking_nights (booking_id, stay_date)` unique; history append-only; một trường total/expiration chuẩn | rollback 1 đêm thất bại → không còn booking/hold |
| Payment | provider/merchant-scoped reference/key unique; một intent mở/booking; retry cùng intent; amount/currency khớp booking | 2 lần nhấn pay, chặn intent mới khi UNKNOWN; callback duplicate/late, reconcile |
| Refund | idempotency unique; tổng succeeded + reservation chưa kết thúc + khoản mới không vượt captured; lock trước khi ghi reservation | 2 refund cạnh tranh trước callback vẫn không vượt hạn mức; retry/partial refund không nhân đôi |
| Chat | unique `(conversation_id, seq)` và `(conversation_id, sender_id, client_message_id)`; payload hash và participant scope | Retry cùng sender/key trả message cũ; khác sender cùng client ID không xung đột; cùng key khác payload bị từ chối |
| Complaint/check-in | type/severity/deadline/assignee cho complaint; giờ đề nghị/thỏa thuận và responder cho check-in; version + history qua audit | Quá hạn/escalation, hai HOST phản hồi cạnh tranh; giờ hẹn không đổi booking thành CHECKED_IN |
| Email | template status/allowed variables/immutable version; unique logical eventType + business + recipient + version; delivery attempts riêng, UNKNOWN/DLQ | Retry giữ version/snapshot; resend cùng logical event; SMTP timeout không kết luận chưa gửi |
| Reminders | unique `(booking_id, reminder_type, window_code, policy_version)`, persisted dueAt/cutoff, DB claim | 2 scheduler không tạo mail trùng; hai mốc 48h/24h cùng policy vẫn tạo được hai reminder hợp lệ |
| Analytics | sourceChannel, guest segment có quy tắc xác định, capturedAt/refund processedAt; grain/time axis/status inclusion/asOf rõ | Một booking 3 đêm không thành 3 booking; không nhân tiền do join facts; đối chiếu kỳ/timezone/cache scope |
| Extensions | FK chặt chẽ nhưng tắt luồng tính tiền/voucher/wallet khi chưa đủ invariants | Core UC vẫn chạy khi không có data extension |

**Dùng `BigDecimal` + PostgreSQL `NUMERIC` cho tiền**; `LocalDate` cho đêm/phạm vi `[from,to)`; `Instant`/UTC (`timestamptz`) cho event/callback/expiry, timezone của property để diễn giải ngày. Enum Java lưu `VARCHAR` bằng `@Enumerated(STRING)` trong target thiết kế (không dùng `ORDINAL`); tránh `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` khi DB chưa có PostgreSQL native ENUM tương ứng. Chạy migration data status cũ trước khi bật `validate`.

**Tên cột đích (sau khi kiểm tra dữ liệu thật):** `users.password_hash` (không phải thêm cả `password`), `bookings.customer_id`, `check_in`, `check_out`, `expires_at`, `total_amount`; `messages.body`; `notifications.recipient_id`. Chọn tên theo PDF và chuẩn hóa entity `@Column`; đây là **định hướng refactor**, không phải lệnh `ALTER DROP` an toàn để chạy ngay. Có thể giữ tên Java field khác nếu ánh xạ đúng cột chuẩn.

### E1. Chi tiết đối tượng và quy tắc còn thiếu đã bổ sung sau rà soát

- **Check-in thực tế:** `Booking.actualCheckIn/actualCheckOut`, checkedInBy/checkedOutBy, phương thức xác minh và history là nguồn ghi nhận lưu trú; `CheckinRequest.agreedCheckinAt` chỉ là giờ hẹn. PENDING/COUNTER_PROPOSED/APPROVED/REJECTED và trạng thái đóng request được định nghĩa riêng. Hủy booking đóng request, hủy reminder chưa gửi; đổi giờ hẹn tính lại dueAt của reminder chưa gửi.
- **Email:** unique logical email theo eventType + businessKey + recipient identity + template version (UC-32 trang 33), cộng unique nguồn event + eventType + recipient để webhook duplicate không tạo email mới khi active template đã đổi, nhưng một business event vẫn có thể tạo các loại email khác nhau theo policy. Recipient identity là định danh/HMAC ổn định, không dùng địa chỉ đã mask để dedupe. Resend tạo delivery attempt trong cùng mail event, giữ nguyên version và snapshot. Verification link hết hạn phải phát hành token an toàn mới theo nghiệp vụ, không resend nguyên link cũ. Bí mật ngắn hạn phục vụ gửi email phải có cơ chế bảo vệ/TTL, không lưu token plaintext trong JSON snapshot/outbox/log.
- **Reminder:** chọn khóa bốn thành phần ở UC-33 trang 34 (có windowCode); mô tả ba thành phần ở trang 44 là rút gọn, không dùng để cấm nhiều mốc nhắc. Chọn `EFFECTIVE_FOR_UNSENT`: policy mới thay thế reminder chưa gửi; lưu dấu logical window đã gửi để policyVersion mới không tự gửi lặp cùng mốc đã hoàn tất.
- **Analytics:** `bookings.sourceChannel` chốt lúc tạo; nhóm khách mới/quay lại được tính theo lịch sử đủ quyền với cutoff và định nghĩa nhất quán, không đọc một user flag hiện tại để diễn giải lại lịch sử. Payment có capturedAt, refund có processedAt; thời gian tạo và ngày lưu trú là các trục khác nhau. Định nghĩa metric gồm grain, tử/mẫu, status inclusion, currency, timezone, nguồn, cutoff/asOf và freshness. Fact/summary table tùy tải; không coi có đủ entity là đủ báo cáo.
- **Enum:** catalog đi kèm ghi các tập mã đích, phân biệt mã nêu trong PDF với mã kỹ thuật được thiết kế bổ sung. Enum không thay lịch sử, entity, kiểm tra quyền, transaction hoặc state machine. Không sửa enum code hay dữ liệu trạng thái trong lần cập nhật tài liệu này.

## F. Lộ trình triển khai dành cho nhóm học Spring Boot

### Giai đoạn 0 — Chốt schema và dựng sạch trên 2 máy

1. Backup DB ở cả hai máy (`pg_dump`), đối chiếu schema thực với báo cáo; lấy danh sách các constraint/index/FK và dữ liệu cột trùng. Không chạy drop/copy đè mù.
2. Chốt bảng canonical trong mục A thành một ERD, note tất cả bảng `legacy`, `required`, `extension`. Review với mentor.
3. Tạo bộ SQL **theo thứ tự** `db/manual/V001__baseline.sql`, `V002__normalize_existing_data.sql`, `V003__constraints_indexes.sql`, `V004__seed_reference_data.sql` (tên là gợi ý, **không** phải Flyway tự chạy). Script phải kiểm tra tiền điều kiện/idempotency hoặc fail rõ ràng và có `schema_version`/checksum; không sửa file đã được nhóm chạy mà tạo version tiếp theo.
4. Có **một** lệnh PowerShell/CLI áp dụng SQL lên DB mới và script kiểm tra bảng/constraint quan trọng; chạy từ DB rỗng trên cả hai máy. Đồng bộ code/entity rồi đổi `spring.jpa.hibernate.ddl-auto=validate` cho môi trường nhóm; secret để trong `.env`/environment, không commit mật khẩu cố định.

### Giai đoạn 1 — Identity + Catalog + Search

- `users`, tokens, roles, permissions, property, type, room, amenity, room rate, sale blocks, media assets.
- Endpoint register/verify/login/refresh/logout; HOST scope; MinIO presigned upload/finalize; public search và quote snapshot. Viết test quyền sai scope và hai người trùng email.

### Giai đoạn 2 — Booking/Inventory (ưu tiên chất lượng hơn số bảng)

- `bookings`, `booking_nights`, `room_allocations`, `booking_status_history`, idempotency, outbox.
- Chốt status `PENDING_PAYMENT -> CONFIRMED -> CHECKED_IN -> COMPLETED`; `PENDING_PAYMENT -> EXPIRED`; cancellation/refund transition riêng; chỉ một cơ chế lock/write tồn kho.
- Test concurrency trên PostgreSQL thật (ưu tiên Testcontainers), webhook/Redis/MinIO chưa cần để bắt đầu test chống double booking.

### Giai đoạn 3 — Fake Bank/Payment/Refund

- `payments`, `refunds`, `bank_webhook_events`, `reconciliation_cases`; Fake Bank độc lập với `bank_accounts`, `bank_transactions`, `bank_ledger_entries`, `bank_webhook_outbox` phía service/schema bank.
- Chứng minh saga/compensation/idempotency khi callback mất, tới muộn, bị gửi lặp hoặc Bank debit thành công nhưng Booking DB timeout.

### Giai đoạn 4 — Hoàn thiện toàn bộ 36 UC

- Check-in/out, review, host/admin views, audit, reporting/funnel/export, conversation/complaint/check-in request, config/email/reminder/DLQ, observability, hardening, performance/recovery.
- Từ lúc này core đáp ứng đầy đủ tất cả UC nếu nghiệm thu pass; **chỉ có đủ entity không được coi là hoàn thành UC**.

### Giai đoạn 5 — Tính năng mở rộng (không xoá)

- Collections, review media, host profile, AI recommendation, giá gợi ý, VIP, loyalty, voucher, FX; sau cùng wallet/payout vì liên quan tiền và yêu cầu đối soát/permission cao.
- Mỗi extension cần API, bảng/quy tắc nghiệp vụ, FK/unique/transaction, test và feature flag; không bật cả hệ thống chỉ vì entity/table đã tồn tại.

## G. Việc nên tránh

- Không tự xóa 9 bảng mục 3.3 hay 12 entity code-only để đạt “bằng nhau” 50/62.
- Không đưa AI/voucher/loyalty vào phép tính tiền cho booking khi chưa có snapshot, quota, tax/fee và khả năng rollback.
- Không để `rooms` và `room_units` cùng độc lập nhận booking.
- Không để `room_rates`/`room_calendar`/`rooms.price_per_night` quyết định giá ngẫu nhiên theo endpoint.
- Không sửa bằng cách đổi tên `@Table("refund_requests")` thành `refunds` đơn thuần khi chưa đối chiếu cột và lifecycle.
- Không biến `Wallet` thành Fake Bank chỉ vì đều có số dư.
- Không coi build thành công và Swagger hiển thị API là bằng chứng constraint/transaction/36 UC đã hoàn tất.
- Không chạy `docker compose down -v` trên dữ liệu chưa backup.

## H. Điểm cần kiểm chứng trước khi viết DDL thật

Các hành động như thêm unique, chuyển `Payment` sang many-to-one, migrate `password` thành `password_hash`, hợp nhất hai loại phòng, đổi tên cột và xóa/retire mapping đều phụ thuộc **dữ liệu và FK/index thực tế**. `DB_SPEC_COMPARISON.md` có metadata tổng quan nhưng ZIP **không chứa bản `pg_dump --schema-only` hay snapshot dữ liệu DB**, nên chưa đủ căn cứ viết SQL `ALTER` chạy an toàn trên hai DB đang tồn tại. Hãy lấy `pg_dump --schema-only` và kiểm tra số bản ghi/giá trị trùng khi bắt đầu Giai đoạn 0.

**Kết luận:** giữ 9/9 bảng 3.3 như extension; giữ phần lớn 62 bảng DB; bổ sung mapping cho dữ liệu bắt buộc còn thiếu; chỉ hợp nhất *nguồn sự thật* ở các điểm trùng nghĩa, không xóa dữ liệu mù; tiếp tục đáp ứng 36 UC bằng hành vi + test PostgreSQL thật. Không dùng Flyway là ngoại lệ có quản lý SQL version thủ công, không phải bỏ việc quản trị schema.
