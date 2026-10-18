# Minh chứng rà soát toàn dự án — 16/10/2026

Đọc cùng [PROJECT_SPEC_AUDIT.md](../PROJECT_SPEC_AUDIT.md). Các kết quả bên dưới thay thế số liệu của lượt kiểm tra cũ. Nguồn yêu cầu là PDF nghiệp vụ v3.0, 51 trang, 36 use case.

## 1. Kết quả kiểm thử hiện tại

| Bộ kiểm tra | Kết quả | Phạm vi bằng chứng |
|---|---|---|
| DatphongApplicationTests | 1 đạt | Spring context khởi động với ddl-auto=validate, không tự sửa schema |
| UserAccountStatusTest | 16 đạt | BCrypt login, JWT ký thực, ACTIVE/IN_ACTIVE/LOCKED/PENDING_VERIFICATION, active=false/null, status=null, user đã xóa và ADMIN bị khóa. Repository giả |
| EntitySchemaTest trong lượt mặc định | 2 đạt, 3 bỏ qua | Bootstrap metadata 74 entity, xuất DDL ra file, kiểm tra StayRange. Ba test bật bằng DATPHONG_SCHEMA_VALIDATE không chạy trong lượt mặc định |
| EntitySchemaTest.everyMappedTableAndColumnValidatesAgainstLocalDatabase chạy riêng | 1 đạt | Hibernate validate và SELECT tối đa một hàng cho từng entity trên PostgreSQL local, bao gồm tên cột và kiểu tùy biến. Không ghi dữ liệu |
| SpecificationAuditTest | 11 test: 2 đạt, 9 failure, 0 error | Các probe đặt kỳ vọng theo PDF để phát hiện khoảng thiếu, giữ ngoài source set mặc định |

Lượt mặc định kết thúc lúc **20:05:45, 16/10/2026, Asia/Bangkok**: Maven BUILD SUCCESS, tổng 22 test khai báo, 19 chạy đạt và 3 bỏ qua. XML probe ghi 11 testcase hoàn tất lúc khoảng 20:08:16; suite mapping DB hoàn tất khoảng 20:08:37. Kết quả probe/mapping được đối chiếu trực tiếp từ XML Surefire, không diễn giải failure nghiệp vụ thành lỗi biên dịch hoặc test error.

## 2. Chi tiết 11 probe theo PDF

| Test | Kỳ vọng theo đặc tả | Quan sát | Kết quả |
|---|---|---|---|
| uc01UserCreationHashesPasswordBeforeSave | Password được hash trước save | Giá trị đưa vào repository vẫn bằng password đầu vào | Không đạt — F01 |
| uc05CustomerCannotCreateRooms | CUSTOMER bị từ chối 403 | HTTP 201 với service persistence giả | Không đạt — F02 |
| uc05RoomUpdateChangesTheStoredEntity | Entity và response có tên mới | Tên vẫn là Original name | Không đạt — F04 |
| uc05CreatedRoomHasRequiredPropertyAndRoomTypeRelations | Room có quan hệ property và room type hợp lệ | Cả hai quan hệ đều null khi đưa vào repository, dù request có ID | Không đạt — UC-05 |
| uc06UploadPersistsMediaMetadata | Upload nối với metadata/liên kết ảnh | Không gọi RoomImageRepository.save | Không đạt — F05 |
| uc06UploadEndpointAcceptsMultipartFile | Servlet Part được bind đúng | HTTP 201, Part không null, filename đúng | Đạt ở boundary binding |
| uc07AnonymousGuestCanSearchRooms | GUEST tìm công khai nhận 200 | HTTP 401 | Không đạt — F06 |
| uc08DraftPropertyCannotBeShownAsPublicRoomDetail | Không trả chi tiết công khai của property DRAFT | Service trả response thay vì từ chối | Không đạt — F08 |
| uc08QuoteRejectsZeroGuests | Số khách phải dương | Service chấp nhận 0 khách và tạo quote | Không đạt — F08 |
| uc08QuoteRejectsAnIncompleteDatePair | Một đầu ngày thiếu phải được báo input không hợp lệ | Service trả response dạng giá tham khảo | Không đạt — F08 |
| uc08QuoteSumsEachNightAndUsesRoomTypeWithHalfOpenDateRange | Ba đêm giá khác nhau cộng đúng và query theo roomTypeId với khoảng nửa mở | 100000 + 150000 + 200000 = subtotal 450000, 3 đêm; đúng roomTypeId và checkOut truyền vào repository | Đạt ở boundary tính subtotal/query parameters |

