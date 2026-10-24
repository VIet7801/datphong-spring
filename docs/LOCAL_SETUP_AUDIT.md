# Kiểm tra quy trình chạy local — 24/10/2026

Phạm vi: clone → build → Docker → khởi tạo/cập nhật DB → seed → khởi động backend.
Không phải kiểm thử đầy đủ toàn bộ use case nghiệp vụ.

## Các lỗi đã sửa

| Vấn đề | Cách xử lý |
|---|---|
| README thiếu cập nhật schema/runtime và RBAC sau baseline | Thêm `cai-dat-local.cmd`, tự chạy đúng thứ tự và dừng khi lỗi |
| `.env` của Compose không tự truyền vào backend/các công cụ Java | Hai entry point local đọc cấu hình Compose, ưu tiên biến môi trường đã đặt |
| Công cụ SQL chịu ảnh hưởng múi giờ JVM như `Asia/SaiGon` | Đặt `Asia/Ho_Chi_Minh` trước khi mở JDBC; cấu hình tương tự cho test |
| Script cập nhật tìm JDK/JDBC khác script khởi tạo | Dùng bộ tìm JDK chung; tìm cả JDBC trong `target/schema-lib` |
| Test khởi động thật chạy cả khi máy chưa có DB | Bật test này bằng `DATPHONG_APP_INTEGRATION=true`; test thường không cần DB |
| Seed buộc đúng 74 bảng, schema hiện tại có 78 bảng | Kiểm tra các bảng cần thiết thay vì số lượng cố định |
| Seed chèn trùng mẫu email xác minh do V005 đã tạo | Tái sử dụng mẫu đã publish, giữ nội dung xác minh thật |
| Seed ghi cột legacy không tồn tại trên DB mới | Dùng `address`, `checkin_local`, `checkout_local`, `price_version`; bỏ các cột thừa `policy_json`, `media_assets.sort_order`, `complaint_tickets.reason` |

Không sửa các migration SQL đã có hoặc checksum của baseline.

## Kết quả kiểm chứng

Môi trường Windows, JDK 17, PostgreSQL 16, Redis, MinIO và Mailpit.
Dùng bản sao các file nguồn, ban đầu không có `target` và `.env`; dùng DB mới,
bucket MinIO riêng và Redis riêng cho kiểm tra HTTP. Cache Maven/Docker của máy
vẫn được sử dụng; đây không phải một máy ảo Windows mới hoàn toàn.

- Maven `package`: **BUILD SUCCESS**; 106 test được phát hiện, 24 chạy thành công,
  82 test tích hợp bỏ qua theo điều kiện, không có failure/error.
- Tạo DB từ đầu: baseline V000 + V002, runtime V006 + V007, RBAC V003–V005 thành công;
  **78 bảng public**, có mẫu email xác minh và quyền ADMIN/SUPPORT cần thiết.
- Seed: **10 tài khoản BCrypt, 100 booking, 55 fixture ảnh/CSV**; kiểm tra giá/đêm,
  tồn kho, ownership, hoàn tiền, hội thoại và history đều thành công.
- Seed `-VerifyOnly`: thành công.
- Chạy lại toàn bộ `cai-dat-local.cmd -Seed`: thành công, không tạo trùng;
  vẫn 10 user, 100 booking và 1 mẫu `EMAIL_VERIFICATION` cho `vi-VN`.
- Thử với `JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/SaiGon`: công cụ DB và backend
  vẫn chạy thành công nhờ đặt múi giờ chuẩn trước khi kết nối.
- `chay-local.cmd`: backend khởi động, Hibernate `validate` thành công trên DB mới.
- HTTP thật: Swagger **200**, OpenAPI **3.1.0**, đăng nhập CUSTOMER và ADMIN thành công,
  ADMIN có `USER_READ`, gọi `/api/v1/users/me` bằng JWT thành công.
- Kiểm tra ánh xạ `.env` tùy chỉnh và ưu tiên biến môi trường đặt sẵn: thành công.

## Cách dùng trên máy mới

Cài Git, JDK 17, Docker Desktop; mở Docker Desktop và đặt `JAVA_HOME`.
Mở terminal tại thư mục chứa `pom.xml`:

```powershell
.\cai-dat-local.cmd -Seed
.\chay-local.cmd
```

Bỏ `-Seed` nếu chỉ cần schema và cấu hình quyền. Tài khoản demo xem
[TAI_KHOAN_TEST.md](TAI_KHOAN_TEST.md). Với DB cũ có dữ liệu nhưng thiếu lịch sử
khởi tạo, công cụ sẽ dừng để giữ dữ liệu; không tự xóa DB/volume để làm lại.
