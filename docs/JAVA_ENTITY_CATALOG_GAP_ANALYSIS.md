# Đối chiếu từng entity Java với catalog đích

Ngày kiểm tra: 16/10/2026. Đích đối chiếu: [DB_SPEC_CORE_ENTITY_CATALOG.md](DB_SPEC_CORE_ENTITY_CATALOG.md) và [ADR đã rà soát](DB_SPEC_ARCHITECTURE_DECISIONS.md).

## 1. Phạm vi và kết quả

Đã đọc toàn bộ 52 file trong package `entity`: **49 class `@Entity`, 1 `@MappedSuperclass`, 2 `@Embeddable`**; đối chiếu enum hiện có và một số mapper/repository/service/test trực tiếp liên quan để nhận diện ảnh hưởng khi refactor. Đây là kiểm tra tĩnh mã nguồn, không kết nối lại DB, không chạy Spring context/DDL và không chứng minh chức năng bằng runtime test.

| Phân loại 49 entity hiện có | Số lượng | Hướng xử lý |
|---|---:|---|
| Map đúng bảng có trong catalog lõi/hỗ trợ lõi | 24 | Giữ hoặc sửa theo từng mục 3 |
| Legacy, chuyển domain hoặc helpdesk tách khỏi core | 6 | Mục 4; không tự xóa dữ liệu/class |
| Extension được ADR giữ | 19 | Mục 5; không ép thêm trường lõi ngoài yêu cầu |

Catalog mô tả 49 đối tượng lưu trữ (48 lõi/hỗ trợ và 1 read model tùy chọn), nhưng **không phải cùng 49 class hiện có**. Có 25 tên đối tượng catalog chưa có class tương ứng; `UserRole` đã được map gián tiếp bằng JoinTable, `RoomTypeAmenity` có thể dùng JoinTable, `AnalyticsRefreshState` có thể dùng JDBC và là tùy chọn. Còn lại **22 đối tượng cần mapping mới hoặc refactor/thay thế từ mô hình cũ**, không nhất thiết phải là 22 file `@Entity` mới nếu chọn adapter JDBC có ownership rõ ràng.

Quy ước báo cáo:

- **Giữ** = thành phần phù hợp, không phải chứng nhận toàn bộ class đã khớp.
- **Bổ sung/đổi mapping** = đề xuất thay đổi lần triển khai sau; tên field Java có thể giữ nếu `@Column/@JoinColumn` map đúng cột chuẩn và không có hai nguồn ghi.
- **DB** = constraint/index cần migration riêng. Không thấy annotation trong Java **không đủ kết luận DB thực tế chưa có constraint**.
- **TX/SV** = kiểm tra transaction/service; không thể giải quyết chỉ bằng thêm annotation/entity.
- Các trường/enum được catalog ghi là đề xuất vẫn là lựa chọn thiết kế, không biến thành yêu cầu nguyên văn PDF. Độ chính xác tiền 19,2 và metadata audit bổ sung cũng thuộc thiết kế đích.

## 2. Thay đổi chung và các class hỗ trợ

### 2.1. BaseEntity.java

Nguồn: [BaseEntity.java](../src/main/java/com/vn/datphong/entity/BaseEntity.java). Hiện chỉ có createdAt/updatedAt kiểu `LocalDateTime`, không có id hoặc version.

- Chuyển thời điểm nghiệp vụ/audit cần UTC sang `Instant` và cột TIMESTAMPTZ theo catalog. Dữ liệu cũ cần xác định timezone trước chuyển đổi; không đổi type và giả định timestamp cũ là UTC.
- **Không thêm `@Version` hoặc id vào BaseEntity một cách mù quáng**: Booking/Payment/Property/Room/RoomTypeEntity và RoomUnit đã khai báo version/id riêng. Có thể giữ version ở từng entity hoặc tách nền M/E; không để hai version mapping trong cùng entity.
- User và RoomRate đang extends BaseEntity nhưng chưa có version. Role/Permission/Amenity/CancellationPolicy/RoomImage/Review/Conversation còn thiếu một phần hoặc toàn bộ metadata M.
- BookingNight và CancellationPolicyRule thiếu createdAt của nền E; BookingStatusHistory/IdempotencyRecord/OutboxEvent/Notification/Message/UserBehaviorLog đã có createdAt nhưng còn LocalDateTime.
- Không ép toàn bộ extension/legacy kế thừa nền M: catalog không yêu cầu biến lịch sử append-only thành entity mutable.

### 2.2. RolePermissionId.java và RoomAmenityId.java

Nguồn: [RolePermissionId.java](../src/main/java/com/vn/datphong/entity/RolePermissionId.java), [RoomAmenityId.java](../src/main/java/com/vn/datphong/entity/RoomAmenityId.java).

- **Giữ:** Embeddable, Serializable, hai khóa Long, equals/hashCode theo cả hai khóa hiện đã có.
- Chưa cần đổi cấu trúc theo catalog; khi triển khai kiểm tra NOT NULL/PK/FK thật của bảng và không để khóa thay đổi sau persist.
- Nếu chọn thêm UserRole/ConversationParticipant/ReadReceipt entity khóa ghép, tạo ID class tương ứng hoặc cấu hình khóa ghép hợp lệ. RoomTypeAmenity dùng JoinTable thì không cần ID class/entity riêng.

### 2.3. Enum, thời gian, tiền và JSON áp dụng xuyên class

- Target là VARCHAR + EnumType.STRING. Hiện **15 class** có `SqlTypes.NAMED_ENUM`: CoHost, DisputeEvidence, HostVipSubscription, KycDocument, Message, Notification, OtpVerification, Payment, RefundRequest, SupportTicket, UserBehaviorLog, Voucher, Wallet, WalletTransaction, WithdrawalRequest. Core cần chuyển theo catalog; extension chỉ chuyển cùng quyết định mapping/DDL tương thích, không gỡ annotation rời rạc rồi coi DB đã đồng bộ.
- `pricePerNight:Long` không phải nguồn giá chuẩn. Các tiền core đã dùng BigDecimal phần lớn đang precision 12,2; đồng bộ về 19,2 nếu thực hiện lựa chọn catalog. Giữ tỷ lệ phần trăm 5,2; không đổi mọi NUMERIC thành kiểu tiền.
- String TEXT chứa JSON hiện tại ở snapshot/outbox/response cần schema DTO/value object và mapping JSONB theo catalog; đổi columnDefinition mà không parse/validate dữ liệu cũ là chưa đủ.
- Danh sách N–1/1–N trong catalog là quan hệ dữ liệu, **không bắt buộc thêm collection inverse** vào tất cả entity. Không kết luận Role thiếu quyền chỉ vì không có Set<Permission> khi RolePermission đã map quan hệ.
- UQ đơn giản có thể khai báo JPA; partial unique, expression unique email, CHECK, daterange exclusion, NULLS NOT DISTINCT, cross-table consistency và quyền cần DB/TX/SV tương ứng.

## 3. Chi tiết 24 entity có bảng đích trong catalog

### 3.1. User.java — catalog 2.1 / users

Nguồn: [User.java](../src/main/java/com/vn/datphong/entity/User.java).

