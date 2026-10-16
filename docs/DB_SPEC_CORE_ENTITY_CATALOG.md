# Danh mục entity lõi sau rà soát PDF và ADR

Ngày: 16/10/2026. Nguồn: [ADR đã rà soát](DB_SPEC_ARCHITECTURE_DECISIONS.md), [PDF v3.0, 51 trang](Dac_ta_he_thong_dat_phong_spring.pdf), [đối chiếu DB](DB_SPEC_COMPARISON.md).

Đây là **thiết kế đích**, không phải mô tả chính xác entity/schema đang chạy. Chưa sửa Java, enum, cấu hình, DDL hoặc dữ liệu. Không tự áp dụng tên cột/constraint dưới đây lên DB hiện hữu. Các mục “đề xuất” là cách triển khai để đáp ứng hành vi PDF, không phải yêu cầu nguyên văn về tên bảng/class.

## 1. Quy ước và phạm vi

- Mỗi mục mô tả một đối tượng lưu trữ; bảng liên kết có thể dùng mapping thay `@Entity`. Có **48 đối tượng/bảng thuộc thiết kế lõi hoặc hỗ trợ lõi**, thêm **1 read model analytics tùy chọn**. Không đồng nhất con số này với số Java `@Entity` phải tạo.
- `M` = nền mutable: `id:Long` PK, `createdAt:Instant`, `updatedAt:Instant`, `version:Long` dùng optimistic locking. `E` = nền event: `id:Long` PK, `createdAt:Instant`; lịch sử/event đã chốt không được sửa nội dung, nhưng metadata xử lý queue có thể thay đổi. `C` = khóa ghép/khóa tự nhiên, khai báo riêng.
- Thuộc tính không có `?` là NOT NULL trong thiết kế đích; `?` là nullable và có thể bắt buộc theo trạng thái. FK có tên `...Id` tương ứng quan hệ được ghi trong từng mục, không cần đồng thời lưu hai field writable trùng một cột.
- `String` → VARCHAR/TEXT theo nội dung; độ dài được chốt ở DDL sau khi rà dữ liệu. `Long` → BIGINT, `Integer` → INTEGER, `Boolean` → BOOLEAN, `UUID` → UUID, `LocalDate` → DATE, `LocalTime` → TIME, `Instant` → TIMESTAMPTZ, `BigDecimal` → NUMERIC. Tiền đề xuất NUMERIC(19,2), phần trăm NUMERIC(5,2), tọa độ NUMERIC thích hợp. Đây là lựa chọn thiết kế, không phải độ chính xác PDF quy định.
- `Json` → JSONB với schema payload có version; dùng embeddable/value object/DTO có kiểm tra, không tự trở thành entity. Tất cả enum → VARCHAR + `EnumType.STRING`; CHECK tập mã chỉ thêm cùng migration đồng bộ enum.
- `UQ` = unique/unique index; `CK` = CHECK nội bộ một hàng; `FK` = ràng buộc tham chiếu; `TX` = kiểm tra nhiều hàng/aggregate trong transaction; `SV` = service/domain validation; `IX` = index gợi ý cần EXPLAIN theo workload. Không dùng CHECK giả để kiểm tra tổng refund, quyền sở hữu hoặc tổng nhiều hàng.
- Mặc định FK tới lịch sử giao dịch dùng RESTRICT/NO ACTION, không cascade REMOVE từ User/Property/Room vào booking/payment/audit. FK nullable không có nghĩa được xóa mất truy vết. Actor hệ thống có thể NULL user ID nhưng phải có actorType/source và correlation.
- Các FK many-to-one nên LAZY; không bắt buộc tạo collection inverse cho mọi quan hệ. Id class, enum, snapshot, quote DTO và aggregate báo cáo không phải `@Entity`.
- Tenant chưa có mô hình thật: giữ enum scope hỗ trợ GLOBAL/TENANT/PROPERTY theo PDF nhưng **không cho ghi TENANT scope khi chưa có cơ chế xác minh tenant**. Không tạo FK đa hình giả tới một bảng tenant chưa tồn tại.

## 2. Identity và RBAC

### 2.1. User — `users` — M

- **Thuộc tính:** `email:String`, `passwordHash:String`, `fullName:String`, `phone:String?`, `avatarAssetId:Long?` (tùy chọn hồ sơ), `status:UserStatus`, `emailVerifiedAt:Instant?`, `tokenVersion:Long`; `emailVerifiedAt` map cột chuẩn `verified_at`. Không lưu thêm password plaintext hoặc cột password thứ hai.
- **Quan hệ:** N–N Role qua UserRole; 1–N token, property sở hữu, booking khách. Avatar nếu dùng là N–1 MediaAsset có scope hồ sơ riêng, không ép phải có property.
- **Enum:** UserStatus: PENDING_VERIFICATION/ACTIVE/IN_ACTIVE/LOCKED.
- **Constraint:** UQ lower(trim(email)); SV chuẩn hóa email trước ghi, hash qua PasswordEncoder, registration role CUSTOMER từ server; CK tokenVersion >= 0; trạng thái đăng nhập và token revocation kiểm tra ở SV. Admin sửa role/status qua use case riêng, không mass assignment.
- **Nguồn:** UC-01→03/20, trang 4→6, 21, 39. Trường avatar là hỗ trợ hồ sơ, không bắt buộc để booking chạy.

### 2.2. Role — `roles` — M

- **Thuộc tính:** `code:String`, `name:String`, `description:String?`.
- **Quan hệ:** N–N User qua UserRole; N–N Permission qua RolePermission.
- **Enum:** RoleCode tùy chọn hỗ trợ mã CUSTOMER/HOST/SUPPORT/ADMIN trong code; bảng vẫn là nguồn quản trị role. GUEST là principal chưa xác thực, không bắt buộc một hàng role; USER cũ không tự coi là CUSTOMER khi chưa có mapping migration.
- **Constraint:** UQ code; SV không xóa cứng role đang sử dụng, cấp/thu có audit; không suy ra mọi permission từ chuỗi tên ADMIN.

### 2.3. Permission — `permissions` — M

- **Thuộc tính:** `code:String`, `name:String`, `description:String?`.
- **Quan hệ:** N–N Role qua RolePermission.
- **Enum:** PermissionCode tùy chọn cho danh sách operation do backend hỗ trợ, không thay bảng dữ liệu.
- **Constraint:** UQ code; SV cấp permission không mở rộng vượt policy ủy quyền; không hard-delete permission đang dùng. Nguồn UC-03/20.

### 2.4. UserRole — `user_roles` — C hoặc join mapping

- **Thuộc tính:** PK `(userId:Long, roleId:Long)`, `assignedAt:Instant`.
- **Quan hệ:** N–1 User; N–1 Role. Đề xuất map entity khóa ghép nếu muốn đọc/ghi assignedAt; nếu giữ JoinTable thì metadata và audit phải được quản lý rõ.
- **Enum:** không.
- **Constraint:** PK chống gán trùng; FK user/role; TX gán/thu quyền và tăng tokenVersion theo session policy, audit actor/reason qua AuditLog. assignedAt là thời điểm gán hiện tại; lịch sử thu/gán nhiều lần nằm trong audit, không thể suy ra từ một hàng join.

### 2.5. RolePermission — `role_permissions` — C

- **Thuộc tính:** PK `(roleId:Long, permissionId:Long)`.
- **Quan hệ:** N–1 Role, N–1 Permission; EmbeddedId/MapsId hoặc JoinTable nếu chỉ cần liên kết.
- **Enum:** không.
- **Constraint:** PK + FK; TX thay quyền và audit/invalidate quyền hiệu lực; không dùng cascade REMOVE tới User/Permission.

### 2.6. VerificationToken — `verification_tokens` — E

- **Thuộc tính:** `userId:Long`, `tokenHash:String`, `purpose:VerificationPurpose`, `expiresAt:Instant`, `usedAt:Instant?`, `attempts:Integer`, `revokedAt:Instant?` (đề xuất để quản lý resend).
- **Quan hệ:** N–1 User.
- **Enum:** EMAIL_VERIFICATION/PASSWORD_RESET; mục đích khác chỉ thêm khi có use case được bật.
- **Constraint:** UQ tokenHash; CK expiresAt > createdAt, attempts >= 0; TX consume đúng một lần và cập nhật user; SV hạn lần thử/resend theo policy, không log token. Mã OTP ngắn cần hash/HMAC có bí mật để giảm brute-force ngoại tuyến. Không duy trì OtpVerification như nguồn thứ hai.
- **Nguồn:** UC-01/32/34; reset là hỗ trợ luồng tài khoản nếu triển khai. Outbox/mail không chứa token plaintext; nếu cần gửi link bất đồng bộ dùng payload bí mật mã hóa có TTL/giới hạn truy cập hoặc tham chiếu kho bí mật, DB xác minh vẫn chỉ có hash.

### 2.7. RefreshToken — `refresh_tokens` — E

- **Thuộc tính:** `userId:Long`, `tokenHash:String`, `familyId:UUID` (đề xuất quản lý reuse), `deviceLabel:String?`, `expiresAt:Instant`, `revokedAt:Instant?`, `replacedById:Long?`.
- **Quan hệ:** N–1 User; self N–1 token kế tiếp.
- **Enum:** không cần, hiệu lực suy ra từ expiry/revokedAt.
- **Constraint:** UQ tokenHash; UQ replacedById khi khác NULL (đề xuất một chuỗi rotation); CK expiry > createdAt, replacedById <> id; TX rotate cũ/mới nguyên tử, chống reuse theo family; SV user/status/tokenVersion. IX userId/familyId, expiry cho cleanup. Nguồn UC-02.

## 3. Property, catalog, giá và media

### 3.1. Property — `properties` — M

