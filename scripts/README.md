# Công cụ chạy local

Chạy các lệnh `.cmd` tại thư mục chứa `pom.xml`:

| Lệnh | Công việc |
|---|---|
| `cai-dat-local.cmd -Seed` | Cài đầy đủ dịch vụ, schema, RBAC và dữ liệu demo |
| `chay-local.cmd` | Chạy backend với cấu hình local |
| `khoi-tao-db.cmd` | Khởi tạo baseline DB |
| `cap-nhat-schema.cmd` | Cập nhật schema runtime |
| `cap-nhat-rbac.cmd` | Cập nhật quyền và mẫu email xác minh |
| `tao-du-lieu-test.cmd` | Tạo dữ liệu demo; `-VerifyOnly` để kiểm tra |

Trong thư mục này:

- `local-environment.ps1`: hàm dùng chung để tìm JDK/JAR và đọc cấu hình Compose.
- `local.ps1`: điều phối cài đặt hoặc chạy backend.
- `initialize-schema.ps1`, `update-user-rbac.ps1`, `seed-demo-data.ps1`: chuẩn bị
  môi trường và gọi công cụ Java cho từng thao tác.
- `java/`: thực thi khởi tạo schema, RBAC và seed qua JDBC.
- `sql/`: migration có thứ tự phụ thuộc. Không chạy tùy ý từng file hoặc sửa
  migration/checksum đã áp dụng. Xem [hướng dẫn dự án](../README.md).

Các script SQL/Java được giữ tách theo trách nhiệm; mã tìm JDK và thư viện chỉ
nằm trong một file dùng chung.