- **Giữ:** id, fullName/email/phone, status và quan hệ role qua user_roles.
- **Mapping:** `password` hiện mặc định cột password → map cột `password_hash` (có thể đổi tên Java thành passwordHash); `emailVerifiedAt:Instant?` → verified_at. Bổ sung `tokenVersion:Long`, `@Version Long version`; thời gian nền chuyển Instant. Avatar hiện là String, nếu dùng lựa chọn catalog thì bổ sung quan hệ MediaAsset nullable thay URL độc lập.
- **Trạng thái:** default ACTIVE → PENDING_VERIFICATION cho đăng ký mới theo luồng xác minh; thêm mã vào enum. `active:Boolean` đang tồn tại song song status: cần chốt migration/logic tương thích, không xóa trước khi sửa security/test. username/gender/2FA là trường hỗ trợ ngoài catalog, không tự xóa để làm schema giống nhau.
- **DB/TX:** NOT NULL email/passwordHash/fullName; unique lower(trim(email)), tokenVersion >= 0; UserRole quản lý assignedAt nếu cần. EAGER roles không tự là sai yêu cầu, chỉ cân nhắc fetch khi kiểm tra auth/query.
- **Ảnh hưởng:** CustomUserDetailsService, UserMapper/UserImportMapper, JWT/account status và UserAccountStatusTest. Tên field password **không chứng minh DB đang lưu plaintext**; phải kiểm tra các đường ghi/hash trước migration, không hash lại chuỗi đã hash.

### 3.2. Role.java — catalog 2.2 / roles

Nguồn: [Role.java](../src/main/java/com/vn/datphong/entity/Role.java).

- **Giữ:** id/code/name/description; UQ code đã khai báo.
- **Bổ sung:** metadata M (createdAt/updatedAt Instant và version). Không bắt buộc thêm enum RoleCode hoặc collection inverse vì catalog cho phép chúng tùy chọn.
- **TX/SV:** cấp/thu role có audit/token revocation; không xóa cứng khi đang sử dụng. GUEST không bắt buộc một hàng role, USER cũ không tự map CUSTOMER nếu chưa rà dữ liệu/quyền.

### 3.3. Permission.java — catalog 2.3 / permissions

Nguồn: [Permission.java](../src/main/java/com/vn/datphong/entity/Permission.java).

- **Giữ:** id/code/name/description, UQ code và nullable description phù hợp.
- **Bổ sung:** metadata M; enum mã quyền chỉ là tùy chọn hỗ trợ code.
- **TX/SV:** kiểm soát ủy quyền và audit; RolePermission đã thể hiện quan hệ, không cần tạo hai nguồn grant permission khác nhau.

### 3.4. RolePermission.java — catalog 2.5 / role_permissions

Nguồn: [RolePermission.java](../src/main/java/com/vn/datphong/entity/RolePermission.java).

- **Giữ:** EmbeddedId, MapsId roleId/permissionId, N–1 LAZY và NOT NULL cả hai phía: cấu trúc hiện tại phù hợp catalog C.
- **Không có thay đổi field bắt buộc** theo catalog hiện tại. Không thêm id surrogate hoặc version nền M chỉ để đồng bộ hình thức.
- **DB/TX:** kiểm tra PK/FK thật; thay quyền có audit/invalidate authorization trong service. Entity này không tự thực hiện các hành vi đó.

### 3.5. Property.java — catalog 3.1 / properties

Nguồn: [Property.java](../src/main/java/com/vn/datphong/entity/Property.java).

- **Giữ:** owner, name, city/country, tọa độ, timezone, PropertyStatus, publicContact, cancellationPolicy, version.
- **Mapping:** addressLine hiện mặc định address_line → cột chuẩn address; checkInFrom/check_in_from → checkinLocal/checkin_local; checkOutUntil/check_out_until → checkoutLocal/checkout_local. Có thể giữ tên Java nhưng map rõ. Nền M chuyển Instant.
- **Nullability:** giờ check-in/out hiện nullable nhưng catalog bắt buộc; backfill trước NOT NULL. slug và country hiện bắt buộc trong code còn catalog cho phép nullable: không phải lỗi nghiệp vụ tự thân, chỉ nới nếu muốn theo đúng lựa chọn catalog và đã xét API hiện có.
- **DB/TX/SV:** tọa độ, timezone hợp lệ, owner scope; publish đủ dữ liệu và policy; không xóa/hủy lịch sử khi PAUSED/ARCHIVED. Không cần thêm collection ảnh nếu query MediaAsset đủ.

### 3.6. RoomTypeEntity.java — catalog 3.2 / room_types

Nguồn: [RoomTypeEntity.java](../src/main/java/com/vn/datphong/entity/RoomTypeEntity.java).

- **Giữ:** property/code/name/maxGuests/maxAdults/maxChildren/bedType/description/policy/active, UQ(property_id,code), version.
- **Bổ sung:** mapping N–N amenities qua room_type_amenities (JoinTable đủ); bedType hiện nullable → bắt buộc theo catalog sau rà/backfill. Nền thời gian Instant.
- **DB/TX/SV:** capacity dương, adults/children không âm, cùng property với room/policy; giảm sức chứa kiểm tra booking/hold tương lai. Không thay entity bằng RoomType enum.

### 3.7. Room.java — catalog 3.3 / rooms

Nguồn: [Room.java](../src/main/java/com/vn/datphong/entity/Room.java).

- **Giữ:** id, property, roomTypeEntity, version; chọn đây là phòng vật lý chuẩn.
- **Mapping:** code → room_code; đổi UQ code toàn hệ thống thành UQ(property_id,room_code). property và roomTypeEntity hiện nullable → bắt buộc; bổ sung floor/notes từ mô hình vật lý đích. name hiện bắt buộc còn catalog nullable, chỉ nới khi cần và API tương thích.
- **Hợp nhất:** roomUnit không còn tham chiếu inventory core; type enum SINGLE/DOUBLE/... không thay RoomTypeEntity. capacity lấy từ room type hoặc snapshot hợp lệ, không giữ hai sức chứa tự quyết. pricePerNight/basePrice và imageUrl chuyển vai trò legacy/projection; active không là nguồn trạng thái vận hành thứ hai. Giữ dữ liệu cũ tới khi migration và toàn bộ nơi dùng đã chuyển.
- **Enum:** RoomStatus hiện có AVAILABLE/OCCUPIED/OUT_OF_SERVICE ngoài tập đích. Chuẩn hóa ACTIVE/INACTIVE/MAINTENANCE, map dữ liệu có kiểm chứng; không tự đổi OCCUPIED thành booking hay suy trạng thái lịch sử.
- **DB/TX:** UQ theo property, room/type cùng property (composite FK hoặc cơ chế DB phù hợp), lock room cho inventory; search/quote phải dùng rate/blocks/allocations chuẩn. Không tự xóa description hoặc field hỗ trợ chỉ vì catalog không liệt kê.

### 3.8. Amenity.java — catalog 3.4 / amenities

Nguồn: [Amenity.java](../src/main/java/com/vn/datphong/entity/Amenity.java).

- **Giữ:** id/name/icon và quan hệ qua RoomAmenity.
- **Bổ sung:** `code:String`, `active:Boolean`, metadata M. Hiện **name unique nhưng chưa có code**; UQ code là định danh đích. UQ name hiện tại chỉ bỏ/nới sau khi xét dữ liệu và nhu cầu trùng tên/bản địa hóa, không đổi tên amenity để làm khóa kỹ thuật.
- **DB/SV:** code NOT NULL/unique sau backfill; danh mục admin quản lý, retire thay xóa lịch sử, thay đổi invalidate search/detail.