- **Thuộc tính:** `ownerId:Long`, `name:String`, `slug:String?`, `address:String`, `city:String`, `country:String?`, `latitude:BigDecimal?`, `longitude:BigDecimal?`, `description:String?`, `timezone:String`, `status:PropertyStatus`, `checkinLocal:LocalTime`, `checkoutLocal:LocalTime`, `publicContact:String?`, `cancellationPolicyId:Long?`.
- **Quan hệ:** N–1 User owner; 1–N RoomType/Room; N–1 CancellationPolicy mặc định; 1–N MediaAsset.
- **Enum:** DRAFT/PUBLISHED/PAUSED/ARCHIVED.
- **Constraint:** UQ slug nếu dùng; CK tọa độ đúng miền; SV IANA timezone hợp lệ, owner scope; publish chỉ khi đủ room/type/rate/policy/ảnh READY. Policy có thể NULL lúc DRAFT, phải hợp lệ khi publish. Không xóa lịch sử hoặc tự hủy booking khi pause/archive. Nguồn UC-04.

### 3.2. RoomTypeEntity — `room_types` — M

- **Thuộc tính:** `propertyId:Long`, `code:String`, `name:String`, `maxGuests:Integer`, `maxAdults:Integer?`, `maxChildren:Integer?`, `bedType:String`, `description:String?`, `active:Boolean`, `cancellationPolicyId:Long?`.
- **Quan hệ:** N–1 Property, N–1 CancellationPolicy override, 1–N Room/RoomRate; N–N Amenity qua room_type_amenities.
- **Enum:** không dùng enum RoomType để thay entity. bedType có thể dùng enum khi danh mục cố định, chưa ép enum vì HOST có thể quản lý nội dung danh mục.
- **Constraint:** UQ (propertyId, code); CK maxGuests > 0, maxAdults/maxChildren >= 0 khi có; SV giới hạn người lớn/trẻ em trong tổng capacity và quy tắc sức chứa; TX giảm sức chứa/đóng bán phải kiểm tra booking/hold tương lai. Mỗi type phải cùng property với room/policy dùng cho nó. Nguồn UC-05/08.

### 3.3. Room — `rooms` — M

- **Thuộc tính:** `propertyId:Long`, `roomTypeId:Long`, `roomCode:String`, `name:String?`, `status:RoomOperationalStatus`, `floor:String?`, `notes:String?`.
- **Quan hệ:** N–1 Property, N–1 RoomTypeEntity; 1–N Booking/RoomAllocation/RoomSaleBlock/RoomImage; N–N Amenity qua room_amenities.
- **Enum:** ACTIVE/INACTIVE/MAINTENANCE. Không lưu OCCUPIED như trạng thái chung cho tất cả ngày.
- **Constraint:** UQ (propertyId, roomCode); FK room/type/property; đề xuất composite FK `(room_type_id, property_id)` → type `(id, property_id)` (cần UQ đích) để chặn room thuộc sai property. TX đổi status/type/property không làm sai booking đã bán, không chuyển owner/location để viết lại lịch sử. Lock Room cho đường ghi tồn kho. Không dùng room_units như nguồn thứ hai. Nguồn UC-05/09/15.

### 3.4. Amenity — `amenities` — M

- **Thuộc tính:** `code:String`, `name:String`, `icon:String?`, `active:Boolean` (đề xuất retire danh mục).
- **Quan hệ:** N–N Room và RoomTypeEntity qua bảng join.
- **Enum:** không, danh mục được admin quản trị.
- **Constraint:** UQ code; SV validate icon/path, không xóa cứng liên kết/lịch sử đang cần; thay đổi danh mục invalidate cache. Nguồn UC-05/07/20.

### 3.5. RoomAmenity — `room_amenities` — C

- **Thuộc tính:** PK `(roomId:Long, amenityId:Long)`.
- **Quan hệ:** N–1 Room, N–1 Amenity; entity khóa ghép hiện tại hoặc JoinTable đều hợp lệ.
- **Enum:** không.
- **Constraint:** PK/FK; owner scope khi ghi; không tạo bản sao amenity theo từng phòng. Quyết định catalog: tiện ích hiệu lực = hợp của tiện ích type và tiện ích bổ sung của room; không có cơ chế loại trừ ngầm.

### 3.6. RoomTypeAmenity — `room_type_amenities` — C, không bắt buộc entity riêng

- **Thuộc tính:** PK `(roomTypeId:Long, amenityId:Long)`.
- **Quan hệ:** N–1 RoomTypeEntity, N–1 Amenity; dùng @JoinTable nếu chỉ có hai FK.
- **Enum:** không.
- **Constraint:** PK/FK; owner scope của type; sửa liên kết invalidate search/detail. Nguồn UC-05.

### 3.7. CancellationPolicy — `cancellation_policies` — M

- **Thuộc tính:** `propertyId:Long`, `name:String`, `policyRevision:Integer`, `effectiveAt:Instant`, `active:Boolean` (đề xuất retire).
- **Quan hệ:** N–1 Property; 1–N CancellationPolicyRule; được Property/RoomType tham chiếu. Quan hệ property/policy được tạo hai bước khi property DRAFT để tránh FK vòng lúc khởi tạo.
- **Enum:** không cần enum tên chính sách.
- **Constraint:** UQ (propertyId, name, policyRevision); CK revision > 0; bản đã có hiệu lực/được snapshot không sửa rule tại chỗ, tạo revision mới; TX chọn policy hiệu lực, snapshot đầy đủ khi booking. `policy_json` cũ không là nguồn tính thứ hai. Nguồn UC-04/08/12.

### 3.8. CancellationPolicyRule — `cancellation_policy_rules` — E

- **Thuộc tính:** `policyId:Long`, `daysBeforeCheckin:Integer`, `refundPercent:BigDecimal`.
- **Quan hệ:** N–1 CancellationPolicy.
- **Enum:** không.
- **Constraint:** UQ (policyId, daysBeforeCheckin); CK days >= 0, percent từ 0 đến 100. SV dùng timezone property và thời điểm check-in chuẩn, chốt biên so sánh và làm tròn trong snapshot. Quy tắc thiết kế: chọn ngưỡng lớn nhất không vượt thời gian còn lại; nếu không có rule phù hợp thì không cho publish policy, phải có rule fallback. Ngoại lệ đang CHECKED_IN do SUPPORT/ADMIN xử lý riêng. Nguồn UC-12; cách chọn rule là thiết kế cần kiểm thử, PDF không ấn định thuật toán mốc ngày.

### 3.9. RoomRate — `room_rates` — M

- **Thuộc tính:** `roomTypeId:Long`, `stayDate:LocalDate`, `amount:BigDecimal`, `currency:String`, `closedToSale:Boolean`, `priceVersion:Long`.
- **Quan hệ:** N–1 RoomTypeEntity; không đồng thời ghi giá độc lập theo roomId.
- **Enum:** không cần Currency enum khi core chỉ VND; có thể dùng value object tiền tệ.
- **Constraint:** UQ (roomTypeId, stayDate, currency); CK amount >= 0, currency = 'VND' trong core, priceVersion >= 0. TX mỗi đêm đủ rate và mở bán; total từ server, không fallback base_price/price_override. Sửa rate tăng version/cache invalidation sau commit; snapshot booking không đổi. Nguồn UC-05/08/09.

### 3.10. RoomSaleBlock — `room_sale_blocks` — M

- **Thuộc tính:** `roomId:Long`, `dateFrom:LocalDate`, `dateTo:LocalDate`, `reason:String`, `createdById:Long`, `cancelledAt:Instant?`, `cancelledById:Long?` (đề xuất audit thay xóa).
- **Quan hệ:** N–1 Room, N–1 User actor.
- **Enum:** không bắt buộc; reason là nội dung, không ép danh mục chưa được định nghĩa.
- **Constraint:** CK dateFrom < dateTo, khoảng [from,to); TX khóa Room, kiểm tra allocation/booking tương lai trước đóng bán; không tạo block chồng booking còn chiếm kho. Khoảng block chồng nhau không tự gây bán trùng nhưng SV có thể hợp nhất để tránh thừa. IX (roomId, dateFrom, dateTo). Nguồn UC-05/24.

### 3.11. MediaAsset — `media_assets` — M

- **Thuộc tính:** `createdById:Long`, `propertyId:Long?`, `roomId:Long?`, `objectKey:String`, `contentType:String`, `sizeBytes:Long`, `checksumSha256:String?`, `status:MediaAssetStatus`, `finalizedAt:Instant?`, `uploadExpiresAt:Instant`, `deletedAt:Instant?`.
- **Quan hệ:** N–1 User uploader; N–1 Property/Room nếu là ảnh catalog; được RoomImage/Message và extension tham chiếu. Asset hồ sơ không cần property nhưng vẫn kiểm tra scope uploader.
- **Enum:** PENDING/READY/DELETING/DELETED/PENDING_EXPIRED/FAILED.
- **Constraint:** UQ objectKey; CK sizeBytes > 0; SV MIME/size/checksum thật khi finalize, asset room phải cùng property; READY cần đã xác minh/finalizedAt; chỉ asset có quyền và READY được gắn vào nội dung. Binary ở MinIO; không giữ DB transaction khi upload. Cleanup theo pending TTL; chỉ xóa object khi quan hệ sử dụng/policy cho phép, không làm mất bằng chứng chat. Nguồn UC-06/28.

### 3.12. RoomImage — `room_images` — M

