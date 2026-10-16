# Đối chiếu DB, entity và đặc tả đặt phòng

Ngày kiểm tra: 16/10/2026.

Nguồn: `Dac_ta_he_thong_dat_phong_spring.pdf`, phiên bản 3.0, 51 trang; schema `public` của `jdbc:postgresql://localhost:5435/vivu`; mã nguồn `src/main/java/com/vn/datphong/entity` và `enums`.

Đã đọc metadata bảng, cột, kiểu dữ liệu và danh mục PostgreSQL enum bằng kết nối read-only. Không sửa DB, không khởi động Spring Boot, không chạy Hibernate schema update. Đây là đối chiếu cấu trúc và phạm vi, chưa chứng minh các chức năng hoặc constraint hoạt động đúng.

## 1. Kết luận và cách hiểu

- DB có 62 bảng. Mã nguồn khai báo 49 `@Entity` và 1 bảng liên kết `user_roles`, tương ứng 50 tên bảng.
- 38 bảng DB có mapping; 24 bảng chưa có mapping trong mã nguồn hiện tại.
- 12 tên bảng được entity khai báo chưa tồn tại trong DB.
- 24 bảng thiếu mapping không đồng nghĩa 24 bảng dư. Phần lớn trực tiếp phục vụ yêu cầu PDF.
- PDF trang 39 gọi mô hình dữ liệu là “khuyến nghị”; trang 4 nêu không bắt buộc tên class. Cần đáp ứng hành vi và ràng buộc, không máy móc tạo đủ tên bảng/class.
- “Nên có entity” dưới đây là đề xuất phù hợp kiến trúc JPA hiện tại. JDBC/native SQL vẫn có thể quản lý bảng nếu có ownership, transaction và migration rõ ràng. Có bảng không có nghĩa bắt buộc có `@Entity` riêng.
- “Ngoài phạm vi” nghĩa PDF v3.0 chưa yêu cầu chức năng đó; không phải kết luận bảng vô dụng hoặc được phép xóa.

## 2. Toàn bộ 24 bảng hiện chưa có mapping