### 3.9. RoomAmenity.java — catalog 3.5 / room_amenities

Nguồn: [RoomAmenity.java](../src/main/java/com/vn/datphong/entity/RoomAmenity.java).

- **Giữ:** EmbeddedId/MapsId và N–1 LAZY room/amenity NOT NULL; cấu trúc khóa ghép hiện phù hợp.
- **Không có thay đổi field bắt buộc**. Chuyển sang JoinTable chỉ là refactor tùy chọn, không cần để đạt catalog.
- **DB/SV:** kiểm tra PK/FK; tiện ích hiệu lực lấy hợp của type và room, không phát sinh loại trừ ngầm. Cần thêm mapping room_type_amenities ở phía RoomTypeEntity.

### 3.10. CancellationPolicy.java — catalog 3.7 / cancellation_policies

Nguồn: [CancellationPolicy.java](../src/main/java/com/vn/datphong/entity/CancellationPolicy.java).

- **Hiện có:** id, host nullable và name; chưa đủ mô hình policy theo property/revision.
- **Bổ sung/đổi:** property bắt buộc, policyRevision, effectiveAt Instant, active và metadata M. host không thay property scope; giữ/chuyển host lịch sử có kế hoạch, không đổi FK host_id thành property_id bằng rename đơn thuần.
- **DB/TX:** UQ(propertyId,name,policyRevision), revision > 0, rule theo từng revision; policy đã dùng không sửa làm thay nghĩa snapshot booking. Không thêm policy_json thành nguồn tính thứ hai nếu chọn bảng rule.

### 3.11. CancellationPolicyRule.java — catalog 3.8 / cancellation_policy_rules

Nguồn: [CancellationPolicyRule.java](../src/main/java/com/vn/datphong/entity/CancellationPolicyRule.java).

- **Giữ:** id/policy/daysBeforeCheckin/refundPercent và quan hệ NOT NULL phù hợp.
- **Bổ sung:** createdAt Instant của nền E; UQ(policy_id,days_before_checkin), CHECK ngày >= 0 và phần trăm 0..100 trong DB.
- **SV/TX:** fallback rule/biên deadline/timezone/rounding được chốt theo catalog; khóa sửa rule sau khi policy publish/được dùng. Có annotation updateable hay không không đủ bảo vệ toàn lifecycle.

### 3.12. RoomRate.java — catalog 3.9 / room_rates

Nguồn: [RoomRate.java](../src/main/java/com/vn/datphong/entity/RoomRate.java).

- **Giữ:** roomType bắt buộc, stayDate, currency, closedToSale, priceVersion. Mô hình hiện đã theo loại phòng, không cần đổi sang giá theo room.
- **Mapping:** nightlyRate/nightly_rate → amount; bổ sung @Version riêng (priceVersion là version giá nghiệp vụ, **không tự là optimistic version**), nền thời gian Instant, tiền theo precision đích.
- **DB:** UQ hiện (room_type_id,stay_date); catalog là (room_type_id,stay_date,currency). Với CHECK currency=VND, khóa hiện tại bảo vệ một giá/ngày tương đương; chỉ đổi index khi đồng bộ DDL, không gọi đây là lỗi bán trùng. Bổ sung CHECK amount >= 0/priceVersion >= 0/currency VND.
- **Ảnh hưởng:** RoomRateRepository lọc roomTypeId nhưng RoomServiceImp đang truyền roomId; cần sửa caller trong đợt refactor. Quote/booking đủ rate từng đêm, không fallback rooms.pricePerNight/calendar.

### 3.13. RoomImage.java — catalog 3.12 / room_images

Nguồn: [RoomImage.java](../src/main/java/com/vn/datphong/entity/RoomImage.java).

- **Giữ:** id/room/sortOrder.
- **Bổ sung:** quan hệ asset bắt buộc tới MediaAsset, cover Boolean, metadata M; sortOrder >= 0, UQ(roomId,assetId), partial UQ cover theo room trong DB.
- **Chuyển đổi:** url hiện bắt buộc → legacy, URL hiển thị sinh từ objectKey sau kiểm tra quyền; mediaType hiện có không thay lifecycle asset. Không đoán object key từ URL để xóa như luồng hiện tại sau khi chuyển mô hình.
- **TX/SV:** asset READY/đúng scope; cập nhật upload/finalize/cleanup và mapper DTO cùng lúc, không chỉ thêm FK rồi tiếp tục write URL.

### 3.14. Booking.java — catalog 4.1 / bookings

Nguồn: [Booking.java](../src/main/java/com/vn/datphong/entity/Booking.java).

- **Giữ:** bookingCode, Room/Property/RoomType, các thành phần tiền, snapshot hủy, ghi chú, timestamps nghiệp vụ và @Version; nhưng cần chuẩn hóa mapping/nullability/type.
- **Cột chuẩn:** user/user_id → customer/customer_id; checkinDate/checkin_date → check_in; checkoutDate/checkout_date → check_out; guestsCount/guests_count → guest_count; totalPrice/total_price → total_amount; cancellationReason/cancellation_reason → cancel_reason. Giữ **một** expiresAt/expires_at, chuyển dần khỏi holdExpiresAt; không ghi cả hai như hai nguồn chuẩn.
- **Bổ sung:** propertyTimezoneSnapshot, checkinLocalSnapshot, sourceChannel, guestSegmentSnapshot, checkedInBy, checkedOutBy, checkinVerificationMethod. Snapshot hủy String TEXT → Json/JSONB có schema version; các Instant gồm expiry/confirmed/cancelled/actual timestamps và nền M.
- **Nullability/default:** property/roomType, expiresAt, các thành phần tiền/priceVersion/snapshot cần bắt buộc theo catalog sau backfill; default HOLD → PENDING_PAYMENT. Hợp nhất roomUnit khỏi flow core, voucher/loyalty giữ extension nhưng không tự thay tiền core khi feature tắt. idempotencyKey/quoteToken cũ không làm nguồn thay bảng IdempotencyRecord hoặc chứng minh đã giữ phòng.
- **DB/TX:** UQ code đã có; thêm/check date/guest/tiền/currency và tổng theo core chưa discount; đủ night snapshots và allocation trong cùng transaction. Metadata customer/type/property phải khớp tại lúc đặt; không reinterpret lịch sử theo cấu hình hiện tại.
- **Ảnh hưởng:** BookingMapper, DTO và query trạng thái/date; chỉ đổi tên field sẽ gây lỗi compile/query hoặc vẫn trả dữ liệu từ trường legacy nếu bỏ sót mapper.

### 3.15. BookingNight.java — catalog 4.2 / booking_nights

Nguồn: [BookingNight.java](../src/main/java/com/vn/datphong/entity/BookingNight.java).

- **Giữ:** id, booking NOT NULL, stayDate, nightlyRate/currency; UQ(booking_id,stay_date) đã có.
- **Bổ sung:** taxAmount/feeAmount bắt buộc, createdAt Instant; priceVersion đang nullable → bắt buộc theo target. Tiền chuẩn precision đích, không đổi LocalDate thành timestamp.
- **DB/TX:** tiền không âm, VND; số đêm/khoảng ngày/tổng đêm khớp booking trong TX. Snapshot bất biến; không bổ sung version M chỉ vì có giá.