Các probe HTTP dùng SecurityFilterChain thật và RoomService giả. Các probe service dùng repository/MinIO giả. Vì vậy:
- HTTP 201 của probe quyền không chứng minh insert Room trên PostgreSQL thật thành công.
- Quan hệ null là quan sát trên entity đưa vào repository, chưa thực hiện một insert lỗi lên DB.
- Tổng giá đúng không chứng minh thuế 10%, chính sách hủy viết cứng, TTL quote hoặc tồn kho đã đúng.
- Upload nhận Part đúng không chứng minh MIME/checksum/lifecycle, object thật hay metadata đã lưu.
- Probe không hash password gọi phương thức create trực tiếp để kiểm tra nội dung ghi, không chứng minh có đăng ký public.

Mã probe: [SpecificationAuditTest.java](SpecificationAuditTest.java). Constructor RoomServiceImp đã được cập nhật theo dependency RoomRateRepository hiện tại, không thay đổi logic sản phẩm.

## 3. Kiểm tra DB và source

Đã chạy SchemaTool inspect ở chế độ đọc:
- localhost:5435/vivu có 74 bảng public, không bảng nào có dòng dữ liệu.
- Có unique email chuẩn hóa uq_users_email_normalized.
- Có exclusion ex_catalog_room_allocation cho allocation chưa release.
- Có CHECK ngày/tiền booking, refund reservation và unique amenity code.
- Hibernate validate/SELECT 74 entity đạt.

Log/metadata/XML của lượt này:
- target/spec-audit/default-tests.log
- target/spec-audit/probes.log
- target/spec-audit/db-inspection.txt
- target/spec-audit/results.json — tổng hợp từ XML
- target/surefire-reports/*spec-audit*.xml

Thư mục target là output build, không phải nguồn yêu cầu hoặc dữ liệu nghiệp vụ; có thể bị mất khi clean. Bảng kết quả trong tài liệu là bản ghi bền vững.

## 4. Chạy lại

Tại thư mục có pom.xml, dùng JDK 17 trở lên tương thích cấu hình dự án. PostgreSQL local phải đang chạy nếu chạy contextLoads hoặc mapping DB.

Bộ mặc định, không chạy các test ghi DB mở rộng:

~~~powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
$env:DATPHONG_SCHEMA_VALIDATE = 'false'
.\mvnw.cmd -B -ntp test
~~~

Chỉ bật test mapping/SELECT DB, không bật các test tạo schema hoặc persistence roundtrip:

~~~powershell
$env:DATPHONG_SCHEMA_VALIDATE = 'true'
.\mvnw.cmd -B -ntp '-Dtest=EntitySchemaTest#everyMappedTableAndColumnValidatesAgainstLocalDatabase' test
~~~

Probe audit giữ ngoài src/test. Bản sao tạm chỉ được tạo khi chưa có file đích:

~~~powershell
$probePath = Join-Path (Get-Location) 'src/test/java/com/vn/datphong/audit/SpecificationAuditTest.java'
if (Test-Path -LiteralPath $probePath) { throw 'File đã tồn tại, không ghi đè.' }
New-Item -ItemType Directory -Path (Split-Path $probePath) -Force | Out-Null
Copy-Item -LiteralPath docs/audit/SpecificationAuditTest.java -Destination $probePath
try {
    .\mvnw.cmd -B -ntp '-Dtest=SpecificationAuditTest' test
} finally {
    Remove-Item -LiteralPath $probePath
    $classDirectory = Join-Path (Get-Location) 'target/test-classes/com/vn/datphong/audit'
    if (Test-Path -LiteralPath $classDirectory) {
        Get-ChildItem -LiteralPath $classDirectory -Filter 'SpecificationAuditTest*.class' -File |
            ForEach-Object { Remove-Item -LiteralPath $_.FullName }
    }
}
~~~

Lệnh probe dự kiến không đạt trong trạng thái hiện tại vì 9 assertion phát hiện thiếu sót. Dọn cả bản sao source và class của probe để lần chạy suite mặc định không nhặt lại các test tạm. Không cần xóa các class/test khác.

## 5. Giới hạn và những gì không chạy

- Không chạy normalize-user-status.sql, V000/V001/V002 hoặc lệnh khởi tạo lại DB trong lượt này.
- Không chạy hai test tạo schema tạm/persistence roundtrip; không seed user/role/phòng/booking hay gửi email.
- Không thử storage MinIO, Fake Bank hoặc frontend thật.
- Không chạy concurrency inventory/payment/refund, callback/expiry nhiều instance hay fault injection: workflow tương ứng chưa tồn tại.
- Không có số đo EXPLAIN, load test, p95/p99, cache isolation/stampede hoặc bằng chứng triển khai production.
- SQL normalize-user-status.sql xung đột với bốn trạng thái hiện tại là phát hiện từ source, chưa áp dụng để thử trên dữ liệu.

Kết quả build/mapping đạt chỉ chứng minh phần được kiểm tra. Không dùng chúng để kết luận 36 use case đã hoàn tất.