| Bảng DB | Đối chiếu PDF | Đề xuất |
|---|---|---|
| `refresh_tokens` | UC-02, trang 5; mô hình identity trang 39 | Thêm `RefreshToken`: hash, hạn, thu hồi, rotation. Enum không lưu được từng phiên. |
| `verification_tokens` | UC-01, trang 4–5; UC-32 trang 33 | Thêm `VerificationToken`, hoặc thống nhất một mô hình với `OtpVerification`; cần hash, hạn, lần thử, đã dùng. |
| `media_assets` | UC-06 trang 9; trang 39 | Thêm `MediaAsset`: object key, size, checksum, trạng thái upload/finalize. Chỉ có enum `MediaAssetStatus` là chưa đủ. |
| `room_allocations` | Trang 39–40; UC-09 | Nên thêm `RoomAllocation` nếu giữ mô hình daterange hiện có. PDF cho phép mô hình khác, nhưng phải thống nhất cơ chế chống overlap toàn hệ thống. |
| `room_sale_blocks` | UC-05; UC-24 trang 25 | Thêm `RoomSaleBlock` nếu chọn lịch khóa bán theo khoảng ngày. Có thể hợp nhất với lịch bán theo ngày nếu bảo toàn nghiệp vụ. |
| `room_type_amenities` | UC-05, tiện nghi theo loại phòng | Mapping `RoomTypeEntity` ↔ `Amenity` bằng `@ManyToMany/@JoinTable` là đủ nếu chỉ có hai khóa. Không cần entity riêng khi chưa có thuộc tính quan hệ. |
| `refunds` | UC-13, UC-36; trang 39, 41 | Chọn entity map đúng bảng này. `RefundRequest` hiện map `refund_requests`, nên không sử dụng `refunds`. Phải quyết định gộp mô hình hay tách request và giao dịch thực thi. |
| `bank_webhook_events` | UC-35 trang 36; trang 41 có payment events | Nên thêm `BankWebhookEvent` làm inbox/dedupe phía Booking: event ID, payload hash, xác minh, đã xử lý. Không nhầm với webhook outbox phía ngân hàng. |
| `reconciliation_cases` | UC-35/36; trang 41 | Thêm `ReconciliationCase`: sai lệch thanh toán/hoàn tiền, số lần xử lý, trạng thái, thời điểm giải quyết. |
| `audit_logs` | UC-19; trang 39 | Thêm `AuditLog`, hoặc adapter JDBC chuyên trách. Dữ liệu actor/before/after/reason/time không thể thay bằng enum. |
| `conversation_participants` | UC-27/28; trang 43–44 | Nên có `ConversationParticipant` với khóa ghép: bảng có `joined_at`, `left_at`, phục vụ quyền tham gia và lịch sử. `@ManyToMany` đơn giản sẽ không biểu diễn đủ các thuộc tính này. |
| `read_receipts` | UC-28 trang 29; trang 43 | Nên có `ReadReceipt` với khóa conversation/user, `last_read_seq`, thời điểm cập nhật. Có thể gộp vào participant nếu chọn lại schema nhất quán. |
| `complaint_tickets` | UC-28; trang 43–44 | Thêm `ComplaintTicket`, hoặc hợp nhất có chủ đích với `SupportTicket`. Cần conversation, booking, người xử lý, lịch sử giải quyết. |
| `checkin_requests` | UC-29 trang 30; trang 43–44 | Thêm `CheckinRequest`. Lịch hẹn và phản hồi có vòng đời riêng, không thay bằng `BookingStatus`. |
| `config_definitions` | UC-30; trang 44 | Thêm `ConfigDefinition`: định nghĩa khóa, kiểu, scope, mô tả/validation theo thiết kế. |
| `config_values` | UC-30; trang 44 | Thêm `ConfigValue`: giá trị thay đổi, phạm vi, effective time, version. Không hard-code các giá trị này trong enum. |
| `config_change_logs` | UC-30; trang 44 | Thêm `ConfigChangeLog`, hoặc mapping audit có khả năng lưu đầy đủ lịch sử cấu hình. |
| `email_templates` | UC-31; trang 44 | Thêm `EmailTemplate`: mã, locale, phiên bản active. Nội dung do admin quản lý, không phải enum. |
| `email_template_versions` | UC-31; trang 44 | Thêm `EmailTemplateVersion`: nội dung/version đã publish bất biến, biến cho phép. |
| `mail_events` | UC-32/34; trang 44 | Thêm `MailEvent`: logical event, recipient, snapshot, template version, trạng thái. |
| `mail_delivery_attempts` | UC-34; trang 44 | Thêm `MailDeliveryAttempt`: mỗi lần gửi/retry và kết quả. |
| `scheduled_reminders` | UC-33; trang 44 | Thêm `ScheduledReminder`: due time, policy version, dedupe, claim, trạng thái. |
| `report_exports` | UC-26 trang 27; API export trang 42 | Thêm `ReportExport`/`ReportJob` nếu quản lý export bất đồng bộ bằng JPA. Tên bảng khác `report_job` vẫn đáp ứng PDF. |
| `analytics_refresh_state` | Hỗ trợ freshness/asOf/dataVersion, trang 27, 43 | Bảng kỹ thuật tùy chọn, không được PDF chỉ định bắt buộc. Có thể dùng JDBC/upsert hoặc entity; không phải enum vì lưu version/thời điểm theo property. |

Trong nhóm này, `analytics_refresh_state` là chi tiết triển khai tùy chọn; `room_type_amenities` có thể chỉ là join mapping. Inventory và lịch bán có phương án thay thế được PDF cho phép.

## 3. Toàn bộ 38 bảng đã có mapping

### 3.1. Nhóm thuộc nghiệp vụ cốt lõi: 24 bảng

