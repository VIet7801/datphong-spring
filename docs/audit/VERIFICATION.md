# Minh chứng kiểm tra đặc tả - 16/10/2026

Đọc cùng [báo cáo đối chiếu](../SPEC_COMPLIANCE_AUDIT.md).

## 1. Kiểm tra đã thực hiện

| Kiểm tra | Kết quả | Phạm vi bằng chứng |
|---|---|---|
| UserAccountStatusTest | 14 test đạt, 0 failure/error | Login với BCrypt, JWT ký thực; ACTIVE/IN_ACTIVE/LOCKED, active=false/null, status=null, user bị xóa, ADMIN bị khóa; repository giả |
| UC-01: create user phải hash password | Không đạt | Giá trị truyền vào repository vẫn bằng chuỗi password test |
| UC-05: CUSTOMER không được tạo phòng | Không đạt | MockMvc + filter chain thật: dự kiến 403, thực tế 201; service persistence giả |
| UC-05: đổi tên phòng phải đổi entity/response | Không đạt | Dự kiến Updated name, thực tế Original name; repository giả |
| UC-06: upload phải lưu metadata | Không đạt | Không gọi RoomImageRepository.save; MinIO/repository giả |
| UC-07: khách chưa đăng nhập được search | Không đạt | MockMvc + filter chain thật: dự kiến 200, thực tế 401 |
| UC-06: nhận multipart Servlet Part | Đạt | MockPart nhận HTTP 201, Part không null và đúng tên file tại service; không kiểm tra upload object thật |

Các probe đặt kỳ vọng theo đặc tả; failure là thiếu sót của hành vi hiện tại được tái hiện,
không phải lỗi biên dịch. Không suy rộng các test mock thành bằng chứng đã chạy E2E với PostgreSQL/MinIO thật.

Tổng bằng chứng: **14 test trạng thái tài khoản đạt; 6 probe đối chiếu gồm 1 đạt và 5 failure,
0 error**. Lần chạy probe cuối kết thúc 02:07:43 ngày 16/10/2026 (Asia/Bangkok).
Maven trả BUILD FAILURE ở lệnh probe do 5 assertion chưa đạt; biên dịch main/test thành công.

## 2. Nguồn bằng chứng

- Mã test trạng thái hiện có: [UserAccountStatusTest](../../src/test/java/com/vn/datphong/security/UserAccountStatusTest.java).
- Mã probe đối chiếu: [SpecificationAuditTest.java](SpecificationAuditTest.java).
- Kết quả máy chạy audit: log trong thư mục TEMP và XML của Maven Surefire tại `target/surefire-reports`.
  Thư mục `target` là output build, có thể mất khi chạy clean; bảng kết quả ở trên là bản ghi bền vững trong repo.
- Đã đọc metadata PostgreSQL local: có index unique `uq_users_email_normalized` trên `lower(btrim(email))`;
  chưa có migration versioned tương ứng trong repo, nên không coi đó là khả năng bootstrap DB sạch đã đạt.

## 3. Chạy lại khi cần tái hiện

Mở terminal ở thư mục chứa pom.xml và dùng JDK/JAVA_HOME phù hợp với dự án.

Kiểm tra trạng thái tài khoản:

```powershell
.\mvnw.cmd -B -ntp '-Dtest=UserAccountStatusTest' test
```

Probe audit được giữ ngoài source set mặc định. Muốn chạy lại, sao chép nó vào source test tạm:

```powershell
$probePath = 'src/test/java/com/vn/datphong/audit/SpecificationAuditTest.java'
if (Test-Path -LiteralPath $probePath) { throw 'File đã tồn tại; hãy kiểm tra trước khi ghi đè.' }
New-Item -ItemType Directory -Path (Split-Path $probePath) -Force | Out-Null
Copy-Item -LiteralPath docs/audit/SpecificationAuditTest.java -Destination $probePath
.\mvnw.cmd -B -ntp '-Dtest=SpecificationAuditTest' test
```

Lệnh probe dự kiến thất bại ở các yêu cầu chưa sửa. Sau khi kiểm tra, gỡ bản sao tạm trong `src/test`
và chạy Maven clean ở lần build tiếp theo để loại các class probe cũ khỏi `target/test-classes`.
Mã chuẩn trong `docs/audit` được giữ để đối chiếu sau khi sửa nghiệp vụ.

## 4. Cách hiểu kiểm tra multipart

Controller hiện nhận `jakarta.servlet.http.Part`. Vì vậy probe phải dùng `MockPart` để mô phỏng
`request.getPart()` như Servlet container. Chỉ dùng `MockMultipartFile` có thể làm Part bị null
trong MockMvc dù file nằm trong collection MultipartFile của request giả.
Không dùng khác biệt của mô hình mock đó để kết luận endpoint production không nhận được multipart.
Việc nhận Part cũng không chứng minh nội dung file đã được kiểm tra MIME/checksum hay lưu metadata đúng.

## 5. Chưa kiểm chứng

- Chưa chạy toàn bộ contextLoads trên DB local có ddl-auto:update.
- Chưa dựng database mới hoàn toàn hoặc chạy Testcontainers/PostgreSQL thật.
- Chưa thử ghi media thật, lỗi MinIO sau upload, rollback hoặc dọn object mồ côi.
- Chưa có test concurrency/hold/payment/refund/expiry vì các luồng đó chưa được triển khai.
- Chưa có load test, EXPLAIN ANALYZE, kiểm thử FE, callback/scheduler nhiều instance hoặc monitoring.

Lượt audit không sửa code nghiệp vụ và không sửa dữ liệu/schema của database.
