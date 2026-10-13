# datphong

Spring Boot API for room booking.

## Requirements

- JDK 17
- Docker Desktop
- Git

## Run with Docker

Copy the example environment file if you want to customize local credentials:

```powershell
Copy-Item .env.example .env
```

Start the application and its services:

```powershell
docker compose up --build
```

Useful URLs:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
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
docker compose up -d postgres redis minio
```

Then run `com.vn.datphong.DatphongApplication` from the IDE.

The default local configuration uses:

- PostgreSQL: `jdbc:postgresql://localhost:5435/vivu`
- Redis: `localhost:6379`
- MinIO: `http://localhost:9000`

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
docker compose up --build
```