| Bảng | Mapping hiện có | Nhận xét |
|---|---|---|
| `users` | `User` | Cần giữ entity; rà soát hash mật khẩu, xác minh, token version và trường trùng. |
| `roles` | `Role` | PDF yêu cầu quản trị role; giữ entity, có thể dùng enum cho mã role cố định. |
| `permissions` | `Permission` | Giữ entity; enum mã permission có thể hỗ trợ kiểm tra quyền trong code. |
| `user_roles` | `User.@JoinTable` | Không cần entity riêng nếu chỉ cần liên kết. DB có thêm `assigned_at`: nếu cần đọc/ghi lịch sử này, dùng `UserRole` hoặc cơ chế audit rõ ràng. |
| `role_permissions` | `RolePermission` | Entity khóa ghép hiện có phù hợp; join mapping cũng là lựa chọn nếu không có dữ liệu quan hệ. |
| `properties` | `Property` | Giữ; owner scope, timezone, trạng thái xuất bản. |
| `room_types` | `RoomTypeEntity` | Giữ; danh mục loại phòng do HOST tạo theo property, có sức chứa và chính sách. Không thay bằng enum `RoomType`. |
| `rooms` | `Room` | Giữ mô hình phòng chuẩn; thống nhất quan hệ với `room_units`. |
| `room_units` | `RoomUnit` | Phù hợp nghiệp vụ phòng vật lý; cần quyết định rõ vai trò khi cùng tồn tại `rooms`. Không mặc định cần hai thực thể phòng vật lý. |
| `amenities` | `Amenity` | Giữ danh mục có ID, code, tên, icon; không nên đóng cứng bằng enum nếu quản trị được. |
| `room_amenities` | `RoomAmenity` | Khóa ghép hợp lệ; chỉ là mapping quan hệ nếu không thêm dữ liệu riêng. |
| `room_rates` | `RoomRate` | Giữ giá theo ngày và room/room type theo một quy tắc thống nhất. |
| `cancellation_policies` | `CancellationPolicy` | Giữ chính sách có dữ liệu/phiên bản, không thay bằng enum. Booking cần snapshot. |
| `cancellation_policy_rules` | `CancellationPolicyRule` | Giữ nếu dùng mô hình rule theo mốc ngày/tỷ lệ. Tránh hai nguồn quy tắc độc lập giữa bảng rule và `policy_json`. |
| `bookings` | `Booking` | Giữ; enum chỉ là thuộc tính trạng thái, không thay entity booking. |
| `booking_nights` | `BookingNight` | Giữ snapshot từng đêm. |
| `booking_status_history` | `BookingStatusHistory` | Giữ lịch sử; `fromStatus/toStatus/source` có thể là enum. |
| `payments` | `Payment` | Giữ; cần chuẩn hóa vòng đời, idempotency và provider reference. |
| `idempotency_records` | `IdempotencyRecord` | Giữ dữ liệu khóa/request hash/kết quả; không phải enum. |
| `outbox_events` | `OutboxEvent` | Giữ persistence sự kiện sau commit/retry; status và type có thể enum. |
| `reviews` | `Review` | Giữ; rating là giá trị được validate, status có thể enum. |
| `notifications` | `Notification` | Giữ; mỗi thông báo và trạng thái đã đọc là dữ liệu người dùng. |
| `conversations` | `Conversation` | Giữ nhưng mapping đang thiếu nhiều trường nghiệp vụ trong DB/PDF như booking, property, type/status, sequence. |
| `messages` | `Message` | Giữ; rà soát body/content, seq, client message ID và media asset. |

### 3.2. Nhóm hỗ trợ hoặc thiết kế cần thống nhất: 5 bảng

| Bảng | Mapping | Đánh giá |
|---|---|---|
| `room_images` | `RoomImage` | Ảnh phòng thuộc phạm vi UC-06; bảng này là một cách tổ chức. Cần liên kết/đồng bộ với `media_assets`, không thay thế metadata lifecycle bằng URL đơn thuần. |
| `room_calendar` | `RoomCalendar` | Lịch bán thuộc phạm vi. Có thể thay cho một phần sale blocks/rates theo ngày; tránh ba nguồn cùng quyết định giá và đóng bán. |
| `system_settings` | `SystemSetting` | Cấu hình thuộc phạm vi. Mô hình key/value hiện tại chưa thể hiện đầy đủ scope/effective/version/history mà bộ `config_*` có. Nên chọn nguồn cấu hình chính. |
| `host_profiles` | `HostProfile` | Hồ sơ HOST mở rộng có thể hữu ích; PDF không bắt buộc bảng riêng. Owner nghiệp vụ vẫn phải xác định từ user/property hợp lệ. |
| `review_media` | `ReviewMedia` | Ảnh/video đính kèm đánh giá là phần mở rộng; chưa thấy yêu cầu rõ cho chức năng này trong PDF. Không bắt buộc để hoàn thành use case đánh giá bằng nội dung/rating. |

### 3.3. Nhóm chức năng ngoài phạm vi bắt buộc PDF v3.0: 9 bảng