- **Thuộc tính:** `roomId:Long`, `assetId:Long`, `sortOrder:Integer`, `cover:Boolean` (đề xuất một ảnh cover).
- **Quan hệ:** N–1 Room, N–1 MediaAsset.
- **Enum:** không cần media type trùng với MIME nếu chỉ hỗ trợ ảnh; có thể thêm content type phân loại nếu bật video.
- **Constraint:** UQ (roomId, assetId); partial UQ roomId WHERE cover; CK sortOrder >= 0; SV asset READY, đúng owner/scope; URL sinh khi đọc, không dùng URL làm bằng chứng upload hoàn tất. Ảnh property có thể query trực tiếp MediaAsset theo propertyId; không bắt buộc thêm PropertyImage. Nguồn UC-04/06.

## 4. Booking và inventory

### 4.1. Booking — `bookings` — M

- **Thuộc tính:** `bookingCode:String`, `customerId:Long`, `propertyId:Long`, `roomTypeId:Long`, `roomId:Long`, `checkIn:LocalDate`, `checkOut:LocalDate`, `guestCount:Integer`, `status:BookingStatusType`, `expiresAt:Instant`, `subtotalAmount:BigDecimal`, `taxAmount:BigDecimal`, `feeAmount:BigDecimal`, `totalAmount:BigDecimal`, `currency:String`, `priceVersion:Long`, `cancellationPolicySnapshot:Json`, `propertyTimezoneSnapshot:String`, `checkinLocalSnapshot:LocalTime`, `sourceChannel:BookingSourceChannel`, `guestSegmentSnapshot:GuestSegment`, `specialRequest:String?`, `confirmedAt:Instant?`, `cancelledAt:Instant?`, `cancelledById:Long?`, `cancelReason:String?`, `cancellationFee:BigDecimal?`, `refundableAmount:BigDecimal?`, `actualCheckIn:Instant?`, `actualCheckOut:Instant?`, `checkedInById:Long?`, `checkedOutById:Long?`, `checkinVerificationMethod:String?`.
- **Quan hệ:** N–1 User customer, Property, RoomType, Room; 1–N BookingNight/History/Payment; 0–1 allocation trong core một phòng/booking; actor tham chiếu User. property/type là chiều snapshot tham chiếu, phải khớp room lúc tạo; không cập nhật lịch sử theo type/property mới của room.
- **Enum:** PENDING_PAYMENT/CONFIRMED/CHECKED_IN/COMPLETED/CANCELLATION_PENDING/REFUND_PENDING/CANCELLED/EXPIRED/NEEDS_ATTENTION/NO_SHOW; source channel và guest segment là phân loại báo cáo đích.
- **Constraint:** UQ bookingCode; CK checkIn < checkOut, guestCount > 0, các khoản tiền >= 0, currency = VND, tổng = subtotal + tax + fee cho core chưa bật discount. TX đủ BookingNight, tổng đêm khớp subtotal, capacity/availability/policy hợp lệ; idempotency ở bảng riêng theo actor/operation. expiresAt giữ mốc gốc để audit sau khi confirmed, không tự xóa; chỉ dùng cho hold/cutoff theo state machine.
- **Hành vi:** snapshot giữ nguyên; quote là DTO có expiry/hash/version, không cần Quote entity. Check-in/out và cancellation có actor/history; NO_SHOW không tự suy ra từ việc thiếu timestamp. Giải phóng kho khi cancel/expire và khi kết thúc lưu trú được xác nhận; không coi NEEDS_ATTENTION là tự chiếm lại kho đã giải phóng. Nguồn UC-08→15/22→25.
- **Thiết kế bổ sung:** snapshot timezone/giờ check-in chống đổi nghĩa lịch sử; guest segment NEW/RETURNING được chốt dưới lock customer hoặc cơ chế tuần tự tương đương: RETURNING nếu có booking COMPLETED trước createdAt, NEW nếu chưa có. Không dùng cờ user hiện tại; dữ liệu backfill không chứng minh được dùng UNKNOWN.

### 4.2. BookingNight — `booking_nights` — E

- **Thuộc tính:** `bookingId:Long`, `stayDate:LocalDate`, `nightlyRate:BigDecimal`, `taxAmount:BigDecimal`, `feeAmount:BigDecimal`, `currency:String`, `priceVersion:Long`.
- **Quan hệ:** N–1 Booking.
- **Enum:** không.
- **Constraint:** UQ (bookingId, stayDate); CK tiền >= 0/currency VND; TX stayDate trong [checkIn,checkOut), có đúng một hàng/đêm và tổng khớp booking. Không sửa snapshot khi rate hiện hành thay đổi; lỗi ghi một đêm rollback cả booking/hold/idempotency. Nguồn UC-08/09.

### 4.3. BookingStatusHistory — `booking_status_history` — E

- **Thuộc tính:** `bookingId:Long`, `fromStatus:BookingStatusType?`, `toStatus:BookingStatusType`, `actorId:Long?`, `actorType:ActorType`, `source:BookingStatusChangeSource`, `reason:String?`, `occurredAt:Instant`, `correlationId:String`, `eventId:UUID` (đề xuất dedupe).
- **Quan hệ:** N–1 Booking, N–1 User actor khi là người dùng.
- **Enum:** trạng thái Booking; actor USER/SYSTEM/PROVIDER; source CUSTOMER/HOST/SUPPORT/ADMIN/SCHEDULER/WEBHOOK/RECONCILIATION/SYSTEM.
- **Constraint:** UQ eventId; initial fromStatus NULL, các lần sau phải khớp trạng thái trước; history append-only cùng TX transition, không phải tạo sẵn một hàng cho mỗi enum. IX bookingId/occurredAt. Nguồn UC-09/11/15, trang 39.

### 4.4. RoomAllocation — `room_allocations` — M

- **Thuộc tính:** `bookingId:Long`, `roomId:Long`, `stay:DateRange`, `status:AllocationStatus`, `releasedAt:Instant?`, `releaseReason:String?`.
- **Quan hệ:** N–1 Room; 1–1 Booking theo core một phòng/booking. DateRange là value object/PostgreSQL daterange, không phải entity.
- **Enum:** HELD/CONFIRMED/RELEASED (đề xuất kỹ thuật); CHECKED_IN vẫn allocation CONFIRMED.
- **Constraint:** UQ bookingId; CK daterange không rỗng, hữu hạn, chuẩn [), status RELEASED iff releasedAt khác NULL. TX roomId/range khớp Booking, khóa Room khi kiểm tra overlap và ghi/release; kiểm tra sale block và operational status. Đề xuất exclusion `(room_id WITH =, stay WITH &&) WHERE released_at IS NULL` sau chuẩn hóa dữ liệu/extension cần thiết. Không dùng now() trong predicate.
- **Hành vi:** hết expiresAt không tự bỏ exclusion; expiry/reconcile phải chuyển state và releasedAt trong TX trước khi tái bán. Callback không hồi sinh allocation đã release. Policy đang hủy: thiết kế mặc định release sau khi chốt cancellation và chuyển REFUND_PENDING; tiền vẫn tiếp tục xử lý riêng. COMPLETED/NO_SHOW release theo transition được audit, vẫn giữ booking nights để thống kê lịch sử. Nguồn UC-09/11/12/15, trang 39.

## 5. Payment, refund, độ tin cậy và audit

### 5.1. Payment — `payments` — M

- **Thuộc tính:** `bookingId:Long`, `provider:PaymentProvider`, `merchantId:String`, `idempotencyKey:String`, `requestHash:String`, `providerReference:String?`, `method:PaymentMethodType`, `amount:BigDecimal`, `capturedAmount:BigDecimal`, `currency:String`, `status:PaymentStatusType`, `isOpen:Boolean`, `intentExpiresAt:Instant`, `capturedAt:Instant?`, `failureCode:String?`, `failureMessage:String?`, `correlationId:String`.
- **Quan hệ:** N–1 Booking (không OneToOne); 1–N Refund/Webhook/Reconciliation. Một hàng là một logical intent; id cũng là paymentIntentId. Retry mạng không tạo hàng mới.
- **Enum:** provider FAKE_BANK; method FAKE_BANK_DEBIT (mã thiết kế); status INITIATED/PROCESSING/SUCCEEDED/FAILED/TIMED_OUT/UNKNOWN/RECONCILING/NEEDS_ATTENTION. Refund không đổi Payment SUCCEEDED thành REFUNDED; số tiền hoàn tính riêng.
- **Constraint:** UQ (provider, merchantId, idempotencyKey); UQ (provider, merchantId, providerReference) khi có; partial UQ bookingId WHERE isOpen. CK amount > 0, 0 <= capturedAmount <= amount, currency VND. Core thanh toán toàn bộ booking: TX amount = booking.totalAmount; chặn intent mới khi đã thu đủ hoặc intent trước chưa rõ kết quả. capturedAmount cho phép ghi đúng capture bất thường/partial từ provider để đối soát, không tự confirm booking chưa thu đủ.
- **Hành vi:** lock Booking rồi Payment theo thứ tự chuẩn; isOpen true cho INITIATED/PROCESSING/UNKNOWN/RECONCILING và ca chưa rõ; TIMED_OUT không đủ bằng chứng để đóng. Chỉ đóng intent khi có kết quả cuối được xác minh hoặc provider xác nhận hết hiệu lực không thể debit muộn. Ngăn nhiều intent mới dưới lock booking; provider capture thừa/bất thường ghi thật và đưa bù trừ, không bỏ mất giao dịch chỉ để giữ constraint. Không giữ DB lock khi gọi HTTP. Nguồn UC-10/35/36.

### 5.2. Refund — `refunds` — M

