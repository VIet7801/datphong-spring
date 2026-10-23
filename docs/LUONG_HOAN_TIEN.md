# Luồng hoàn tiền

Phạm vi: ba hàm `RefundService.request/claim/complete`, tác vụ nền gọi chúng và nối luồng hủy booking/thanh toán đến muộn. Đối chiếu UC-13 trang 15–16 và phần bù trừ UC-36 trang 37–38 trong đặc tả. Đây chưa phải toàn bộ UC-13/UC-35: màn hình/API SUPPORT và Fake Bank chạy HTTP riêng nằm ngoài lần sửa này.

## Từ lúc tạo yêu cầu đến lúc hoàn tất

1. Khi chấp nhận hủy booking, `BookingServiceImpl` tính tiền theo snapshot chính sách, giải phóng phòng và chuyển booking sang `REFUND_PENDING`. Nếu thanh toán đến sau khi booking hết hạn, `PaymentOutcomeServiceImpl` yêu cầu hoàn toàn bộ khoản thu đó.
2. `request` khóa booking rồi payment. Payment phải có khoản thu được xác nhận; tổng các yêu cầu đã cam kết cộng yêu cầu mới không được vượt `capturedAmount`. Cùng mã và cùng nội dung trả lại yêu cầu cũ; nội dung khác bị từ chối. Yêu cầu và sự kiện outbox được lưu cùng transaction.
3. Cứ 15 giây, `RefundProcessor` tìm tối đa 50 yêu cầu đến hạn. `claim` khóa theo thứ tự booking → payment → refund, kiểm tra lại tiền và trạng thái, tăng số lần thử, chuyển sang `PROCESSING` rồi commit. Lease 60 giây ngăn hai worker nhận cùng một yêu cầu.
4. Worker gọi Fake Bank sau khi `claim` đã commit. Bank có transaction riêng. Mọi lần thử dùng cùng key, cùng giao dịch thu và cùng số tiền. Nếu bank đã xử lý, bank trả lại kết quả cũ; key trùng nhưng nội dung khác bị từ chối.
5. `complete` mở transaction mới, kiểm tra số tiền/mã trả về. Thành công thì chuyển refund sang `SUCCEEDED`, cộng `Payment.refundedAmount` đúng một lần, ghi lịch sử booking và outbox. Gọi lại với kết quả cũ không cộng tiền lần nữa.
6. Hủy booking chỉ kết thúc ở `REFUNDED` khi mọi yêu cầu hoàn tiền hủy của booking đã thành công. Với thanh toán đến muộn của booking đã hết hạn, booking trở về `EXPIRED` sau khi bù trừ xong. Booking đang `CANCELLED` hoặc đã xác nhận bằng một payment khác được giữ theo trạng thái hiện tại.

Payment đã thu vẫn là `SUCCEEDED`; `capturedAmount` và `refundedAmount` cho biết tiền vào/ra. Không đổi payment thành mã `REFUNDED` cũ để thay thế việc ghi số tiền hoàn.

## Khi có lỗi

| Trường hợp | Xử lý |
|---|---|
| Bank tạm thời không sẵn sàng, chưa tạo giao dịch | Lưu lỗi, chờ `15 × số lần thử` giây, tối đa 300 giây |
| Timeout sau khi bank đã chuyển tiền | Refund ở `UNKNOWN`; lần sau dùng key cũ để nhận lại kết quả, không tạo yêu cầu khác |
| Bank thành công nhưng transaction nội bộ rollback | Yêu cầu vẫn `PROCESSING`; sau lease, worker nhận lại và áp dụng kết quả đã lưu tại bank |
| Kết quả sai số tiền/mã giao dịch hoặc bank báo lỗi nghiệp vụ cuối | `NEEDS_ATTENTION`, dừng tự động để kiểm tra |
| Hết 5 lượt tự động | `NEEDS_ATTENTION`; giữ nguyên khoản cam kết hoàn |
| `force=true` | Cho phép thử lại yêu cầu cần kiểm tra hoặc chưa đến hạn; vẫn không cướp lease của lượt đang chạy |