| Bảng | Mapping | Lý do |
|---|---|---|
| `ai_recommendations` | `AI_Recommendations` | PDF không yêu cầu hệ thống gợi ý AI. |
| `dynamic_pricing_suggestions` | `DynamicPricingSuggestion` | Quản lý giá theo ngày không đồng nghĩa có mô hình gợi ý giá tự động. |
| `currency_exchange_rates` | `CurrencyExchangeRate` | Phạm vi chính dùng VND; trang 24 chỉ lưu ý đa tiền tệ “nếu triển khai”, không bắt buộc hệ thống tỷ giá. |
| `host_vip_packages` | `HostVipPackage` | Không có use case bán gói VIP cho HOST. |
| `loyalty_ranks` | `LoyaltyRank` | Không có yêu cầu hạng thành viên/chiết khấu theo điểm. |
| `point_history` | `PointHistory` | Không có nghiệp vụ tích/tiêu điểm. |
| `collections` | `RoomCollection` | Không có yêu cầu bộ sưu tập/danh sách yêu thích/chia sẻ. |
| `collection_items` | `CollectionItem` | Quan hệ phục vụ collection ngoài phạm vi. |
| `voucher_usages` | `VoucherUsage` | Không có use case voucher/khuyến mãi; bảng `vouchers` mà entity khai báo còn chưa tồn tại trong DB này. |

Giữ hoặc loại khỏi kế hoạch tùy phạm vi sản phẩm đã thống nhất; không tự xóa bảng hoặc mã nguồn vì kết quả đối chiếu này.

## 4. 12 bảng có entity nhưng DB chưa có

| Tên bảng trong entity | Phân loại và hướng xử lý |
|---|---|
| `refund_requests` | Hoàn tiền thuộc phạm vi; lệch với `refunds`. Quyết định một entity giao dịch refund hay tách request/approval khỏi refund thực thi. |
| `support_tickets` | Có thể đáp ứng hỗ trợ/phản ánh; DB có `complaint_tickets`. Không tự coi hai tên là cùng mô hình; rà quan hệ, phân công, deadline, resolution rồi gộp/tách. |
| `otp_verifications` | Có thể là cách triển khai xác minh; DB có `verification_tokens`. Chọn mô hình đáp ứng hash, expiry, attempt và chống dùng lại. Không tự tạo hai nguồn token cho cùng luồng. |
| `user_behavior_logs` | Event log cho funnel UC-25 là trong phạm vi; bảng này chỉ phù hợp nếu có event ID, session/correlation, loại event, event time và scope cần thiết. Không bắt buộc giữ tên hoặc triển khai tracking hành vi ngoài mục tiêu đó. |
| `co_hosts` | Đồng chủ/quyền ủy nhiệm HOST chưa được đặc tả yêu cầu. |
| `kyc_documents` | Luồng KYC xét duyệt giấy tờ chưa được yêu cầu. |
| `dispute_evidence` | Phản ánh/tranh chấp thuộc phạm vi, nhưng một phân hệ chứng cứ riêng chưa được quy định bắt buộc; là mở rộng tùy chọn. |
| `host_vip_subscriptions` | Đăng ký gói VIP ngoài phạm vi. |
| `vouchers` | Voucher ngoài phạm vi. |
| `wallets` | Ví điện tử nội bộ ngoài phạm vi. Tài khoản Fake Bank là mô hình phía ngân hàng, không mặc nhiên là ví Booking. |
| `wallet_transactions` | Sổ giao dịch ví Booking ngoài phạm vi; không thay ledger của Fake Bank. |
| `withdrawal_requests` | Rút tiền/chi trả HOST chưa có use case trong PDF. |

## 5. Trường hợp dùng enum

Quy tắc: dùng enum cho tập giá trị hữu hạn mà thay đổi cần đồng bộ logic chương trình; dùng entity/bảng khi có nhiều bản ghi, quan hệ, dữ liệu do người dùng quản lý, phiên bản, thời gian hiệu lực hoặc lịch sử. Enum thường là thuộc tính của entity, không phải lựa chọn thay cả entity.