- **Thuộc tính:** `paymentId:Long`, `bookingId:Long`, `refundCode:String`, `provider:PaymentProvider`, `merchantId:String`, `idempotencyKey:String`, `requestHash:String`, `providerReference:String?`, `amount:BigDecimal`, `currency:String`, `status:RefundStatusType`, `reservationActive:Boolean`, `reason:String`, `requestedById:Long?`, `processedAt:Instant?`, `failureCode:String?`, `failureMessage:String?`, `nextAttemptAt:Instant?`, `attemptCount:Integer`, `correlationId:String`.
- **Quan hệ:** N–1 Payment, Booking và User requester; SV payment thuộc booking. Provider/merchant là namespace được snapshot từ Payment, phải khớp và ổn định trong lifetime.
- **Enum:** REQUESTED/PROCESSING/SUCCEEDED/FAILED/UNKNOWN/RECONCILING/NEEDS_ATTENTION. REQUESTED là yêu cầu thực thi refund, không mặc nhiên là quy trình phê duyệt riêng.
- **Constraint:** UQ refundCode, UQ (paymentId, idempotencyKey), UQ (provider, merchantId, providerReference) khi reference có giá trị; provider refund key gửi Bank chọn refundCode ổn định để không trùng giữa payment. Không thể tạo unique qua JOIN nên namespace được lưu rõ trên Refund. CK amount > 0, currency VND, attemptCount >= 0; SUCCEEDED/FAILED cuối thì reservationActive=false; REQUESTED/PROCESSING/UNKNOWN/RECONCILING thì reservationActive=true; NEEDS_ATTENTION chưa rõ cũng giữ reservation.
- **TX hạn mức:** lock Payment, tính `SUM(amount WHERE status=SUCCEEDED) + SUM(amount WHERE reservationActive=true) + newAmount <= capturedAmount`; tạo REQUESTED/reservationActive=true trước gọi Bank. UNKNOWN/PROCESSING/RECONCILING và NEEDS_ATTENTION chưa rõ tiếp tục giữ reservation. Thành công chuyển reservation thành consumed trong cùng TX; chắc chắn không hoàn mới release. Không cộng cả SUCCEEDED và reservation của cùng một refund. Failed retryable chưa xác minh không được giải phóng như FAILED cuối.
- **Hành vi:** retry cùng refund/key, processedAt là mốc provider thực hoàn dùng báo cáo, không chỉ mốc request; callback trùng không cộng lại. Nguồn UC-13 bước 02 trang 15, UC-36.

### 5.3. BankWebhookEvent — `bank_webhook_events` — E có metadata xử lý

- **Thuộc tính:** `provider:PaymentProvider`, `merchantId:String`, `eventId:String`, `eventType:BankEventType`, `providerReference:String`, `paymentId:Long?`, `refundId:Long?`, `payloadHash:String`, `payload:Json?` (đã loại bí mật), `verified:Boolean`, `providerOccurredAt:Instant?`, `receivedAt:Instant`, `processedAt:Instant?`, `processingStatus:InboxStatus`, `attemptCount:Integer`, `nextAttemptAt:Instant?`, `leaseOwner:String?`, `leaseUntil:Instant?`, `lastError:String?`.
- **Quan hệ:** N–1 Payment hoặc Refund sau khi match; chưa match vẫn lưu inbox/reconcile an toàn. Chỉ một target nghiệp vụ phù hợp event; event chưa match không được đổi trạng thái payment.
- **Enum:** PAYMENT_SUCCEEDED/PAYMENT_FAILED/REFUND_SUCCEEDED/REFUND_FAILED và sự kiện processing nếu provider hỗ trợ; inbox RECEIVED/PROCESSING/PROCESSED/RETRY/REJECTED/NEEDS_ATTENTION.
- **Constraint:** UQ (provider, merchantId, eventId); CK attemptCount >= 0; TX inbox mark processed cùng business update/outbox. SV HMAC/timestamp/replay window/reference/amount/currency; cùng event ID khác payload bị từ chối/audit. Không đánh processed trước commit; retry worker claim/lease. Nguồn UC-10/35/36; đây là inbox Booking, không phải outbox Bank.

### 5.4. ReconciliationCase — `reconciliation_cases` — M

- **Thuộc tính:** `bookingId:Long?`, `paymentId:Long?`, `refundId:Long?`, `operationKey:String`, `caseType:ReconciliationType`, `status:ReconciliationStatus`, `attemptCount:Integer`, `detail:Json`, `nextAttemptAt:Instant?`, `resolvedAt:Instant?`, `assignedToId:Long?`, `resolution:String?`.
- **Quan hệ:** N–1 Booking/Payment/Refund theo lỗi, N–1 User xử lý; không bắt buộc đủ cả ba nếu provider event chưa match.
- **Enum:** loại PAYMENT_UNKNOWN/CAPTURE_WITHOUT_CONFIRMATION/LATE_CAPTURE/REFUND_UNKNOWN/AMOUNT_MISMATCH/UNMATCHED_EVENT; state OPEN/PROCESSING/RESOLVED/NEEDS_ATTENTION.
- **Constraint:** UQ operationKey do server tạo theo provider operation + discrepancy; CK attemptCount >= 0; SV ít nhất có operation reference trong detail hoặc FK để đối soát; đóng phải có resolution; retry/backoff có giới hạn; mọi can thiệp thủ công cần quyền/reason/audit. Nguồn UC-13/35/36.

### 5.5. IdempotencyRecord — `idempotency_records` — E có metadata hoàn tất

- **Thuộc tính:** `scope:String`, `actorKey:String`, `idempotencyKey:String`, `requestHash:String`, `status:IdempotencyStatus`, `responseCode:Integer?`, `responseBody:Json?`, `resourceType:String?`, `resourceId:Long?`, `completedAt:Instant?`, `expiresAt:Instant?` (retention policy).
- **Quan hệ:** tham chiếu resource đa hình, không giả định một FK resourceId tới tất cả bảng; actorKey lấy từ principal/service đã xác thực.
- **Enum:** IN_PROGRESS/COMPLETED; FAILED chỉ thêm nếu semantics retry được chốt, không giữ lỗi transient thành kết quả vĩnh viễn.
- **Constraint:** UQ (scope, idempotencyKey), trong đó **scope gồm actorKey + operation/version**; payload canonical hash; cùng key khác payload conflict. TX tạo booking/hold và record kết quả nguyên tử; không xóa dedupe record trước cửa sổ retry nghiệp vụ. Response không chứa token/password/URL bí mật. Nguồn UC-09/10/26/28/36.

### 5.6. OutboxEvent — `outbox_events` — E có metadata queue

- **Thuộc tính:** `eventId:UUID`, `eventType:String`, `aggregateType:String`, `aggregateId:Long`, `payload:Json`, `payloadVersion:Integer`, `status:OutboxStatus`, `retryCount:Integer`, `availableAt:Instant`, `processedAt:Instant?`, `leaseOwner:String?`, `leaseUntil:Instant?`, `lastError:String?`.
- **Quan hệ:** aggregate reference đa hình; payload chứa ID cần thiết, không phải FK JPA giả cho mọi aggregate.
- **Enum:** PENDING/PROCESSING/PROCESSED/RETRY/DLQ. Event type whitelist do code quản lý, có thể enum theo module thay một enum khổng lồ.
- **Constraint:** UQ eventId; CK retryCount >= 0/payloadVersion > 0; TX ghi cùng nghiệp vụ, worker claim SKIP LOCKED hoặc lease bền vững; consumer dedupe. Không giữ transaction mạng/SMTP. Không đặt token plaintext trong JSON. IX status/availableAt. Nguồn UC-09/17/32.

### 5.7. AuditLog — `audit_logs` — E append-only

- **Thuộc tính:** `eventId:UUID`, `actorId:Long?`, `actorType:ActorType`, `action:String`, `entityType:String`, `entityId:Long`, `fromStatus:String?`, `toStatus:String?`, `beforeState:Json?`, `afterState:Json?`, `reason:String?`, `requestId:String`, `correlationId:String`, `occurredAt:Instant`, `outcome:AuditOutcome`.
- **Quan hệ:** actor User nếu có; entity reference đa hình. Dùng để lưu lịch sử ComplaintTicket/CheckinRequest có đủ entityType/id + from/to + actor + thời điểm; không bắt buộc thêm hai bảng history.
- **Enum:** actor USER/SYSTEM/PROVIDER; outcome ATTEMPT/SUCCESS/FAILURE. action/entityType là whitelist theo module.
- **Constraint:** UQ eventId; TX SUCCESS chỉ ghi cùng commit nghiệp vụ hoặc outbox nhất quán; REQUIRES_NEW nếu dùng chỉ phản ánh ATTEMPT/FAILURE đúng nghĩa. SV masking password/token/PII/signed URLs; API thông thường không sửa/xóa, retention có kiểm soát. IX entityType/entityId/occurredAt, correlationId. Nguồn UC-19/28/29.

## 6. Lưu trú, review và notification

### 6.1. Review — `reviews` — M

- **Thuộc tính:** `bookingId:Long`, `propertyId:Long`, `customerId:Long`, `rating:Integer`, `content:String`, `status:ReviewStatusType`, `hostReply:String?`, `hostRepliedById:Long?`, `hostRepliedAt:Instant?`, `moderatedById:Long?`, `moderatedAt:Instant?`, `moderationReason:String?`.
- **Quan hệ:** 1–1 Booking, N–1 Property/User; extension ReviewMedia nếu hỗ trợ ảnh.
- **Enum:** PENDING_MODERATION/PUBLISHED/HIDDEN (đề xuất đáp ứng ẩn/khôi phục).
- **Constraint:** UQ bookingId; CK rating theo thang đã chốt (đề xuất 1..5, PDF không ấn định); TX booking COMPLETED, đúng customer/property, chỉ một review; SV HOST không sửa review gốc, chỉ reply theo owner scope; moderation có audit và giữ bản gốc. Nguồn UC-16.

### 6.2. Notification — `notifications` — E có readAt