### 3.16. BookingStatusHistory.java — catalog 4.3 / booking_status_history

Nguồn: [BookingStatusHistory.java](../src/main/java/com/vn/datphong/entity/BookingStatusHistory.java).

- **Giữ:** booking/fromStatus/toStatus/source/actor/reason, nullable fromStatus cho sự kiện khởi tạo.
- **Bổ sung:** actorType, occurredAt Instant, correlationId, eventId UUID unique; createdAt chuyển Instant. occurredAt là thời điểm nghiệp vụ, createdAt là lúc ghi, không lẫn hai nghĩa khi import/reconcile.
- **Enum/TX:** source hiện FAKE_BANK/SYSTEM chưa tách SCHEDULER/WEBHOOK/RECONCILIATION. Chuẩn hóa nguồn theo catalog; actor nullable cho hệ thống nhưng actorType/source bắt buộc. Append-only cùng commit transition, không để mapper tự ghi success trước commit.

### 3.17. Payment.java — catalog 5.1 / payments

Nguồn: [Payment.java](../src/main/java/com/vn/datphong/entity/Payment.java).

- **Giữ:** booking/provider/method, amount/currency, providerReference, idempotencyKey/requestHash, failure fields, capturedAt, version; chúng chưa đủ target.
- **Quan hệ:** booking OneToOne → ManyToOne; DB phải rà/đổi unique booking_id nếu đang tồn tại, thay annotation không tự xóa unique cũ.
- **Bổ sung:** merchantId, capturedAmount, isOpen, intentExpiresAt, correlationId. idempotencyKey/requestHash hiện nullable → bắt buộc; UQ(provider,merchantId,idempotencyKey), scoped providerReference và partial UQ bookingId WHERE isOpen. Tiền/time theo common target.
- **Enum:** bỏ native enum mapping khi migration VARCHAR sẵn sàng; method cần FAKE_BANK_DEBIT thay tập gateway thật không thuộc core; status chuẩn hóa SUCCESS/SUCCEEDED/PENDING, bổ sung UNKNOWN/RECONCILING. paidAt/gatewayTransactionRef/callbackPayload là trường legacy hoặc hỗ trợ, không tạo hai nguồn thời điểm/reference; inbox có metadata kiểm tra/dedupe riêng.
- **TX:** cùng intent retry tái sử dụng hàng/key/hash; chặn intent mới khi trước chưa rõ, capturedAmount thật và currency/amount khớp booking. CHECK một hàng không thay được lock booking hoặc quy tắc không thu trùng; không gọi Bank trong DB transaction.

### 3.18. IdempotencyRecord.java — catalog 5.5 / idempotency_records

Nguồn: [IdempotencyRecord.java](../src/main/java/com/vn/datphong/entity/IdempotencyRecord.java).

- **Giữ:** scope/key/requestHash, UQ(scope,idempotency_key), resourceType/resourceId, responseBody và createdAt.
- **Bổ sung:** actorKey, IdempotencyStatus, responseCode, completedAt, expiresAt nullable. responseBody TEXT → JSONB/value object khi chọn target; createdAt Instant.
- **TX/SV:** scope phải bao gồm actor + operation/version. Chưa thể kết luận scope hiện đang sai chỉ từ entity; cần kiểm tra use case tạo key khi triển khai. Record kết quả và booking/hold commit nguyên tử; resource reference đa hình không thành FK giả tới mọi bảng.

### 3.19. OutboxEvent.java — catalog 5.6 / outbox_events

Nguồn: [OutboxEvent.java](../src/main/java/com/vn/datphong/entity/OutboxEvent.java).

- **Giữ:** eventType, aggregateType/id, payload, status, retryCount, createdAt/processedAt.
- **Bổ sung:** eventId UUID unique, payloadVersion, availableAt, leaseOwner/leaseUntil, lastError; JSONB/schema payload thay TEXT nếu theo target; timestamps Instant.
- **Enum/DB:** OutboxStatus hiện PENDING/PROCESSING/SENT/FAILED; target PENDING/PROCESSING/PROCESSED/RETRY/DLQ. Không rename SENT→PROCESSED hoặc FAILED→DLQ mà chưa xét ý nghĩa các dữ liệu/consumer cũ. retryCount >= 0/payloadVersion > 0; IX queue theo status/availableAt.
- **TX:** outbox cùng commit nghiệp vụ, worker claim/recover lease và consumer dedupe. Thêm trường không tự bảo đảm at-least-once hay retry an toàn.

### 3.20. Review.java — catalog 6.1 / reviews

Nguồn: [Review.java](../src/main/java/com/vn/datphong/entity/Review.java).

- **Giữ:** booking OneToOne + unique đã đúng, user/property, rating và nội dung.
- **Mapping:** user/user_id → customer/customer_id, comment → content. rating Short → Integer nếu thống nhất kiểu catalog; giữ Short vẫn biểu diễn được thang 1..5 nên đây không phải lỗi nghiệp vụ tự thân, nhưng phải thống nhất DTO/DDL. property và content hiện nullable → bắt buộc theo target sau backfill.
- **Bổ sung:** hostReply, hostRepliedBy/At, moderatedBy/At, moderationReason, updatedAt/version; createdAt và các mốc mới Instant. room legacy có thể suy từ booking, không là nguồn property riêng để customer tùy ý gửi.
- **Enum/TX:** VISIBLE/HIDDEN_BY_ADMIN → tập PENDING_MODERATION/PUBLISHED/HIDDEN có migration; review chỉ sau COMPLETED, đúng customer/property; HOST chỉ reply, moderation giữ audit. CHECK rating theo thang đã chốt.

### 3.21. Notification.java — catalog 6.2 / notifications

Nguồn: [Notification.java](../src/main/java/com/vn/datphong/entity/Notification.java).

- **Giữ:** user nhận, title/content và createdAt nhưng cần map recipient_id/Instant.
- **Bổ sung:** eventId UUID, eventType, resourceType/resourceId và readAt Instant; UQ(eventId,recipientId). content hiện nullable cần đồng bộ NOT NULL theo target sau backfill. notifType hiện nhóm BOOKING/MESSAGE/... không đủ mã sự kiện chi tiết, cần mapping/chốt taxonomy; không coi tất cả BOOKING là cùng event.
- **Chuyển đổi:** isRead có thể là projection của readAt; dữ liệu true cũ không đủ biết thời điểm đọc thật, cần quy tắc backfill trung thực. channel IN_APP/EMAIL/BOTH không thay MailEvent/DeliveryAttempt; email queue tách khỏi entity thông báo nội bộ. Native enum mapping chuyển theo kế hoạch VARCHAR.
- **TX/SV:** recipient từ DB/principal, idempotent mark read, outbox consumer dedupe; không gửi secret/signed URL lâu dài.

### 3.22. Conversation.java — catalog 7.1 / conversations

Nguồn: [Conversation.java](../src/main/java/com/vn/datphong/entity/Conversation.java).