| Thuộc tính | Có nên enum? | Lưu ý |
|---|---|---|
| `users.status` | Có: `UserStatus` | Cần thêm `PENDING_VERIFICATION` theo UC-01; hiện chỉ có ACTIVE/IN_ACTIVE/LOCKED. |
| `properties.status` | Có: `PropertyStatus` | DRAFT/PUBLISHED/PAUSED/ARCHIVED hiện phù hợp vòng đời PDF. |
| `bookings.status` | Có: `BookingStatusType` | Chuẩn hóa HOLD/HELD/PENDING_PAYMENT và nhánh sai lệch; enum không tự bảo vệ transition hay overlap. |
| `payments.status` | Có: `PaymentStatusType` | Hiện có cả SUCCESS và SUCCEEDED; cần chọn mã chuẩn/migration. Chưa có UNKNOWN/RECONCILING mà UC-35/36 dùng cho timeout chưa rõ kết quả. |
| `refunds.status` | Có: `RefundStatusType` | Chuẩn hóa PENDING/REQUESTED, PAID/SUCCEEDED theo nghĩa nghiệp vụ. Nếu có phê duyệt yêu cầu hoàn, phân biệt trạng thái request và trạng thái giao dịch thực thi. |
| Trạng thái phòng vật lý | Có: `RoomUnitStatus` hoặc enum thống nhất | ACTIVE/INACTIVE/MAINTENANCE là tình trạng vận hành. Không dùng cờ OCCUPIED toàn cục thay truy vấn tồn theo khoảng ngày. |
| `media_assets.status` | Có: `MediaAssetStatus` | Hiện thiếu DELETED so với vòng đời trang 38; enum đã có nhưng chưa có `MediaAsset` entity. |
| `conversations.type/status` | Có | Ví dụ INQUIRY/BOOKING_SUPPORT; OPEN/... theo state machine được chốt. |
| `messages.type` | Có | PDF dùng NORMAL/COMPLAINT/CHECKIN_REQUEST cho mục đích tin nhắn. Enum hiện có TEXT/IMAGE/LOCATION/TEMPLATE cho dạng nội dung; nên phân biệt hai khái niệm thay vì trộn vào một tập. |
| `complaint_tickets.status` | Có | OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/CLOSED theo trang 44. `TicketStatusType` hiện chưa khớp đầy đủ. |
| `checkin_requests.status` | Có | PENDING/APPROVED/REJECTED/COUNTER_PROPOSED và trạng thái kết thúc theo thiết kế. Vẫn cần entity lưu booking, giờ hẹn, actor và version. |
| `outbox_events.status`, trạng thái mail/job/reminder | Có | Enum riêng theo từng vòng đời; không dùng một enum Status chung cho tất cả. |
| `mail_events.event_type`, mã nghiệp vụ email | Có nếu tập sự kiện do code quy định | ACCOUNT_CREATED/BOOKING_CONFIRMED/REFUND_SUCCEEDED/...; nội dung template và các version phải là dữ liệu DB. |
| `config_definitions.value_type/scope_type` | Có | STRING/INTEGER/BOOLEAN/... và GLOBAL/TENANT/PROPERTY theo tập được hỗ trợ. Giá trị minLength, TTL, số giờ nhắc là dữ liệu cấu hình, không phải hằng enum. |
| Provider/phương thức thanh toán | Có nếu tập tích hợp cố định | Enum mã Fake Bank/phương thức; credential, endpoint, giao dịch vẫn là cấu hình/dữ liệu. |
| `roles.code`, `permissions.code` | Có thể enum song song entity | Enum giúp code tránh sai mã; không bỏ bảng role/permission và quan hệ RBAC mà PDF yêu cầu quản trị. GUEST có thể chỉ là người chưa xác thực. |
| Loại giường/nhóm phòng cố định | Có điều kiện | Enum chỉ phù hợp khi danh sách cố định do code quản lý. Nếu HOST/admin thêm, sửa hoặc bản địa hóa thì dùng dữ liệu danh mục. |
| `room_types` | Không thay bằng enum | Có property, code, sức chứa, mô tả và chính sách. `RoomType` enum SINGLE/DOUBLE/... chỉ có thể là một phân loại phụ. |
| `amenities`, chính sách hủy, template, config values | Không | Các bản ghi có nội dung/thuộc tính riêng và có thể quản trị. |
| Token, webhook, read receipt, audit, lịch sử trạng thái | Không | Mỗi bản ghi biểu diễn một đối tượng/sự kiện thực tế. Chỉ type/status/purpose bên trong có thể enum. |

