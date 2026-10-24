# Khởi tạo entity và schema theo catalog

> Đây là tài liệu baseline 74 bảng. Với code hiện tại, máy Windows mới dùng
> `cai-dat-local.cmd` (hoặc `-Seed` để có dữ liệu demo), rồi `chay-local.cmd`.
> V006 bổ sung 4 bảng runtime, V007 bổ sung index; V003–V005 bổ sung RBAC và
> mẫu email xác minh. Xem [hướng dẫn hiện tại](../README.md#chạy-trên-máy-windows-mới).

Ngày triển khai: 16/10/2026. Đích: [ADR](DB_SPEC_ARCHITECTURE_DECISIONS.md), [catalog](DB_SPEC_CORE_ENTITY_CATALOG.md), [báo cáo thay đổi Java](JAVA_ENTITY_CATALOG_GAP_ANALYSIS.md).

## Kết quả

- Bổ sung 25 class entity để mỗi bảng hiện có đều xem được bằng Java trên IDE, bao gồm UserRole, RoomTypeAmenity và AnalyticsRefreshState dù catalog cho phép join/JDBC. Thêm 4 Embeddable ID; giữ 2 ID class đã có.
- Tổng cộng 74 `@Entity` và 6 `@Embeddable`; BaseEntity vẫn là MappedSuperclass, không phải bảng riêng.
- Sửa mapping/trường còn thiếu của entity lõi, thêm enum phục vụ token, inventory, chat, complaint, check-in, config, email, reporting, inbox và reconciliation.
- Bổ sung Refund riêng; RefundRequest/OtpVerification/SupportTicket và các extension vẫn giữ để tương thích, không tự xóa dữ liệu hoặc triển khai hai workflow lõi song song.
- Payment → Booking là ManyToOne. Bổ sung namespace idempotency/reference, một intent mở/booking; cột refund reservation; message key có sender; reminder key có windowCode; PostgreSQL daterange có UserType riêng.
- Timestamp lõi chuyển Instant/TIMESTAMPTZ; các response DTO liên quan đồng bộ Instant. Giữ tên Java accessor cũ ở một số field nhưng ánh xạ cột chuẩn, ví dụ User.password → password_hash, Booking.user → customer_id, RoomRate.nightlyRate → amount. getHoldExpiresAt là alias tương thích, chỉ expires_at là cột expiry chuẩn.
- Các mã enum cũ vẫn được giữ để không phá dữ liệu/API legacy; bổ sung mã đích và chọn default chuẩn cho model mới. Chưa thực hiện migration nghiệp vụ để xóa mọi alias enum cũ.
- Giữ `ddl-auto: validate` mà workspace đã cấu hình; không dùng Hibernate update/create để tự thay DB khi khởi động.

## DB local đã áp dụng

Đã kiểm tra trực tiếp `localhost:5435/vivu`: ban đầu 62 bảng public và tất cả đều không có dòng dữ liệu. Sau V001/V002 có **74 bảng**, các bảng mới đều rỗng; không thêm seed nghiệp vụ.

12 bảng mới: `co_hosts`, `dispute_evidence`, `host_vip_subscriptions`, `kyc_documents`, `otp_verifications`, `refund_requests`, `support_tickets`, `user_behavior_logs`, `vouchers`, `wallet_transactions`, `wallets`, `withdrawal_requests`.

Các bảng nghiệp vụ đã có như refunds, media_assets, refresh_tokens, config_*, mail_* được bổ sung entity và cột/constraint phù hợp. Bảng legacy/code-only được tạo để các entity còn scan có schema tương ứng, không đồng nghĩa tính năng extension đã hoàn tất hoặc được bật.

Không drop bảng/cột hoặc seed user/role/booking. Migration giữ cột legacy, nới NOT NULL ở cột đã ngừng làm nguồn chuẩn và chuyển PK room_rates/booking_nights sang id phù hợp ORM, đồng thời giữ unique khóa nghiệp vụ. Một số constraint cũ được thay bằng bộ phù hợp catalog; đây là khởi tạo **DB rỗng**, không phải công cụ migrate dữ liệu sản xuất.

## SQL có phiên bản và cách chạy

Nguồn: `scripts/sql/checksums.sha256`; không sửa file migration đã áp dụng, thay đổi sau dùng version mới.

| File | Khi nào dùng |
|---|---|
| V000__empty_database_baseline.sql | Một schema/DB hoàn toàn chưa có bảng: tạo baseline từ metadata entity; **không chạy V001 sau baseline này** |
| V001__initialize_entity_catalog.sql | Schema cũ rỗng như snapshot đã kiểm tra, tạo bảng thiếu/bổ sung cột/chuẩn hóa constraint. Tự từ chối nếu có bảng public chứa dữ liệu |
| V002__complete_catalog_constraints.sql | Chạy sau baseline V000 hoặc khởi tạo V001: hoàn thiện amenity code unique, refund percent và kiểm tra tiền hủy |

Chạy từ thư mục project có pom.xml:

```powershell
.\khoi-tao-db.cmd
```

Đây là điểm chạy duy nhất trên Windows; không cần chạy thủ công cả ba file SQL. File CMD gọi `scripts/initialize-schema.ps1`, tự tìm JDK 17 trở lên từ `JAVA_HOME`, PATH hoặc thư mục cài JDK. JDBC được Maven Wrapper tải theo dependency của dự án vào `target/schema-lib/`; lần đầu cần Internet.

Script dùng PostgreSQL đang chạy theo datasource; nếu cổng local mặc định `5435` chưa mở thì tự chạy `docker compose up -d --wait postgres`. Máy cần cài và mở Docker Desktop để dùng nhánh này. Khi khởi động bằng Compose, script đọc cấu hình PostgreSQL từ Compose, bao gồm `.env`; biến `SPRING_DATASOURCE_*` được đặt sẵn luôn được ưu tiên. Khi dùng PostgreSQL tự cài, đặt các biến này cho đúng tài khoản của máy đó. Spring Boot không tự đọc `.env`, nên cấu hình chạy ứng dụng cũng cần khớp nếu đã đổi thông tin mặc định.

`SchemaTool initialize` kiểm tra SHA256 của cả ba file trước khi kết nối. `.gitattributes` cố định LF cho các file SQL có phiên bản để checksum giữ nguyên khi pull trên Windows. Database chưa tồn tại được tạo bằng kết nối tới database quản trị `postgres`; tài khoản cần quyền `CREATEDB`. Database đã tồn tại chỉ cần quyền DDL và tạo extension `btree_gist`.

Sau khi lấy advisory lock, runner kiểm tra các bảng public đều rỗng và tự chọn V000 nếu chưa có bảng, còn lại chọn V001 cho cấu trúc DB cũ tương thích với snapshot đã rà soát. File đã chọn và V002 chạy trong **cùng một transaction**, có timeout và kiểm tra số dòng trước/sau; lỗi ở bất kỳ file nào sẽ rollback toàn bộ thay đổi cấu trúc. Nếu bước tạo database mới đã thành công nhưng khởi tạo bảng thất bại, database trống vẫn được giữ lại để chạy lại script.

Runner lưu hai dòng lịch sử phiên bản/checksum trong `datphong_schema.initialization_history`, cùng transaction với DDL. Đây là bảng kỹ thuật ngoài schema `public`, không phải entity nghiệp vụ; 74 bảng nghiệp vụ vẫn rỗng. Chạy lại với lịch sử khớp sẽ bỏ qua toàn bộ SQL, kể cả khi ứng dụng đã tạo dữ liệu sau đó. File đã áp dụng mà đổi checksum sẽ bị từ chối để yêu cầu migration mới. DB có dữ liệu nhưng chưa có lịch sử sẽ dừng và giữ nguyên DB. Không drop/truncate bảng hay seed dữ liệu nghiệp vụ.

Log metadata trước/sau, version/checksum nằm ở `target/schema-audit/`, không chứa bản dump dữ liệu người dùng. Runner chỉ hỗ trợ khởi tạo và kiểm tra; DB đã có dữ liệu cần migration riêng đã review. Hai lệnh thủ công cũ `SchemaTool inspect` và `SchemaTool apply <sql-file>` vẫn được giữ; `apply` chỉ áp dụng một file trong một transaction.

Nếu cần chỉ định đường dẫn JDK hoặc JDBC có sẵn:

```powershell
.\khoi-tao-db.cmd -JavaHome 'C:\Program Files\Java\jdk-17'
.\khoi-tao-db.cmd -JavaHome 'C:\Program Files\Java\jdk-17' -JdbcJar 'C:\drivers\postgresql.jar'
```

## Kiểm tra

```powershell
# Test thông thường; cần DB local cho contextLoads hiện có của project.
.\mvnw.cmd test

# Bật thêm kiểm tra trực tiếp PostgreSQL cho mapping, baseline và daterange.
$env:DATPHONG_SCHEMA_VALIDATE = 'true'
.\mvnw.cmd '-Dtest=EntitySchemaTest' test
```

EntitySchemaTest bootstrap toàn bộ 74 model, export DDL không thay DB, validate từng bảng/cột trên DB và SELECT từng entity. Test baseline tạo schema tạm mang UUID, dựng baseline, validate rồi dọn đúng schema tạm. Test persistence ghi thử model lõi trong transaction và luôn rollback, không để lại business row; sequence có thể đã cấp số dù transaction rollback.

Đã kiểm tra điểm chạy `khoi-tao-db.cmd` bằng các database thử riêng: tự tìm JDK, tải JDBC, tạo database chưa tồn tại và 74 bảng rỗng; chọn V001/V002 cho cấu trúc cũ tương thích; chạy lại nhận biết lịch sử; từ chối DB có dữ liệu chưa được khởi tạo bằng runner và giữ nguyên dữ liệu; lỗi cố ý ở V002 hoàn tác cả bảng do V000 tạo. File SQL sai checksum bị chặn trước khi kết nối. Các database thử được xóa sau kiểm tra; DB `vivu` chỉ đọc để đối chiếu. Máy kiểm tra chưa có Docker CLI nên chưa kiểm chứng trực tiếp nhánh khởi động container và đọc cấu hình Compose.

Khi xem IntelliJ: refresh Maven để nhận Lombok/JPA classes mới; Database → Synchronize để nạp lại 74 bảng. Không cần bật ddl-auto:update để IDE nhìn thấy entity.

## Ranh giới triển khai

Đây là **khởi tạo model, mapping, schema và kiểm tra persistence**. Chưa triển khai toàn bộ service/API/state machine/scheduler cho 36 UC. Các cột reservation/isOpen/audit/lease tạo nền tảng, nhưng quy tắc aggregate (ví dụ tổng refund), quyền truy cập và retry vẫn cần application service và test nghiệp vụ tiếp theo. Enum/constraint không tự thực hiện các quy tắc đó.

Sửa thêm caller báo giá để truyền đúng roomTypeId vào RoomRateRepository và nhận trạng thái phòng ACTIVE; không triển khai lại toàn bộ pricing/search workflow trong đợt này.

Fake Bank account/ledger/transaction/outbox trong catalog là ranh giới service/schema riêng, chưa tạo nhầm trong public Booking; Wallet của Booking vẫn chỉ là extension. Không coi 74 bảng này là toàn bộ hệ thống phân tán đã hoàn tất.