- **Giữ:** host và customer đang tên user, createdAt; user_id → customer_id.
- **Bổ sung:** property bắt buộc, booking nullable, type/status, lastMessageSeq, closedAt, updatedAt/version; timestamps Instant. room legacy không thay bối cảnh property/booking của thread.
- **DB/TX:** partial UQ active conversation theo booking/type, seq >= 0; cấp seq dưới lock, message/outbox cùng commit. hostId lúc tạo không cấp quyền vĩnh viễn sau property đổi owner; bổ sung Participant/ReadReceipt, query quyền hiện tại.

### 3.23. Message.java — catalog 7.3 / messages

Nguồn: [Message.java](../src/main/java/com/vn/datphong/entity/Message.java).

- **Giữ:** conversation, sender, dữ liệu nội dung và createdAt; content → body, createdAt Instant.
- **Bổ sung:** clientMessageId, requestHash, seq, purpose, contentType (tách khỏi msgType cũ), asset nullable, MessageStatus, correlationId, redactedAt/redactedBy/redactionReason.
- **Bỏ vai trò chuẩn của** readAt chung trên message; đọc theo từng participant tại ReadReceipt. Native message_type_enum/msg_type không tự trở thành MessagePurpose; enum TEXT/IMAGE là dạng nội dung, NORMAL/COMPLAINT/CHECKIN_REQUEST là mục đích.
- **DB/TX:** UQ(conversation_id,seq) và UQ(conversation_id,sender_id,client_message_id), seq > 0, body hoặc asset; cùng sender/key khác payload conflict, khác sender cùng client ID hợp lệ. Append-only/redaction audit; cấp sequence/ghi outbox nguyên tử, không dùng Redis Pub/Sub làm nguồn lịch sử.

### 3.24. UserBehaviorLog.java — catalog 10.1 / user_behavior_logs

Nguồn: [UserBehaviorLog.java](../src/main/java/com/vn/datphong/entity/UserBehaviorLog.java).

- **Hiện có:** user/room nullable, actionType, searchQuery JSONB, dwellTimeSeconds, createdAt; chưa phải event log funnel theo target.
- **Bổ sung:** eventId UUID, sessionKey, eventType FunnelEventType, property/type/booking/payment nullable, actorKey, sourceChannel, eventTime, ingestedAt, requestId, correlationId, metadata có schema; createdAt Instant.
- **Enum:** BehaviorActionType hiện **HI/HIE/HIA**, không diễn đạt SEARCH_SHOWN/QUOTE_ISSUED/HOLD_CREATED/... . Không tự đoán ý nghĩa ba mã cũ; chốt/backfill hoặc đánh coverage UNKNOWN trước chuyển sang FunnelEventType và VARCHAR.
- **DB/SV:** UQ eventId; không UQ(session,eventType) máy móc; dedupe theo session/business context và event time, không suy full funnel từ booking. user/room/searchQuery/dwell hiện tại có thể giữ extension, nhưng không lưu PII/raw query không kiểm soát trong metadata lõi.

## 4. Sáu entity chuyển vai trò, legacy hoặc helpdesk

| Entity hiện tại | Bằng chứng trong code | Thay đổi cần thực hiện |
|---|---|---|
| [OtpVerification.java](../src/main/java/com/vn/datphong/entity/OtpVerification.java) | Map otp_verifications, otpCode length 10, email/user, isUsed, native OtpPurposeType | Chuyển flow sang VerificationToken/verification_tokens; tokenHash, user bắt buộc, usedAt/attempts/revokedAt/expiry; REGISTER→EMAIL_VERIFICATION và FORGOT_PASSWORD→PASSWORD_RESET sau xác nhận nghĩa. Không đổi tên cột otp_code thành token_hash rồi coi mã cũ đã được hash; không tạo hai nguồn token. |
| [RefundRequest.java](../src/main/java/com/vn/datphong/entity/RefundRequest.java) | Map refund_requests, payment nullable, refundDestination WALLET/BANK, approvedBy, status pha trộn approval và transfer | Thay flow core bằng Refund/refunds; payment bắt buộc, provider/merchant/refundCode/hash/reservationActive/requester/retry metadata, UQ theo catalog. approvedBy và destination thuộc approval/wallet extension nếu còn dùng, không ép vào giao dịch Fake Bank. Nền M/Instant, reservation trước gọi Bank. Không chỉ sửa @Table. |
| [RoomUnit.java](../src/main/java/com/vn/datphong/entity/RoomUnit.java) | Đủ property/type/code/status/floor/notes và @Version, đang được Booking/Room tham chiếu | Giữ legacy/compatibility; core tạo/sửa rooms, di chuyển floor/notes và FK theo kế hoạch. Không thêm allocation/rate độc lập cho room_units; không xóa class khi mapper/DTO vẫn còn dùng. |
| [RoomCalendar.java](../src/main/java/com/vn/datphong/entity/RoomCalendar.java) | room/calendarDate/status/priceOverride, UQ(room,date) | Chuyển read-only/projection từ RoomRate/RoomSaleBlock/Allocation; ngừng write giá/tồn độc lập qua service. Có thể map immutable read model, nhưng annotation read-only không thay việc chặn đường ghi/native SQL. Giữ dữ liệu legacy tới khi chuyển đổi xong. |
| [SystemSetting.java](../src/main/java/com/vn/datphong/entity/SystemSetting.java) | Key/value đơn giản, updatedBy/updatedAt, chưa scope/version/effective time | Giữ legacy/read-only; migrate từng key sang ConfigDefinition/Value/ChangeLog và chuyển reader/writer. Không cố thêm ba field để duy trì hai nguồn policy cùng được ghi. |
| [SupportTicket.java](../src/main/java/com/vn/datphong/entity/SupportTicket.java) | booking/createdBy/againstUser/ticketType/description/status/resolution, không conversation/origin message/severity/deadline | Giữ helpdesk extension; UC-28 dùng ComplaintTicket mới. Nếu reuse code cần adapter/refactor rõ, không xử lý cùng complaint song song hai bảng. Native enum/VARCHAR và Instant chỉ đồng bộ khi triển khai feature; không gọi INVESTIGATING là đủ toàn vòng đời complaint. |

## 5. Mười chín entity extension được giữ

Catalog chưa đặc tả bộ field đích đầy đủ cho các extension. Các việc dưới đây là **giữ ranh giới core và điều kiện tích hợp theo ADR**, không phải yêu cầu tạo thêm mọi field/constraint extension ngay bây giờ. Không tự xóa class/bảng hoặc ép chúng vào schema lõi.