`FAILED`, `UNKNOWN` và `NEEDS_ATTENTION` không giải phóng khoản cam kết. Nếu bank đã chuyển tiền mà hệ thống chưa biết, tạo một yêu cầu khác có thể hoàn trùng. Không tự đổi các trạng thái lịch sử `PENDING/PAID` sang trạng thái mới.

## Vì sao chọn cách này

- Dùng ba transaction ngắn và một worker nhỏ. Không giữ khóa DB lúc gọi bank, cũng không thêm framework workflow/saga.
- Ưu tiên chậm một lượt quét hơn là gửi hai khoản hoàn. Nếu tiến trình dừng, lease hết hạn cho phép tiếp tục bằng mã cũ.
- Chỉ thử lại tự động lỗi tạm thời/không rõ kết quả. Lỗi nghiệp vụ có kết quả cuối cần đối soát; đổi key để né lỗi có thể tạo một khoản hoàn khác.
- DB hiện có cả `refunds` và `refund_requests`. Payment có dữ liệu ở `refunds`, hoặc counter không khớp các refund đã thành công trong `refund_requests`, phải đối soát trước. Không tự gộp hai nguồn vì chưa chứng minh chúng là hai khoản độc lập hay cùng một khoản.
- Luồng này chỉ thực thi hoàn qua `BANK`. Yêu cầu lịch sử có đích `WALLET` chuyển sang cần kiểm tra, không gửi nhầm sang ngân hàng.
- Outbox lưu sự kiện cùng transaction; phần này không bổ sung bộ gửi email hoàn tiền. Email lỗi không được kích hoạt hoàn tiền lần nữa.

## Cập nhật DB và chạy

```powershell
.\cap-nhat-schema.cmd
.\mvnw.cmd spring-boot:run
```

`cap-nhat-schema.cmd` áp dụng V006 và V007, giữ nguyên dữ liệu hiện có. V007 thêm unique index cho `refund_requests.idempotency_key` và index quét yêu cầu đến hạn. Nếu DB khác đã có key trùng, migration sẽ dừng để đối soát thay vì xóa bản ghi.

Sau khi log xuất hiện `Started DatphongApplication`, mở `http://localhost:8080/swagger-ui/index.html`.

```powershell
$env:DATPHONG_REFUND_INTEGRATION='true'
$env:DATPHONG_SCHEMA_VALIDATE='true'
$env:DATPHONG_RBAC_INTEGRATION='true'
.\mvnw.cmd test
```

Kiểm thử hoàn tiền dùng PostgreSQL và Fake Bank thật trong schema `refund_test_<UUID>` riêng. Kiểm thử khởi động dùng server HTTP thật, truy cập Swagger và `/v3/api-docs`, đồng thời tắt job trong test để không xử lý dữ liệu demo ở schema public.

Kết quả ngày 23/10/2026:

| Lượt chạy | Kết quả |
|---|---|
| Toàn bộ Maven test | 129/129 đạt, gồm 21 ca hoàn tiền, 97 ca admin/user/auth, 6 ca schema, 4 ca snapshot, 1 ca khởi động |
| Kiểm tra cuối sau khi thêm điều kiện WALLET | 23/23 đạt: 22 ca hoàn tiền và 1 ca khởi động/Swagger |
| HTTP Swagger và /v3/api-docs | 200, không cần đăng nhập |
| Đối chiếu DB | Số dòng trong cả 78 bảng public giữ nguyên; hai index V007 tồn tại |

Các ca hoàn tiền bao gồm yêu cầu/worker chạy đồng thời, kết quả lặp, vượt số đã thu, timeout sau bank commit, rollback nội bộ sau bank commit, retry có giới hạn, khôi phục booking hết hạn và giữ nguyên lỗi booking không liên quan. Không có failure, error hoặc test bị bỏ qua trong hai lượt chạy trên.

Log: `target/refund-final-tests.log` và `target/refund-final-check.log`. Báo cáo JUnit: `target/surefire-reports/`.