Ví dụ ánh xạ enum Java vào cột VARCHAR hiện có:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 40)
private PaymentStatusType status;
```

Không dùng ORDINAL cho trạng thái lưu lâu dài vì đổi thứ tự enum có thể đổi ý nghĩa dữ liệu. Java enum không bắt buộc PostgreSQL native ENUM. Truy vấn `pg_type/pg_enum` trong schema `public` hiện không trả về native enum; các cột trạng thái đã kiểm tra là VARCHAR. Những entity dùng `SqlTypes.NAMED_ENUM` cần migration/type DB tương ứng nếu được đưa vào sử dụng. Không tự thêm CHECK hoặc native enum trước khi rà giá trị hiện có và chọn tập mã chuẩn.

## 6. Những chỗ cần thống nhất trước khi thêm hàng loạt entity

1. **Hai mô hình phòng:** `Room` liên kết `RoomUnit`, trong khi cả hai đều có code, property, type và status. Chốt đâu là phòng vật lý mà booking/allocation/rate tham chiếu. PDF cho phép thiết kế khác tên, nhưng inventory phải nhất quán.
2. **Hai mô hình refund:** bảng thật là `refunds`, entity hiện tại trỏ `refund_requests`. Đổi tên `@Table` đơn lẻ chưa chứng minh đúng vì cột, trạng thái và lifecycle khác nhau.
3. **Cấu hình:** `system_settings` và `config_*` cùng tồn tại. Chốt ownership và nguồn đọc; mô hình cần scope, effective time, version, history theo UC-30.
4. **Media:** URL trong `room_images` không thay lifecycle PENDING/READY/cleanup của `media_assets`.
5. **Giá/lịch:** `room_rates`, `room_calendar.price_override`, `room_sale_blocks` cần quy tắc ưu tiên hoặc hợp nhất. Không cho quote và booking đọc hai nguồn khác nhau.
6. **Schema có trường trùng nghĩa:** DB chứa `users.password_hash/password`; `bookings.customer_id/user_id`, `check_in/checkin_date`, `expires_at/hold_expires_at`; `messages.body/content`; `notifications.recipient_id/user_id`. Đây là bằng chứng cấu trúc của nhiều mô hình cùng tồn tại, chưa xác định được script/phiên bản nào đã tạo chúng. Cần đối chiếu dữ liệu, constraint và code trước migration.
7. **Các cấu trúc được PDF nhắc nhưng chưa thấy bảng tên tương ứng:** `payment_intents`, `payment_events`, `mail_dead_letters`, fact/summary analytics. Có thể dùng `payments` cho intent nếu mô hình đủ; `bank_webhook_events` cho inbox event; trạng thái DLQ và lịch sử gửi thay bảng dead-letter riêng. Fact tables tùy tải. Phải chứng minh hành vi, không mặc định tạo thêm mọi tên bảng trong PDF.
8. **Fake Bank có ranh giới riêng:** bank_accounts, bank_transactions, bank_ledger_entries, bank_webhook_outbox thuộc service/schema/transaction phía Fake Bank (trang 37, 41). Chưa thấy trong schema public Booking này không đủ để kết luận thiếu toàn hệ thống; cũng không nên tự thêm chúng vào module Booking.

## 7. Thứ tự đề xuất

1. Chốt mô hình phòng/tồn kho, refund, cấu hình và tên cột chuẩn; kiểm tra dữ liệu/constraint trước migration.
2. Hoàn thiện identity token, media, allocation và payment/refund/webhook/reconciliation trước vì liên quan quyền, tồn kho và tiền.
3. Thêm mapping cho chat/complaint/check-in, config, email/reminder và audit theo schema đã chốt.
4. Chuẩn hóa các enum và migrate giá trị cũ; kiểm thử transition, retry và concurrency theo PDF.
5. Tách các tính năng AI/VIP/loyalty/voucher/collection/FX khỏi tiêu chí bắt buộc v3.0; giữ lịch sử và tham chiếu nếu sau này quyết định dọn.
6. Dùng Flyway versioned migration theo yêu cầu PDF; sau khi schema ổn định, dùng `ddl-auto: validate` để phát hiện lệch thay vì tự tích lũy cột bằng `update`.

Các bước trên là đề xuất triển khai, chưa được thực hiện trong lần đối chiếu này.