| Entity hiện tại | Cấu trúc đã đọc | Việc cần làm khi tích hợp/triển khai |
|---|---|---|
| [AI_Recommendations.java](../src/main/java/com/vn/datphong/entity/AI_Recommendations.java) | user/room/score/modelVersion/generatedAt | Giữ gợi ý, không cam kết availability hoặc tự tạo allocation; generatedAt dùng Instant nếu thống nhất audit; core không phụ thuộc có recommendation. |
| [DynamicPricingSuggestion.java](../src/main/java/com/vn/datphong/entity/DynamicPricingSuggestion.java) | room/targetDate/suggestedPrice/applied/modelVersion | Giữ; khi HOST áp dụng phải quy về roomTypeId/date chuẩn để cập nhật RoomRate, xác định phạm vi cả loại phòng và audit; không ghi rooms.pricePerNight hoặc snapshot booking. |
| [CurrencyExchangeRate.java](../src/main/java/com/vn/datphong/entity/CurrencyExchangeRate.java) | UQ base/target currency, rate/updatedAt | Giữ phục vụ hiển thị; không đổi currency core khỏi VND, nếu bật quy đổi thì snapshot rate/source/time theo ADR. Không tuyên bố rate hiện tại là tỷ giá giao dịch cũ. |
| [HostProfile.java](../src/main/java/com/vn/datphong/entity/HostProfile.java) | OneToOne user unique, display/business/bio/hostStatus/active | Giữ hồ sơ; không thay properties.owner_id hoặc cấp scope chỉ vì hostStatus APPROVED. Không bắt buộc thêm version M cho feature này chỉ vì extends BaseEntity, nhưng thay timestamp nền sẽ tác động tới nó. |
| [HostVipPackage.java](../src/main/java/com/vn/datphong/entity/HostVipPackage.java) | name/price/durationDays/benefits JSONB | Giữ; chưa cần đổi field để core chạy; khi bật VIP cần validation giá/thời hạn và snapshot quyền lợi riêng, không cấp quyền owner từ gói. |
| [HostVipSubscription.java](../src/main/java/com/vn/datphong/entity/HostVipSubscription.java) | host/vipPackage/startDate/endDate/status/paymentRef | Giữ; period/overlap/quyền lợi và paymentRef có contract riêng. Không dùng paymentRef chuỗi làm bằng chứng capture core; native enum chuyển cùng schema nếu chọn VARCHAR toàn module. |
| [LoyaltyRank.java](../src/main/java/com/vn/datphong/entity/LoyaltyRank.java) | RankNameType/minPoints/discountPercent/benefits | Giữ; enum rank cố định hiện không tự là lỗi. Nếu bật discount, áp dụng có snapshot/rounding/feature flag, không tự diễn giải lại giá đơn cũ theo hạng hiện tại. |
| [PointHistory.java](../src/main/java/com/vn/datphong/entity/PointHistory.java) | user/booking/pointsChange/reason/createdAt | Giữ; event điểm idempotent/append-only khi bật; điểm không thay Bank ledger hoặc tiền capture. Không thêm dữ liệu tích điểm bắt buộc vào flow core. |
| [RoomCollection.java](../src/main/java/com/vn/datphong/entity/RoomCollection.java) | user/name/isPublic/shareToken unique/createdAt | Giữ; share token/quyền public không làm lộ private media hoặc cấp quyền booking; collection không có ý nghĩa hold. |
| [CollectionItem.java](../src/main/java/com/vn/datphong/entity/CollectionItem.java) | collection/room/addedAt, UQ(collection_id,room_id) | Giữ khóa đã phù hợp ADR; không cascade xóa phòng có lịch sử khi xóa collection. |
| [Voucher.java](../src/main/java/com/vn/datphong/entity/Voucher.java) | code/owner/discount/validity/quota/targetRank, owner EAGER | Giữ extension; schema phải có trước FK VoucherUsage nếu scan entity. Khi bật cần quota transaction/idempotency/snapshot, core khi tắt không đọc voucher để tính giá. Fetch EAGER chỉ là điểm kiểm tra hiệu năng, không lỗi spec tự thân. |
| [VoucherUsage.java](../src/main/java/com/vn/datphong/entity/VoucherUsage.java) | voucher/user/booking/usedAt, chưa khai báo UQ nghiệp vụ | Giữ; bổ sung khóa chống dùng trùng phù hợp rule khi triển khai quota, không tự UQ(voucher,user) nếu cho phép dùng nhiều lần. Không coi có entity là đủ bảo đảm limit. |
| [CoHost.java](../src/main/java/com/vn/datphong/entity/CoHost.java) | host/coHostUser/permissions JSON/status, UQ(host,coHostUser) | Giữ; quyền theo host pair chưa tự chứng minh có scope property. Chỉ bật ủy quyền sau khi xác định property/phạm vi và kiểm tra service; không đổi role owner của core. |
| [KycDocument.java](../src/main/java/com/vn/datphong/entity/KycDocument.java) | user/docType/fileUrl/review status/reviewedBy | Giữ extension; khi bật dùng private MediaAsset/object key và quyền review, không lưu URL private lâu dài; KYC không là prerequisite core nếu chưa được chốt. |
| [DisputeEvidence.java](../src/main/java/com/vn/datphong/entity/DisputeEvidence.java) | FK ticket tới SupportTicket, uploadedBy/url/evidenceType | Giữ helpdesk evidence hiện tại. Nếu muốn gắn complaint/refund lõi phải thêm quan hệ rõ và MediaAsset private, không đổi ticket_id sang complaint_id mà không kiểm tra ID/domain. |
| [ReviewMedia.java](../src/main/java/com/vn/datphong/entity/ReviewMedia.java) | review/url/mediaType | Giữ ảnh review tùy chọn; khi bật liên kết MediaAsset READY/đúng quyền thay chỉ URL. Không bắt core review phải có ảnh. |
| [Wallet.java](../src/main/java/com/vn/datphong/entity/Wallet.java) | ownerId/ownerType/balance/currency, UQ(ownerId,ownerType) | Giữ ví Booking tùy chọn; ownerId đa hình không là FK User mặc định. Không dùng làm Fake BankAccount; khi bật cần kiểm soát cập nhật balance/concurrency/ledger của ví. |
| [WalletTransaction.java](../src/main/java/com/vn/datphong/entity/WalletTransaction.java) | wallet/txType/amount/reference/balanceAfter/createdAt | Giữ; trước bật phải có idempotency ledger/transaction khớp balance, không coi referenceType/id đơn thuần là chống trùng. Không phát sinh giao dịch ví trong core khi chưa bật. |
| [WithdrawalRequest.java](../src/main/java/com/vn/datphong/entity/WithdrawalRequest.java) | host/amount/bank info/status/requestedAt/processedAt | Giữ extension payout, chưa có yêu cầu core; trước bật cần duyệt/quyền/reservation/idempotency/đối soát và bảo vệ bank info riêng. Không dùng như Refund Fake Bank. |

## 6. Hai mươi lăm đối tượng catalog chưa có class tương ứng

Các trường đầy đủ, nullable, kiểu và constraint nằm ở đúng mục catalog tham chiếu; bảng này ghi hành động cần thực hiện, tránh sao chép một danh mục target khác dễ lệch. “Chưa có class” không có nghĩa bảng DB chưa có.

