# datphong

Spring Boot API for room booking.

Đã tách API hồ sơ cá nhân và quản trị User, bổ sung Permission và giới hạn Audit Log
cho ADMIN có AUDIT_READ. Xem [báo cáo triển khai và hướng dẫn Swagger](docs/BAO_CAO_USER_PERMISSION_AUDIT_LOG.md).
Với DB đã có schema, chạy .\cap-nhat-rbac.cmd để bổ sung catalog/grant;
dùng -VerifyOnly để kiểm chứng. Khởi động lại backend và đăng nhập lấy JWT mới sau cập nhật.

Đọc [luồng request từ FE đến response](docs/REQUEST_FLOW.md) để biết thứ tự xử lý,
vai trò từng lớp và vị trí đặt breakpoint khi debug đăng nhập hoặc API user.

Xem [báo cáo đối chiếu toàn dự án với đặc tả v3.0](docs/PROJECT_SPEC_AUDIT.md) để biết
trạng thái đủ 36 use case, lỗi cần ưu tiên và kết quả kiểm chứng ngày 16/10/2026.

## Requirements

- JDK 17
- Docker Desktop
- Git

## Start dependencies with Docker

Docker Compose runs PostgreSQL, Redis, and MinIO only. Run the Spring Boot
application from IntelliJ or Maven on port 8080.

Copy the example environment file if you want to customize local credentials:

```powershell
Copy-Item .env.example .env
```

Start the dependencies:

```powershell
docker compose up -d
```

MinIO uses the community mirror `dappros/minio:RELEASE.2025-09-07T16-13-09Z`
because the original `minio/minio` image is no longer publicly available.
Its healthcheck runs `mc ready local`, which is included in this image.

Useful URLs:

- API: http://localhost:8080
- Swagger UI (after starting the application): http://localhost:8080/swagger-ui/index.html
- MinIO Console: http://localhost:9001
- PostgreSQL: `localhost:5435`
- Redis: `localhost:6379`

Stop all containers:

```powershell
docker compose down
```

Reset local database, Redis, and MinIO data:

```powershell
docker compose down -v
```

## Khởi tạo DB bằng một lệnh trên Windows

Sau khi pull code, mở terminal tại thư mục có `pom.xml` và chạy:

```powershell
.\khoi-tao-db.cmd
```

Cần cài JDK 17 trở lên. Nếu chưa có PostgreSQL đang chạy tại `localhost:5435`,
hãy cài và mở Docker Desktop; script sẽ tự khởi động dịch vụ `postgres` trong
Docker Compose. JDBC được tải tự động bằng Maven Wrapper ở lần chạy đầu.

Script tạo database nếu chưa tồn tại, tự chọn `V000 → V002` cho DB chưa có bảng
hoặc `V001 → V002` cho DB cũ chưa có dữ liệu. Hai tệp SQL chạy trong cùng một giao dịch;
lỗi ở bất kỳ bước nào sẽ hoàn tác thay đổi cấu trúc. Kết quả là 74 bảng rỗng.
Lịch sử khởi tạo được lưu trong schema kỹ thuật `datphong_schema`, tách khỏi 74 bảng
nghiệp vụ ở `public`. Chạy lại sẽ nhận biết phiên bản đã áp dụng và bỏ qua SQL.
DB có dữ liệu nhưng chưa có lịch sử này sẽ bị từ chối, không bị xóa để tạo lại.
V001 dành cho cấu trúc DB cũ đã rà soát; với máy mới nên bắt đầu bằng database chưa có bảng.

Thông tin kết nối lấy từ `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` hoặc mặc định trong `src/main/resources/application.yaml`.
Tài khoản cần quyền tạo bảng và extension `btree_gist`; nếu database chưa tồn tại
thì cần thêm quyền `CREATEDB`. Cấu hình Docker mặc định đã đáp ứng các quyền này.

Chi tiết và cách chỉ định JDK/JDBC: [hướng dẫn khởi tạo schema](docs/ENTITY_SCHEMA_INITIALIZATION.md).

## Tạo dữ liệu test theo đặc tả

Khi PostgreSQL đã có schema và MinIO đang chạy, tạo bộ dữ liệu UC bằng:

```powershell
.\tao-du-lieu-test.cmd
```

Script tạo 100 booking và dữ liệu liên quan; 3 CUSTOMER, 3 HOST, 3 SUPPORT,
1 ADMIN. Admin bootstrap hiện có được tái dùng và đổi sang mật khẩu test đúng
một lần. Mật khẩu dùng BCrypt cost 12; tài khoản và mật khẩu trước mã hóa ở
[docs/TAI_KHOAN_TEST.md](docs/TAI_KHOAN_TEST.md). Các ca test, ngày dữ liệu và số
lượng bản ghi ở [docs/DU_LIEU_TEST.md](docs/DU_LIEU_TEST.md).

Chạy lại không tạo trùng, không đổi lại mật khẩu hay dịch ngày. Kiểm chứng dữ liệu:

```powershell
.\tao-du-lieu-test.cmd -VerifyOnly
```

Thông tin kết nối dùng biến môi trường hoặc giá trị mặc định trong
`application.yaml`, giống backend. Script không tự nạp `.env` của Docker Compose.
Nếu đã đổi cấu hình Compose, đặt các biến `SPRING_DATASOURCE_*` và `MINIO_*`
tương ứng trước khi chạy.

## Run from an IDE

Start only the dependencies:

```powershell
docker compose up -d
```

Then run `com.vn.datphong.DatphongApplication` from the IDE.

After editing Java code, rerun the application in IntelliJ to apply changes.
If you customize credentials in `.env`, set the matching environment variables
in the IntelliJ Run Configuration too; the application does not load Compose's
`.env` file automatically.

The default local configuration uses:

- PostgreSQL: `jdbc:postgresql://localhost:5435/vivu`
- Redis: `localhost:6379`
- MinIO: `http://localhost:9000`

## Account status and existing databases

`UserStatus` contains `ACTIVE`, `IN_ACTIVE`, `LOCKED`, and `PENDING_VERIFICATION`.
Login and authenticated API requests require both `active = true` and
`status = ACTIVE`. A null `active` flag is treated as disabled. Locked or disabled
accounts cannot use previously issued JWTs while they remain locked or disabled.

Before starting the updated backend against an existing database, run:

```powershell
Get-Content -Raw scripts/sql/normalize-user-status.sql | docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'
```

The script converts legacy status names, updates the PostgreSQL check
constraint, and sets the default for raw SQL inserts to `IN_ACTIVE`.
It preserves `active` flags, account records, and passwords.
Hibernate `ddl-auto: update` does not replace this data migration.

## Maven commands

Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

## Push to GitHub

Create an empty repository on GitHub, then run these commands in this folder:

```powershell
git init
git add .
git commit -m "Initial project setup"
git branch -M main
git remote add origin https://github.com/<your-username>/<repo-name>.git
git push -u origin main
```

After that, your teammate can clone and run:

```powershell
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>
Copy-Item .env.example .env
.\khoi-tao-db.cmd
docker compose up -d
```

Open the project in IntelliJ, reload Maven, and run
`com.vn.datphong.DatphongApplication`.