- **Thuộc tính:** `eventId:UUID`, `recipientId:Long`, `eventType:String`, `title:String`, `content:String`, `resourceType:String`, `resourceId:Long`, `readAt:Instant?`.
- **Quan hệ:** N–1 User recipient; source event/resource reference theo whitelist, không cascade delete lịch sử.
- **Enum:** eventType có thể NotificationType theo các business event đã hỗ trợ; không cần thêm boolean isRead làm nguồn thứ hai.
- **Constraint:** UQ (eventId, recipientId); TX consumer tạo notification/mark outbox nguyên tử hoặc idempotent; SV recipient từ quan hệ thật, query theo principal; mark read không làm timestamp lùi. Không chứa secret hoặc URL private lâu dài. Nguồn UC-17.

## 7. Conversation, complaint và check-in request

### 7.1. Conversation — `conversations` — M

- **Thuộc tính:** `propertyId:Long`, `bookingId:Long?`, `customerId:Long`, `hostId:Long`, `type:ConversationType`, `status:ConversationStatus`, `lastMessageSeq:Long`, `closedAt:Instant?`.
- **Quan hệ:** N–1 Property/Booking/User customer/User host; 1–N Participant/Message/Complaint/CheckinRequest. hostId ghi bối cảnh mở thread, không đủ để cấp quyền mãi sau đổi owner.
- **Enum:** INQUIRY/BOOKING_SUPPORT; OPEN/CLOSED.
- **Constraint:** CK seq >= 0; partial UQ (bookingId, type) WHERE bookingId IS NOT NULL AND status='OPEN'; SV inquiry khi property published hoặc booking thuộc customer; TX cấp seq đơn điệu, tạo message + outbox; owner đổi phải cập nhật/revoke quyền tham gia và audit, không dựa hostId cũ. Nguồn UC-27/28.

### 7.2. ConversationParticipant — `conversation_participants` — C

- **Thuộc tính:** PK `(conversationId:Long, userId:Long)`, `joinedAt:Instant`, `leftAt:Instant?`, `participantRole:ParticipantRole`, `assignedById:Long?`, `version:Long`.
- **Quan hệ:** N–1 Conversation/User; actor assignment N–1 User nếu có. Lịch sử nhiều lần tham gia/rời nằm AuditLog, hàng này là membership hiện tại/gần nhất.
- **Enum:** CUSTOMER/HOST/SUPPORT.
- **Constraint:** CK leftAt >= joinedAt khi có; TX grant/revoke/assignment có audit. SV HOST phải còn owner/co-host được phép theo feature đang bật; SUPPORT có phân công, không được tự tham gia bằng role chung; participant đã rời không tự có quyền đọc. Nguồn UC-27/28.

### 7.3. Message — `messages` — E append-only

- **Thuộc tính:** `conversationId:Long`, `senderId:Long`, `clientMessageId:String`, `requestHash:String`, `seq:Long`, `purpose:MessagePurpose`, `contentType:MessageContentType`, `body:String?`, `assetId:Long?`, `status:MessageStatus`, `correlationId:String`, `redactedAt:Instant?`, `redactedById:Long?`, `redactionReason:String?`.
- **Quan hệ:** N–1 Conversation/User/MediaAsset; sender phải có quyền hiện tại trước gửi; có thể kiểm tra pair participant bằng composite FK nhưng FK không thay kiểm tra leftAt/scope.
- **Enum:** purpose NORMAL/COMPLAINT/CHECKIN_REQUEST; content TEXT/IMAGE/LOCATION/TEMPLATE (hai loại sau chỉ bật khi có contract); status SENT/REDACTED (đề xuất).
- **Constraint:** UQ (conversationId, seq), UQ **(conversationId, senderId, clientMessageId)**; CK seq > 0 và có body hoặc asset. TX cấp seq dưới lock conversation, insert message + notification outbox; cùng key khác requestHash trả conflict; cùng ID khác sender hợp lệ. Không sửa/xóa cứng để xóa chứng cứ; redaction phải giữ audit và chỉ hiển thị đã che. Read state theo ReadReceipt, không dùng readAt chung trên Message. Nguồn UC-28 trang 29.

### 7.4. ReadReceipt — `read_receipts` — C

- **Thuộc tính:** PK `(conversationId:Long, userId:Long)`, `lastReadSeq:Long`, `updatedAt:Instant`, `version:Long`.
- **Quan hệ:** pair tham chiếu ConversationParticipant; suy ra Conversation/User qua participant, không tạo user membership mới khi mark-read.
- **Enum:** không.
- **Constraint:** CK lastReadSeq >= 0; TX tăng đơn điệu và không vượt seq đã tồn tại trong conversation; SV quyền đọc hiện tại, không tin số seq client gửi. Unread phải tính theo quy tắc đã chốt về tin của chính người dùng/redaction, không mặc định seq gap luôn là số tin chưa đọc hiển thị. Nguồn UC-28.

### 7.5. ComplaintTicket — `complaint_tickets` — M

- **Thuộc tính:** `conversationId:Long`, `bookingId:Long?`, `originMessageId:Long`, `createdById:Long`, `issueType:String`, `severity:ComplaintSeverity`, `status:ComplaintStatus`, `responseDueAt:Instant`, `assignedSupportId:Long?`, `acknowledgedAt:Instant?`, `resolution:String?`, `resolvedById:Long?`, `resolvedAt:Instant?`, `customerConfirmedAt:Instant?`, `closedAt:Instant?`.
- **Quan hệ:** N–1 Conversation/Booking/Message/User; booking có thể NULL cho phản ánh trước booking nếu policy cho phép; lưu trú bắt buộc có booking. Lịch sử trạng thái/assignee/severity qua AuditLog.
- **Enum:** severity LOW/NORMAL/HIGH/URGENT là đề xuất, PDF chỉ yêu cầu có mức độ; status OPEN/ACKNOWLEDGED/IN_PROGRESS/RESOLVED/CLOSED theo trang 44. issueType là mã danh mục cấu hình/whitelist được chốt, không tự dựng danh sách enum ngoài yêu cầu.
- **Constraint:** UQ originMessageId chống tạo hai ticket do retry; TX message thuộc cùng conversation và creator có quyền; responseDueAt từ policy, resolution/actor/time bắt buộc khi resolve. Deadline/escalation không tự đóng ticket; không cho HOST tự coi đã giải quyết khi policy cần xác nhận khách. SV scope SUPPORT được phân công. Nguồn UC-28.

### 7.6. CheckinRequest — `checkin_requests` — M

- **Thuộc tính:** `bookingId:Long`, `conversationId:Long`, `originMessageId:Long`, `requestedById:Long`, `proposedAt:Instant`, `counterProposedAt:Instant?`, `agreedCheckinAt:Instant?`, `status:CheckinRequestStatus`, `note:String?`, `respondedById:Long?`, `respondedAt:Instant?`, `responseNote:String?`, `customerAcceptedAt:Instant?`, `closedAt:Instant?`.
- **Quan hệ:** N–1 Booking/Conversation/Message/User requester và responder; nhiều request theo thời gian cho một booking nhưng một request đang đàm phán là lựa chọn thiết kế.
- **Enum:** PENDING/COUNTER_PROPOSED/APPROVED/REJECTED/CLOSED/CANCELLED; hai mã kết thúc sau là đề xuất hỗ trợ đóng và hủy. History qua AuditLog, không ghi đè mất các lần đề nghị.
- **Constraint:** UQ originMessageId; partial UQ bookingId WHERE status IN ('PENDING','COUNTER_PROPOSED','APPROVED') (đề xuất một lịch hẹn còn hiệu lực). TX booking CONFIRMED lúc yêu cầu, đúng customer/property/thread; optimistic version khi phản hồi; APPROVED cần agreedCheckinAt, counter offer chỉ thành agreed khi khách chấp nhận. Đề nghị mới thay thế lịch cũ phải đóng lịch cũ trong cùng TX; không đổi booking thành CHECKED_IN. Cancel/expire/check-in đóng request và hủy nhắc chưa gửi. Nguồn UC-29.

## 8. Cấu hình chính sách

### 8.1. ConfigDefinition — `config_definitions` — C

- **Thuộc tính:** PK `configKey:String`, `valueType:ConfigValueType`, `allowedScopes:Json`, `description:String`, `minValue:BigDecimal?`, `maxValue:BigDecimal?`, `allowedValues:Json?`, `publiclyReadable:Boolean`, `definitionVersion:Integer`.
- **Quan hệ:** 1–N ConfigValue. Thay đổi định nghĩa/validation cần audit và rollout tương thích code.
- **Enum:** STRING/INTEGER/DECIMAL/BOOLEAN/DURATION/JSON; scope GLOBAL/TENANT/PROPERTY.
- **Constraint:** CK definitionVersion > 0, min <= max khi đủ hai giá trị; SV key whitelist, allowedScopes là tập enum hợp lệ. Không lưu secret SMTP/JWT dưới cấu hình công khai. Password length floor là ràng buộc backend, không cho admin tắt tùy ý. Nguồn UC-30.

### 8.2. ConfigValue — `config_values` — M

- **Thuộc tính:** `configKey:String`, `scope:ConfigScope`, `scopeId:Long?`, `valueText:String`, `valueType:ConfigValueType`, `policyVersion:Integer`, `effectiveAt:Instant`, `updatedById:Long`, `changeReason:String`.
- **Quan hệ:** N–1 ConfigDefinition/User; PROPERTY scopeId tham chiếu Property qua validation/trigger nếu dùng scope đa hình. GLOBAL scopeId NULL, TENANT chưa được phép ghi khi chưa có tenant model.
- **Enum:** scope/valueType như trên; đây là kiểu giá trị, không phải enum cho minLength hoặc giờ nhắc.
- **Constraint:** UQ (configKey, scope, scopeId, policyVersion) với **NULLS NOT DISTINCT** hoặc index tương đương để GLOBAL không trùng vì NULL; CK version > 0, GLOBAL iff scopeId NULL. SV type khớp definition, parse/range/cross-key minLength <= maxLength; TX tạo revision mới và history, không sửa revision đang dùng. Chọn bản có effectiveAt <= now mới nhất, tie-break policyVersion; ưu tiên PROPERTY > TENANT > GLOBAL khi scope tồn tại và có quyền. Dùng cùng một thời điểm/version khi đọc bộ policy liên quan. Nguồn UC-30, trang 44.