| Đối tượng đích | Catalog | Hành động cần thực hiện |
|---|---|---|
| UserRole / user_roles | 2.4 | Đã có JoinTable trong User; thêm entity + ID nếu cần quản lý assignedAt, hoặc giữ join và xử lý metadata/audit rõ. Không bắt buộc tạo class. |
| VerificationToken / verification_tokens | 2.6 | Thay/refactor OtpVerification sang hash/expiry/attempts/usedAt, không hai nguồn token. |
| RefreshToken / refresh_tokens | 2.7 | Thêm mapping rotation/family/self-reference, hash/expiry/revokedAt. |
| RoomTypeAmenity / room_type_amenities | 3.6 | Thêm JoinTable vào RoomTypeEntity; không bắt buộc entity/ID riêng. |
| RoomSaleBlock / room_sale_blocks | 3.10 | Thêm mapping khóa bán range/date và actor/cancel metadata; TX kiểm tra booking tương lai. |
| MediaAsset / media_assets | 3.11 | Thêm mapping lifecycle objectKey/checksum/size/scope; liên kết RoomImage/Message sau đó. |
| RoomAllocation / room_allocations | 4.4 | Thêm mapping daterange hoặc value object phù hợp, releasedAt/status, Room/Booking; exclusion/locks ở DB/TX. |
| Refund / refunds | 5.2 | Thay flow RefundRequest, thêm reservation/provider namespace và trạng thái chưa rõ. |
| BankWebhookEvent / bank_webhook_events | 5.3 | Thêm inbox dedupe/hash/verification/processing/lease và target Payment/Refund. |
| ReconciliationCase / reconciliation_cases | 5.4 | Thêm ca sai lệch operationKey/status/retry/assignment/resolution. |
| AuditLog / audit_logs | 5.7 | Mapping JPA hoặc JDBC append-only, đầy đủ actor/from/to/time/outcome; dùng cho history complaint/check-in. |
| ConversationParticipant / conversation_participants | 7.2 | Thêm khóa ghép, role/joined/left/assignment/version và kiểm tra scope hiện tại. |
| ReadReceipt / read_receipts | 7.4 | Thêm khóa ghép participant, lastReadSeq/updatedAt/version, tăng đơn điệu. |
| ComplaintTicket / complaint_tickets | 7.5 | Thêm ticket conversation/type/severity/deadline/assignee/resolution; không thay bằng SupportTicket không đủ trường. |
| CheckinRequest / checkin_requests | 7.6 | Thêm requester/responder/proposed/counter/agreed time/status/version; audit history và reminder integration. |
| ConfigDefinition / config_definitions | 8.1 | Thêm key/value type/allowed scope/validation/public flag. |
| ConfigValue / config_values | 8.2 | Thêm scope/revision/effectiveAt/value/type/actor; NULL uniqueness phải đúng cho GLOBAL. |
| ConfigChangeLog / config_change_logs | 8.3 | Thêm append-only before/after/actor/reason/correlation. |
| EmailTemplate / email_templates | 9.1 | Thêm code/locale/status/active version cùng-template. |
| EmailTemplateVersion / email_template_versions | 9.2 | Thêm subject/html/text/allowed-required variables/published metadata, immutable sau publish. |
| MailEvent / mail_events | 9.3 | Thêm queue/snapshot/version/recipient identity/UNKNOWN/DLQ, hai lớp dedupe theo catalog. |
| MailDeliveryAttempt / mail_delivery_attempts | 9.4 | Thêm attempt number/operator key/provider result/timestamps; resend cùng logical mail. |
| ScheduledReminder / scheduled_reminders | 9.5 | Thêm windowCode/policyVersion/due/cutoff/lease; UQ bốn thành phần và dấu window đã gửi. |
| ReportExport / report_exports | 10.2 | Thêm request hash/scope/query/status/objectKey/asOf/expiry và claim. |
| AnalyticsRefreshState / analytics_refresh_state | 10.3 | Read model tùy chọn, có thể JDBC/upsert; không bắt buộc entity mới. |

Fake BankAccount/BankTransaction/BankLedgerEntry/BankWebhookOutbox ở catalog 11 cũng **chưa thấy class trong source tree dự án này**; triển khai trong service/module/schema Bank riêng. Kết quả này không chứng minh repo/service Fake Bank ở nơi khác chưa tồn tại; không tạo chúng trong package Booking chỉ để đủ số class.

## 7. Các enum cần giữ, sửa hoặc bổ sung

| Enum hiện tại | So với catalog | Việc cần làm |
|---|---|---|
| UserStatus | Thiếu PENDING_VERIFICATION | Thêm sau khi thống nhất flow/migration/test 3 trạng thái hiện tại. |
| PropertyStatus | Khớp DRAFT/PUBLISHED/PAUSED/ARCHIVED | Giữ; không đổi mã chỉ để đổi hình thức. |
| BookingStatusType | Có HOLD và REFUNDED ngoài tập core đích | Migrate theo nghĩa, HOLD về PENDING_PAYMENT khi hợp lệ; REFUNDED không mặc định đồng nghĩa CANCELLED. |
| BookingStatusChangeSource | CUSTOMER/HOST/SUPPORT/ADMIN/SYSTEM/FAKE_BANK | Bổ sung SCHEDULER/WEBHOOK/RECONCILIATION; FAKE_BANK cũ xác minh nguồn trước mapping. |
| RoomStatus / RoomUnitStatus | Hai tập chồng nhau; AVAILABLE/OCCUPIED/OUT_OF_SERVICE ngoài đích | Chọn một RoomOperationalStatus hoặc giữ tên RoomStatus với bộ mã chuẩn. RoomUnitStatus giữ legacy nếu còn mapping; không xóa enum còn dùng. |
| RoomType | SINGLE/DOUBLE/SUITE/DELUXE/FAMILY | Không thay RoomTypeEntity; giữ phân loại phụ hoặc legacy có chủ đích. |
| PaymentProvider | Có FAKE_BANK, khớp core | Giữ. |
| PaymentMethodType | Toàn mã gateway/thẻ, chưa có FAKE_BANK_DEBIT | Thêm/chuyển core theo method đích; provider enum đúng không có nghĩa method đã đúng. |
| PaymentStatusType | Có PENDING/SUCCESS/SUCCEEDED/REFUNDED, thiếu UNKNOWN/RECONCILING | Chuẩn hóa không mất thông tin giao dịch; refund tính riêng. |
| RefundStatusType | Pha approval/transfer: PENDING/APPROVED/REJECTED/PAID/TIMED_OUT; thiếu UNKNOWN/RECONCILING | Core refund execution dùng bộ đích, approval extension nếu còn dùng có enum/lifecycle riêng. |
| RefundDestinationType | WALLET/BANK ngoài flow refund Fake Bank core | Giữ cho legacy/extension, không buộc core có destination ví. |
| MediaAssetStatus | Thiếu DELETED | Thêm theo lifecycle khi tạo MediaAsset. |
| MessageTypeEnum | Diễn tả TEXT/IMAGE/LOCATION/TEMPLATE | Giữ/tái sử dụng làm contentType; thêm MessagePurpose, không map COMPLAINT thành TEXT một cách mất nghĩa. |
| ReviewStatusType | VISIBLE/HIDDEN_BY_ADMIN | Map sang PUBLISHED/HIDDEN có kiểm chứng; thêm PENDING_MODERATION. |
| OutboxStatus | PENDING/PROCESSING/SENT/FAILED | Chuẩn hóa PROCESSED/RETRY/DLQ theo consumer; giữ nghĩa trạng thái cũ trong migration. |
| NotificationTypeEnum / NotificationChannelType | Nhóm sự kiện tổng quát và IN_APP/EMAIL/BOTH | Không thay eventId/eventType dedupe hoặc MailEvent. Có thể giữ nhóm hiển thị ngoài taxonomy business event. |
| OtpPurposeType | REGISTER/FORGOT_PASSWORD | Thống nhất VerificationPurpose sau thay OtpVerification, không duy trì hai purpose độc lập cho cùng token. |
| BehaviorActionType | HI/HIE/HIA | Cần định nghĩa/map dữ liệu cũ, bổ sung FunnelEventType; không suy ra nghĩa các mã. |
| TicketStatusType / TicketTypeEnum | Helpdesk legacy; không đủ state/type/severity complaint đích | Giữ cho SupportTicket; thêm ComplaintStatus/Severity/issue type phù hợp cho ComplaintTicket. |
| CalendarStatusType | AVAILABLE/BOOKED/BLOCKED | Chỉ projection lịch, không quyết định allocation độc lập. |
| UserType | GUEST/USER/CUSTOMER/HOST/SUPPORT/ADMIN | Có thể là mã hỗ trợ, không thay bảng Role/Permission hoặc tự cấp quyền từ enum. |

