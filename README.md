# datphong

Spring Boot API for room booking.

Đọc [luồng request từ FE đến response](docs/REQUEST_FLOW.md) để biết thứ tự xử lý,
vai trò từng lớp và vị trí đặt breakpoint khi debug đăng nhập hoặc API user.

Xem [báo cáo đối chiếu đặc tả v3.0](docs/SPEC_COMPLIANCE_AUDIT.md) để biết các use case
đã triển khai một phần, phần còn thiếu và kết quả kiểm chứng ngày 16/10/2026.

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

`UserStatus` contains only `ACTIVE`, `IN_ACTIVE`, and `LOCKED`.
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
docker compose up -d
```

Open the project in IntelliJ, reload Maven, and run
`com.vn.datphong.DatphongApplication`.