### 8.3. ConfigChangeLog — `config_change_logs` — E append-only

- **Thuộc tính:** `configValueId:Long`, `previousConfigValueId:Long?`, `oldValue:String?`, `newValue:String`, `actorId:Long`, `reason:String`, `changedAt:Instant`, `correlationId:String`.
- **Quan hệ:** N–1 ConfigValue revision mới/cũ, N–1 User actor.
- **Enum:** không.
- **Constraint:** FK giữ revision lịch sử; TX ghi log cùng config value, không để secret trong old/new. Invalidate cache version sau commit; chính sách không thay mật khẩu cũ hoặc reminder đã gửi hồi tố. Nguồn UC-30.

## 9. Email và reminder

### 9.1. EmailTemplate — `email_templates` — M

- **Thuộc tính:** `templateCode:MailEventType`, `locale:String`, `status:TemplateStatus`, `activeVersion:Integer?`.
- **Quan hệ:** 1–N EmailTemplateVersion; activeVersion tham chiếu cặp `(templateId, versionNo)` của chính template, không được trỏ version template khác.
- **Enum:** status DRAFT/ACTIVE/ARCHIVED; event types xem mục 12.
- **Constraint:** UQ (templateCode, locale); ACTIVE cần một version đã publish hợp lệ; draft/archive vẫn giữ các version cũ để mail retry. FK vòng activeVersion thiết lập sau khi tạo parent/version; không hard-delete template có mail lịch sử. SV fallback locale có cấu hình, preview dữ liệu giả/masked, publish có quyền/audit. Nguồn UC-31.

### 9.2. EmailTemplateVersion — `email_template_versions` — E

- **Thuộc tính:** `templateId:Long`, `versionNo:Integer`, `subject:String`, `htmlBody:String`, `textBody:String`, `allowedVariables:Json`, `requiredVariables:Json`, `publishedAt:Instant?`, `publishedById:Long?`.
- **Quan hệ:** N–1 EmailTemplate/User publisher; 1–N MailEvent. Chọn draft mới có thể sửa trước publish; **sau publish nội dung bất biến**.
- **Enum:** không cần status trùng template; publishedAt phân biệt draft/published, active pointer xác định version đang dùng.
- **Constraint:** UQ (templateId, versionNo); CK versionNo > 0; SV tập biến thuộc whitelist theo event, required là tập con allowed, subject không rỗng/fallback text hợp lệ, HTML sanitize/context escape; không thực thi script/query/expression tùy ý. Mail snapshot giữ đúng version khi template mới publish. Nguồn UC-31.

### 9.3. MailEvent — `mail_events` — E có metadata queue

- **Thuộc tính:** `eventId:UUID`, `eventType:MailEventType`, `businessKey:String`, `recipientId:Long?`, `recipientIdentity:String`, `recipientAddress:String`, `recipientMasked:String`, `templateVersionId:Long`, `payloadSnapshot:Json`, `status:MailStatus`, `retryCount:Integer`, `dueAt:Instant`, `sentAt:Instant?`, `expiresAt:Instant?`, `leaseOwner:String?`, `leaseUntil:Instant?`, `lastErrorCode:String?`, `deadLetterReason:String?`, `correlationId:String`.
- **Quan hệ:** N–1 EmailTemplateVersion và User recipient nếu có; businessKey ví dụ booking/refund/token issuance/reminder window ID, do backend tạo. eventId là nguồn business event, không tự đổi UUID để lách dedupe.
- **Enum:** QUEUED/SENDING/SENT/RETRY/UNKNOWN/FAILED_VALIDATION/FAILED/DLQ/CANCELLED.
- **Constraint:** UQ (eventId, eventType, recipientIdentity); UQ (eventType, businessKey, recipientIdentity, templateVersionId). recipientIdentity là user ID/định danh hoặc HMAC email chuẩn hóa ổn định, **không dùng recipientMasked**. Địa chỉ thực cần bảo vệ khi lưu, không trả về đại trà; snapshot không có token plaintext/password. CK retryCount >= 0. TX consumer tạo mail + mark outbox an toàn; version chốt một lần lúc enqueue, không render lại bằng active version mới khi retry.
- **Hành vi:** UNKNOWN khi SMTP có thể đã nhận nhưng chưa biết kết quả; reconcile theo providerMessageId nếu hỗ trợ, retry bounded chấp nhận at-least-once delivery. Resend cùng logical mail tạo attempt mới; verification link hết hạn là nghiệp vụ phát token/link mới, không gửi link cũ. DLQ thể hiện ngay status/reason và attempts, không bắt buộc bảng mail_dead_letters riêng. Nguồn UC-32/34.

### 9.4. MailDeliveryAttempt — `mail_delivery_attempts` — E có hoàn tất kết quả

- **Thuộc tính:** `mailEventId:Long`, `attemptNo:Integer`, `operatorActionKey:String?`, `triggeredById:Long?`, `providerMessageId:String?`, `result:MailAttemptResult`, `errorCode:String?`, `attemptedAt:Instant`, `completedAt:Instant?`.
- **Quan hệ:** N–1 MailEvent/User operator.
- **Enum:** STARTED/SUCCEEDED/TEMPORARY_FAILURE/PERMANENT_FAILURE/UNKNOWN.
- **Constraint:** UQ (mailEventId, attemptNo); partial UQ (mailEventId, operatorActionKey) khi có; CK attemptNo > 0. TX claim + tăng attempt, hoàn tất attempt/status có kiểm tra lease để worker cũ không ghi đè worker mới; timeout sau gửi không đánh chắc chắn thất bại. Không tạo business event mới khi SUPPORT resend cùng mail. Nguồn UC-34.

### 9.5. ScheduledReminder — `scheduled_reminders` — M

- **Thuộc tính:** `bookingId:Long`, `checkinRequestId:Long?`, `reminderType:ReminderType`, `windowCode:String`, `policyVersion:Integer`, `policySnapshot:Json`, `scheduledCheckinAt:Instant`, `dueAt:Instant`, `cutoffAt:Instant`, `status:ReminderStatus`, `mailEventId:Long?`, `leaseOwner:String?`, `leaseUntil:Instant?`, `sentAt:Instant?`, `cancelReason:String?`.
- **Quan hệ:** N–1 Booking/CheckinRequest; 0–1 logical MailEvent. Policy snapshot giữ đủ cấu hình hiệu lực, không gán policyVersion thành FK tùy tiện khi config gồm nhiều key.
- **Enum:** CHECKIN_REMINDER; SCHEDULED/CLAIMED/QUEUED/SENT/CANCELLED/SKIPPED/FAILED.
- **Constraint:** UQ **(bookingId, reminderType, windowCode, policyVersion)**; UQ mailEventId khi có. CK policyVersion > 0, dueAt <= cutoffAt. TX claim batch DB, chỉ CONFIRMED chưa check-in/chưa cancel và còn cutoff mới enqueue; khóa booking khi kiểm tra/tạo tác vụ, worker kiểm tra lại điều kiện trước gửi theo policy. Hai window 48h/24h cùng policy là hai reminder hợp lệ.
- **Đổi policy/giờ hẹn:** EFFECTIVE_FOR_UNSENT, thay/cancel lịch chưa gửi trong TX; partial UQ (bookingId, reminderType, windowCode) WHERE sentAt IS NOT NULL để mốc đã gửi không tự gửi lại chỉ vì policyVersion đổi. Dedupe logical mail dùng window ổn định, giữ dấu đã gửi; windowCode không thay tùy tiện để né chống trùng. dueAt dựa agreedCheckinAt nếu có, nếu không dựa ngày/giờ/timezone đã chốt. Không tuyên bố exactly-once SMTP; DB chỉ bảo đảm logical event và claim. Nguồn UC-33, trang 34/44.

## 10. Analytics và export

### 10.1. UserBehaviorLog — `user_behavior_logs` — E append-only

- **Thuộc tính:** `eventId:UUID`, `sessionKey:String`, `eventType:FunnelEventType`, `propertyId:Long?`, `roomTypeId:Long?`, `bookingId:Long?`, `paymentId:Long?`, `actorKey:String?`, `sourceChannel:BookingSourceChannel`, `eventTime:Instant`, `ingestedAt:Instant`, `requestId:String`, `correlationId:String`, `metadata:Json`.
- **Quan hệ:** các FK Property/Type/Booking/Payment khi có ngữ cảnh; search chưa chọn property có thể NULL. sessionKey là pseudonymous ID, không email/IP thô.
- **Enum:** SEARCH_SHOWN/QUOTE_ISSUED/HOLD_CREATED/PAYMENT_INITIATED/PAYMENT_CAPTURED/BOOKING_CONFIRMED/CHECKIN_COMPLETED.
- **Constraint:** UQ eventId; SV không ghi PII/message/token vào metadata; định nghĩa session/attribution window và grain dedupe từng bước. Không UQ(session,eventType) một cách mù quáng vì một session có thể có nhiều booking; query dedupe theo session/business context được chốt. Event-time khác ingestion-time; missing coverage phải công bố, không suy từ booking thành toàn bộ funnel. IX eventTime/propertyId/eventType theo workload. Nguồn UC-25.