Các enum mới khác cần theo catalog 12: AllocationStatus; ConversationType/Status; ParticipantRole; MessageStatus; CheckinRequestStatus; ConfigScope/ValueType; TemplateStatus; MailEventType/MailStatus/MailAttemptResult; ReminderType/Status; ReportStatus; BookingSourceChannel/GuestSegment; ActorType/AuditOutcome; InboxStatus; IdempotencyStatus; ReconciliationType/Status; BankEventType. Bộ enum Account/Operation/Transaction của Fake Bank thuộc module riêng.

Các enum extension còn lại (CohostStatusType, DocReviewStatus, EvidenceTypeEnum, HostStatus, KycStatusType, RankNameType, DiscountTypeEnum, VipSubStatusType, VoucherOwnerType, WalletOwnerType, WalletTxType, WithdrawalStatusType, MediaTypeEnum) chưa có yêu cầu đổi bộ giá trị từ catalog lõi; giữ và review khi feature tương ứng triển khai. Không tự xóa KycStatusType hoặc enum chưa có field dùng chỉ vì đợt rà soát này tập trung entity.

## 8. Phụ thuộc code đã kiểm tra: không thể sửa entity riêng lẻ

| Nơi dùng hiện tại | Phát hiện trực tiếp | Ảnh hưởng khi thực hiện thay đổi |
|---|---|---|
| [RoomServiceImp.java](../src/main/java/com/vn/datphong/service/Impl/RoomServiceImp.java), dòng 235; [RoomRateRepository.java](../src/main/java/com/vn/datphong/repository/RoomRateRepository.java), dòng 15→22 | Service truyền roomId vào method truy vấn theo roomType.id=:roomTypeId | Cần truyền đúng roomTypeId hoặc đổi contract/query rõ ràng. Đây là lệch định danh thật trong code; có thể lấy giá sai hoặc báo thiếu giá, không chỉ vấn đề tên method. |
| RoomServiceImp, dòng 218 và 242 | Quote kiểm tra RoomStatus.AVAILABLE và thuế/phí gộp 10% hard-code | Khi chuẩn hóa ACTIVE và policy cấu hình, sửa điều kiện/tính tiền cùng DTO, đồng thời kiểm tra property published/timezone/capacity theo catalog; không chỉ thay enum trong entity. |
| [RoomRepository.java](../src/main/java/com/vn/datphong/repository/RoomRepository.java), [RoomCustomImpl.java](../src/main/java/com/vn/datphong/repository/Impl/RoomCustomImpl.java) | Search/filter/sort còn r.pricePerNight và query booking có HOLD | Chuyển giá ngày chuẩn, trạng thái và tồn kho/hold expiry theo catalog, không để quote một nguồn/search một nguồn. |
| [BookingMapper.java](../src/main/java/com/vn/datphong/mapper/BookingMapper.java) | Đọc roomUnit/getHoldExpiresAt/getTotalPrice | Cần mapping response từ trường canonical; DTO legacy có thể giữ nhưng không đọc nguồn dữ liệu cũ độc lập. |
| [RoomMapper.java](../src/main/java/com/vn/datphong/mapper/RoomMapper.java), [RoomRateMapper.java](../src/main/java/com/vn/datphong/mapper/RoomRateMapper.java), [RoomCatalogMapper.java](../src/main/java/com/vn/datphong/mapper/RoomCatalogMapper.java) | Còn RoomUnit, capacity/pricePerNight/media URL trong mapping | Cập nhật phụ thuộc khi Room là phòng chuẩn, rate theo type và MediaAsset làm metadata; không bỏ field trước migration/API compatibility. |
| [CustomUserDetailsService.java](../src/main/java/com/vn/datphong/service/Impl/CustomUserDetailsService.java), [UserMapper.java](../src/main/java/com/vn/datphong/mapper/UserMapper.java), [UserImportMapper.java](../src/main/java/com/vn/datphong/mapper/UserImportMapper.java) | Auth đọc getPassword và active/status; mapper user nhận password/status/active, import có BCrypt hashing | Đổi mapping hash phải kiểm tra từng đường ghi và encoder, tránh double hashing; flow registration không cho client tự ACTIVE/role. Thực tế entity có tên password không đủ khẳng định plaintext. |
| [UserAccountStatusTest.java](../src/test/java/com/vn/datphong/security/UserAccountStatusTest.java), dòng 169→170 | Assert đúng ba UserStatus hiện tại và test active/status | Thêm pending sẽ cần cập nhật có chủ đích test/contract cùng migration, không xóa test để làm pass. |
| [ReviewMapper.java](../src/main/java/com/vn/datphong/mapper/ReviewMapper.java) | Default ReviewStatusType.VISIBLE | Đồng bộ target status/moderation; không chỉ sửa tên enum trong entity. |
| [BookingServiceImpl.java](../src/main/java/com/vn/datphong/service/Impl/BookingServiceImpl.java) | Nội dung file hiện đều bị comment | Không dùng đoạn code trong file này làm bằng chứng inventory/idempotency đã triển khai. Cần tìm/xây application service thực khi thực hiện core; đợt rà soát này không tuyên bố toàn hệ thống không có implementation khác. |

## 9. Thứ tự thay đổi đề xuất, chưa thực hiện

1. **Chốt migration dữ liệu và mapping nền:** field/cột canonical, Instant, enum transition, nullable/backfill, version/id không trùng. Giữ code hiện tại chạy được tới khi rollout đồng bộ.
2. **Identity/catalog:** User + tokens, role metadata, Room/RoomType/Amenity/policy/rate/media; sửa đúng roomId vs roomTypeId, trạng thái phòng và nguồn giá ở query/mapper.
3. **Booking/inventory/payment/refund:** Booking/Night/History/Allocation, ManyToOne Payment, một intent mở, Refund reservation, inbox/reconciliation/idempotency/outbox/audit. Kiểm thử concurrency/retry/late callback trước bật.
4. **Chat/config/email/reporting:** bổ sung missing mapping và chuẩn hóa Message/Conversation/Notification/Review/UserBehaviorLog, history qua audit, reminder key có windowCode, queue claim/recovery.
5. **Legacy/extension:** chuyển read-only/feature flag và scan/schema theo ADR, không xóa 25 entity ngoài bảng canonical cho bằng số lượng catalog. Fake Bank triển khai ở ranh giới riêng.

**Kiểm chứng cho báo cáo:** đối chiếu đủ 49 entity và 3 class hỗ trợ, kiểm tra coverage với 49 đối tượng catalog. Không chạy test ứng dụng vì đây là tác vụ chỉ đọc/phân tích; không khởi động context có thể tự thay schema. Một báo cáo annotation không thay việc kiểm tra DB constraints và nghiệm thu TX/SV khi triển khai.