### 10.2. ReportExport — `report_exports` — M

- **Thuộc tính:** `requestedById:Long`, `requestKey:String`, `requestHash:String`, `scopeSnapshot:Json`, `querySnapshot:Json`, `status:ReportStatus`, `asOf:Instant?`, `objectKey:String?`, `rowCount:Long?`, `requestedAt:Instant`, `finishedAt:Instant?`, `expiresAt:Instant?`, `failureCode:String?`, `leaseOwner:String?`, `leaseUntil:Instant?`.
- **Quan hệ:** N–1 User requester; object nằm MinIO, không bắt buộc là MediaAsset ảnh.
- **Enum:** QUEUED/RUNNING/COMPLETED/FAILED/CANCELLED/EXPIRED; hai mã cuối là hỗ trợ vận hành được đề xuất.
- **Constraint:** UQ (requestedById, requestKey); cùng key khác requestHash conflict. CK rowCount >= 0 khi có; COMPLETED cần objectKey/finishedAt/asOf. SV whitelist metric/dimension/sort, range/rows/concurrency; snapshot scope không tự cấp quyền lâu dài, kiểm tra quyền hiện tại khi chạy/download. Export có audit, chống CSV formula injection; URL ký tạo khi tải, không lưu URL lâu dài. Nguồn UC-26.

### 10.3. AnalyticsRefreshState — `analytics_refresh_state` — C, read model tùy chọn

- **Thuộc tính:** PK `propertyId:Long`, `dataVersion:Long`, `refreshedAt:Instant`, `sourceCutoffAt:Instant?` (đề xuất đo lag).
- **Quan hệ:** 1–1 Property; JDBC/upsert đủ nếu không dùng JPA.
- **Enum:** không.
- **Constraint:** CK dataVersion >= 0; TX/upsert tăng version không lùi, refreshedAt chỉ cập nhật sau khi dữ liệu tương ứng hoàn tất. Không lấy now() làm asOf của dữ liệu stale. Consumer incremental cần checkpoint/dedupe event bền vững nếu có summary; bảng này một mình không chứng minh exactly-once aggregate. Nguồn UC-21/26 và trang 43; PDF không bắt buộc tên bảng này.

### 10.4. Hợp đồng chỉ số, không phải entity mới bắt buộc

| Chỉ số/chiều | Grain và nguồn | Trục thời gian / trạng thái |
|---|---|---|
| Booking count/value | Một Booking, không COUNT(*) sau join nights | createdAt hoặc trục người dùng chọn; công bố trạng thái được tính |
| Room nights | Một BookingNight | stayDate, trạng thái lưu trú theo định nghĩa KPI |
| Captured | Một capture được xác minh trong Payment | capturedAt, capturedAmount thực tế; không dùng booking total thay tiền thu |
| Refunded | Một Refund thành công | processedAt khi provider hoàn, status SUCCEEDED; pending không trừ net cash |
| Net cash | Tổng captured trừ succeeded refund sau aggregate riêng | Cùng currency/property/time axis/cutoff, không join nhân bản tiền |
| Occupancy | Một phòng/ngày có thể bán và một phòng/ngày đã sử dụng | Room/status + sale blocks + booking history/nights; COMPLETED vẫn có đêm lịch sử, không chỉ đọc allocation chưa release |
| Source channel | Booking.sourceChannel snapshot và funnel event | Mã do server validate, không cho client tự nhận nguồn đặc quyền |
| Guest segment | NEW/RETURNING/UNKNOWN snapshot theo quy tắc mục Booking | Cùng cutoff, không dùng trạng thái hiện tại của user để đổi lịch sử |
| Freshness | Dữ liệu gốc hoặc read model/refresh state | asOf, sourceType LIVE/AGGREGATE/CACHE, lag, fromCache; key gồm scope fingerprint |

Mỗi API báo cáo còn phải công bố tử/mẫu, status inclusion, timezone, currency, filter và định nghĩa kỳ so sánh. Fact/summary tables chỉ tạo khi tải và EXPLAIN cho thấy cần; grain tách booking/night/payment/refund/occupancy. Redis lỗi phải có fallback/giới hạn tài nguyên, không bỏ owner scope. Nguồn UC-18/21→26, trang 43/49.

## 11. Fake Bank: đối tượng bắt buộc thuộc service/schema riêng

Các đối tượng dưới đây phục vụ UC-35/36 nhưng **không thuộc danh sách entity lõi module Booking**. Không dùng Wallet làm bản thay thế. Bank cũng dùng các quy ước kiểu/enum/transaction trên, với lifecycle và ownership riêng.

| Entity / bảng | Thuộc tính và quan hệ | Enum / constraint |
|---|---|---|
| BankAccount / `bank_accounts` | M; accountCode:String, ownerReference:String, balance:BigDecimal, currency:String, status:BankAccountStatus | ACTIVE/BLOCKED; UQ accountCode; CK balance >= 0/currency VND; lock/conditional debit trong TX bank |
| BankTransaction / `bank_transactions` | M; merchantId:String, merchantReference:String, idempotencyKey:String, requestHash:String, providerTxnId:String, debitAccountId:Long, creditAccountId:Long, amount:BigDecimal, currency:String, operation:BankOperation, status:BankTransactionStatus, originalTxnId:Long?, completedAt:Instant?; N–1 account debit/credit, self refund→capture | DEBIT/REFUND; INITIATED/PROCESSING/SUCCEEDED/FAILED; UQ (merchantId,idempotencyKey), providerTxnId; CK amount > 0/debitAccount <> creditAccount; TX refund không vượt capture kể cả reservation đang xử lý; cùng key khác payload conflict |
| BankLedgerEntry / `bank_ledger_entries` | E; txnId:Long, accountId:Long, entryNo:Integer, debit:BigDecimal, credit:BigDecimal; N–1 BankTransaction/Account | Không cần enum hướng nếu đã có debit/credit; UQ (txnId,entryNo); CK đúng một phía dương, phía kia 0; TX tổng debit = tổng credit theo transaction/currency và cập nhật balance cùng commit; không CHECK tổng nhiều hàng |
| BankWebhookOutbox / `bank_webhook_outbox` | E + queue metadata; eventId:UUID, txnId:Long, eventType:BankEventType, payload:Json, attempts:Integer, nextAttemptAt:Instant, status:OutboxStatus, deliveredAt:Instant?; N–1 BankTransaction | UQ eventId; commit cùng ledger/transaction; gửi signed HTTP ngoài TX, retry bounded/duplicate/delay để kiểm thử; lưu lịch sử delivery attempts nếu cần audit từng lần |

Provider paymentIntentId phải tương ứng Payments.id bên Booking; merchantReference không thay idempotencyKey. Không có distributed ACID giữa hai DB. Bank timeout/success/ledger không bị rollback bởi lỗi Booking DB. Nguồn trang 36→38, 41.

## 12. Enum đích và state machine

Các tập dưới đây là **mã thiết kế đích**, chưa được áp dụng vào Java hoặc dữ liệu. Tên có thể giữ hậu tố Type của code hiện có nếu thống nhất mapping; không tạo hai enum khác nhau cho cùng một ý nghĩa.

| Enum | Giá trị đích | Nguồn/ghi chú |
|---|---|---|
| UserStatus | PENDING_VERIFICATION, ACTIVE, IN_ACTIVE, LOCKED | Pending theo UC-01; giữ inactive/locked đang có |
| PropertyStatus | DRAFT, PUBLISHED, PAUSED, ARCHIVED | PDF trang 38 |
| RoomOperationalStatus | ACTIVE, INACTIVE, MAINTENANCE | Hợp nhất status phòng vận hành; mã OUT_OF_SERVICE cũ cần mapping rõ, không xóa dữ liệu mù |
| BookingStatusType | PENDING_PAYMENT, CONFIRMED, CHECKED_IN, COMPLETED, CANCELLATION_PENDING, REFUND_PENDING, CANCELLED, EXPIRED, NEEDS_ATTENTION, NO_SHOW | HOLD/HELD cũ map có kiểm chứng sang PENDING_PAYMENT; REFUNDED thuộc số liệu refund, không tự đổi lịch sử booking |
| AllocationStatus | HELD, CONFIRMED, RELEASED | Mã kỹ thuật đề xuất; không dùng expiry thời gian làm enum động |
| PaymentStatusType | INITIATED, PROCESSING, SUCCEEDED, FAILED, TIMED_OUT, UNKNOWN, RECONCILING, NEEDS_ATTENTION | SUCCESS/PENDING cũ cần migration theo nghĩa; không coi TIMED_OUT là chắc chắn chưa thu |
| RefundStatusType | REQUESTED, PROCESSING, SUCCEEDED, FAILED, UNKNOWN, RECONCILING, NEEDS_ATTENTION | Mở rộng xử lý kết quả chưa rõ; APPROVED/REJECTED của quy trình duyệt riêng không ép vào giao dịch thực thi |
| PaymentProvider / PaymentMethodType | FAKE_BANK / FAKE_BANK_DEBIT | Chọn mã đích, chưa đổi enum code hiện có |
| MediaAssetStatus | PENDING, READY, DELETING, DELETED, PENDING_EXPIRED, FAILED | Bổ sung DELETED theo PDF |
| ConversationType / ConversationStatus | INQUIRY, BOOKING_SUPPORT / OPEN, CLOSED | UC-27 |
| ParticipantRole / MessageStatus | CUSTOMER, HOST, SUPPORT / SENT, REDACTED | Role tham gia không tự cấp quyền vượt owner/assignment; REDACTED là mã bổ sung |
| MessagePurpose / MessageContentType | NORMAL, COMPLAINT, CHECKIN_REQUEST / TEXT, IMAGE, LOCATION, TEMPLATE | Purpose theo PDF; content tách riêng, LOCATION/TEMPLATE chỉ bật khi có contract |
| ComplaintStatus / ComplaintSeverity | OPEN, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CLOSED / LOW, NORMAL, HIGH, URGENT | State theo trang 44; mức độ cụ thể là đề xuất |
| CheckinRequestStatus | PENDING, COUNTER_PROPOSED, APPROVED, REJECTED, CLOSED, CANCELLED | CLOSED/CANCELLED là mã kết thúc bổ sung |
| ReviewStatusType | PENDING_MODERATION, PUBLISHED, HIDDEN | UC-16, HIDDEN phục vụ ẩn/khôi phục |
| VerificationPurpose | EMAIL_VERIFICATION, PASSWORD_RESET | Không ép bật reset nếu chưa triển khai |
| ConfigScope / ConfigValueType | GLOBAL, TENANT, PROPERTY / STRING, INTEGER, DECIMAL, BOOLEAN, DURATION, JSON | Scope theo UC-30; kiểu cụ thể là whitelist thiết kế |
| TemplateStatus | DRAFT, ACTIVE, ARCHIVED | UC-31 |
| MailEventType | ACCOUNT_CREATED, EMAIL_VERIFICATION, BOOKING_CONFIRMED, PAYMENT_SUCCEEDED, PAYMENT_FAILED, REFUND_SUCCEEDED, REFUND_FAILED, CHECKIN_REMINDER, COMPLAINT_REPLY | UC-31/32; thêm CHECKIN_CONFIRMED chỉ khi bật email check-in theo UC-29 |
| MailStatus / MailAttemptResult | QUEUED, SENDING, SENT, RETRY, UNKNOWN, FAILED_VALIDATION, FAILED, DLQ, CANCELLED / STARTED, SUCCEEDED, TEMPORARY_FAILURE, PERMANENT_FAILURE, UNKNOWN | Tách logical mail khỏi một lần gửi |
| ReminderType / ReminderStatus | CHECKIN_REMINDER / SCHEDULED, CLAIMED, QUEUED, SENT, CANCELLED, SKIPPED, FAILED | windowCode là dữ liệu cấu hình ổn định, không hard-code 48h thành enum bắt buộc |
| ReportStatus | QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED, EXPIRED | Bốn mã đầu theo UC-26; hai mã sau bổ sung |
| BookingSourceChannel / GuestSegment | WEB, MOBILE, ADMIN_ASSISTED, UNKNOWN / NEW, RETURNING, UNKNOWN | Phân loại thiết kế, không thêm trạng thái tùy ý từ client |
| FunnelEventType | SEARCH_SHOWN, QUOTE_ISSUED, HOLD_CREATED, PAYMENT_INITIATED, PAYMENT_CAPTURED, BOOKING_CONFIRMED, CHECKIN_COMPLETED | Các bước UC-25 |
| ActorType / AuditOutcome | USER, SYSTEM, PROVIDER / ATTEMPT, SUCCESS, FAILURE | Phân biệt actor và outcome, tránh audit success khi rollback |
| BookingStatusChangeSource | CUSTOMER, HOST, SUPPORT, ADMIN, SCHEDULER, WEBHOOK, RECONCILIATION, SYSTEM | Nguồn transition, không thay actorId |
| OutboxStatus / InboxStatus | PENDING, PROCESSING, PROCESSED, RETRY, DLQ / RECEIVED, PROCESSING, PROCESSED, RETRY, REJECTED, NEEDS_ATTENTION | Kỹ thuật queue/inbox |
| IdempotencyStatus | IN_PROGRESS, COMPLETED | Lỗi transient có thể rollback record, không mặc định cache lỗi vĩnh viễn |
| ReconciliationType / ReconciliationStatus | PAYMENT_UNKNOWN, CAPTURE_WITHOUT_CONFIRMATION, LATE_CAPTURE, REFUND_UNKNOWN, AMOUNT_MISMATCH, UNMATCHED_EVENT / OPEN, PROCESSING, RESOLVED, NEEDS_ATTENTION | Mã kỹ thuật đối soát |
| BankEventType | PAYMENT_SUCCEEDED, PAYMENT_FAILED, REFUND_SUCCEEDED, REFUND_FAILED | Mở rộng processing event chỉ nếu contract provider có |
| BankAccountStatus / BankOperation / BankTransactionStatus | ACTIVE, BLOCKED / DEBIT, REFUND / INITIATED, PROCESSING, SUCCEEDED, FAILED | Riêng module Fake Bank |

State machine tối thiểu:

- Booking: PENDING_PAYMENT → CONFIRMED → CHECKED_IN → COMPLETED; PENDING_PAYMENT → EXPIRED/CANCELLED; CONFIRMED → CANCELLATION_PENDING/REFUND_PENDING → CANCELLED. NEEDS_ATTENTION/NO_SHOW phải có transition, policy và history cụ thể; không tự hồi sinh booking EXPIRED khi callback tới muộn.
- Payment: INITIATED → PROCESSING → SUCCEEDED hoặc FAILED; thiếu kết quả → UNKNOWN/RECONCILING. SUCCEEDED không quay FAILED do webhook cũ. isOpen và bằng chứng provider xác định có được tạo intent mới, không chỉ so expiry của client.
- Refund: REQUESTED → PROCESSING → SUCCEEDED hoặc FAILED được xác minh; UNKNOWN/RECONCILING/NEEDS_ATTENTION giữ reservation tới khi có kết luận. Retry không tạo Refund mới.
- Complaint: OPEN → ACKNOWLEDGED → IN_PROGRESS → RESOLVED/CLOSED; yêu cầu bổ sung thông tin không tự thành CLOSED, điều kiện xác nhận theo policy được lưu/audit.
- Check-in request: PENDING → APPROVED/REJECTED/COUNTER_PROPOSED; counter offer được khách chấp nhận mới APPROVED; thay thế/booking hủy/check-in thực tế thì đóng/hủy, lưu history và cập nhật reminder.
- Mail/reminder: claim chỉ là bắt đầu xử lý; không đánh SENT chỉ vì đã tạo outbox. Template version/payload chốt khi enqueue, UNKNOWN xử lý theo khả năng provider và giới hạn retry.

## 13. Bảng legacy/extension và những đối tượng không cần tạo

| Nhóm | Cách xử lý theo ADR |
|---|---|
| room_units | Giữ dữ liệu/mapping tương thích; không là phòng vật lý thứ hai; core không tạo mới vào đây |
| room_calendar | Projection/read-only từ rate/sale block/allocation, không nguồn giá/tồn độc lập |
| system_settings | Legacy/read-only hoặc migrate sang config_values, không dual-write |
| otp_verifications / refund_requests / support_tickets | Chuyển token về VerificationToken, refund về Refund; helpdesk riêng là extension, không xử lý cùng complaint ở hai nguồn |
| host_profiles / review_media | Hỗ trợ mở rộng, không bắt buộc core phụ thuộc; review ảnh được PDF cho phép nếu hỗ trợ |
| ai_recommendations / dynamic_pricing_suggestions / currency_exchange_rates / host_vip_packages / loyalty_ranks / point_history / collections / collection_items / voucher_usages | Giữ đúng quyết định ADR, không xóa; tắt phụ thuộc vào core khi extension chưa hoàn chỉnh |
| co_hosts / kyc_documents / dispute_evidence / host_vip_subscriptions / vouchers / wallets / wallet_transactions / withdrawal_requests | Extension giữ theo ADR; schema/mapping riêng khi triển khai, không thay Fake Bank ledger |
| payment_intents / payment_events | Chọn logical intent trong Payment và inbox BankWebhookEvent; không tự tạo thêm hai nguồn giao dịch |
| complaint/check-in history | Chọn AuditLog append-only có đầy đủ trạng thái/actor/time/reason; không cần thêm bảng chỉ để đủ tên entity |
| mail_dead_letters | Chọn MailEvent.status=DLQ + reason + attempts; hỗ trợ màn hình retry/audit, không bắt buộc bảng riêng |
| quote / policy snapshot / money / date range / report facts | DTO/value object/read model theo mục đích; fact tables chỉ thêm theo workload, không tạo entity vô nghĩa cho từng enum |

## 14. Checklist triển khai sau tài liệu

1. Rà schema/data/FK/index thực tế và các cột trùng; catalog không phải script ALTER an toàn sẵn dùng.
2. Map từng trường Java → cột chuẩn; chọn cơ chế migration/version SQL theo ngoại lệ Flyway đã ghi ở ADR; bảo toàn dữ liệu và enum cũ có bảng chuyển mã.
3. Xây constraint nội bộ ở DB, kiểm tra aggregate trong TX, quyền/validation ở service; đề xuất lock order Customer (nếu cần tuần tự snapshot segment) → Booking → Room theo thứ tự ID → Payment → Allocation/Refund khi cùng thao tác. Các use case khóa subset không được khóa ngược rồi quay lại aggregate trước; mọi thay đổi lock order phải test deadlock/concurrency. PDF nêu các ví dụ thứ tự khác nhau; yêu cầu bắt buộc là một thứ tự thực thi nhất quán, không phải đúng tên thứ tự minh họa này.
4. Chứng minh refund reservation, một intent mở, idempotency theo actor, message sender key, reminder window, queue lease recovery; không gọi Bank/SMTP/MinIO khi giữ transaction tồn kho.
5. Test dữ liệu lịch sử/actor cho complaint/check-in, immutable template/snapshot, báo cáo không nhân dòng, cache đúng owner scope; kiểm tra từ DB rỗng rồi mới bật ddl-auto validate.

Không có bước nào trong checklist này đã được thực thi bằng việc cập nhật hai file Markdown.
