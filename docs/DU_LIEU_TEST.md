# Dữ liệu test UC

Đã seed vào DB hiện tại lúc 2026-10-20T10:28:17.054424300Z. Ngày nghiệp vụ: **2026-10-20**, múi giờ `Asia/Ho_Chi_Minh`.

Tài khoản và mật khẩu: [TAI_KHOAN_TEST.md](TAI_KHOAN_TEST.md).

## Chạy lại và kiểm tra

```powershell
.\tao-du-lieu-test.cmd
.\tao-du-lieu-test.cmd -VerifyOnly
```

Script đọc cấu hình `application.yaml` và các biến môi trường giống backend. Cần JDK 17+, PostgreSQL đã có 74 bảng, và MinIO đang chạy. Nếu DB mới, chạy `khoi-tao-db.cmd` trước. Không tự seed khi ứng dụng khởi động.

Lịch sử nằm tại `datphong_schema.seed_history`; chạy lại bỏ qua toàn bộ seed, giữ nguyên ngày và các chỉnh sửa test. Không xóa marker để seed chồng. Toàn bộ ghi DB nằm trong một transaction và được kiểm tra trước commit; lỗi rollback DB. Ảnh PNG và CSV fixture nằm trong prefix MinIO `demo/uc-demo-v1/`, có thể được ghi lại nếu lần trước rollback. Bucket giữ nguyên chính sách truy cập; ảnh ở bucket private cần URL ký qua API media.

## 100 booking theo trạng thái

| Mã booking | Trạng thái | Số lượng |
|---|---|---|
| DEMO-001 - DEMO-030 | COMPLETED | 30 |
| DEMO-031 - DEMO-048 | CONFIRMED | 18 |
| DEMO-049 - DEMO-058 | CHECKED_IN | 10 |
| DEMO-059 - DEMO-068 | CANCELLED | 10 |
| DEMO-069 - DEMO-076 | REFUNDED | 8 |
| DEMO-077 - DEMO-081 | REFUND_PENDING | 5 |
| DEMO-082 - DEMO-086 | HOLD | 5 |
| DEMO-087 - DEMO-089 | PENDING_PAYMENT | 3 |
| DEMO-090 - DEMO-093 | EXPIRED | 4 |
| DEMO-094 - DEMO-096 | NEEDS_ATTENTION | 3 |
| DEMO-097 - DEMO-098 | NO_SHOW | 2 |
| DEMO-099 - DEMO-100 | CANCELLATION_PENDING | 2 |

## Các ca có thể dùng ngay

- UC-01/02/03/20: 10 tài khoản ACTIVE, role/permission, 9 token xác minh đã dùng, 3 token reset hết hạn và 3 refresh token đã thu hồi. API login hiện dùng username.
- UC-04/05/06/07/08: 9 property (6 PUBLISHED, 1 DRAFT, 1 PAUSED, 1 ARCHIVED), 27 loại phòng, 54 phòng/room unit, 12 tiện ích, giá từ D-150 đến D+120 có chênh cuối tuần; phòng bảo trì/ngừng bán, block còn hiệu lực/đã hủy; 54 ảnh PNG thật trong MinIO, thêm 4 metadata upload PENDING/EXPIRED/FAILED/DELETED.
- UC-09/10/11: DEMO-082..084 HOLD còn hạn lúc seed; DEMO-085/086 HOLD quá hạn và DEMO-089 PENDING_PAYMENT quá hạn; DEMO-087/088 chờ thanh toán còn hạn; DEMO-090..093 EXPIRED và allocation đã giải phóng. Hold còn hạn sẽ tự quá hạn theo thời gian thực.
- UC-12/13/35/36: DEMO-069..076 hoàn thành hoàn tiền (đơn chẵn hoàn 50%, đơn lẻ hoàn 100% theo mốc hủy trong snapshot); DEMO-077..081 refund REQUESTED/PROCESSING/UNKNOWN/RECONCILING/FAILED. DEMO-094 capture trễ sau expiry, không CONFIRMED, đã giải phóng phòng và có yêu cầu bù trừ. DEMO-095/096 payment UNKNOWN/RECONCILING. Có webhook sai chữ ký bị REJECTED và ca đối soát. Không có schema Fake Bank độc lập trong repo; dữ liệu bank ở đây là payment/webhook/refund phía Booking, wallet không thay ledger bank.
- UC-14/15/16: DEMO-031..048 sắp nhận phòng, DEMO-049..058 đang ở, DEMO-001..030 hoàn tất có review PUBLISHED/PENDING_MODERATION/HIDDEN và phản hồi host; DEMO-097/098 NO_SHOW; DEMO-099/100 CANCELLATION_PENDING.
- UC-17/18/19/21..26: notification đã đọc/chưa đọc, outbox PROCESSED/RETRY/DLQ, audit và status history, nguồn WEB/MOBILE/ADMIN_ASSISTED, phân khúc khách, funnel gồm 20 phiên GUEST không tạo hold; snapshot từng đêm phục vụ ADR/RevPAR; export QUEUED/COMPLETED/FAILED/EXPIRED có CSV thật.
- UC-27/28/29: hội thoại có participant/read receipt/message, 8 complaint có người hỗ trợ, hẹn check-in nhiều trạng thái và reminder. Giờ hẹn và giờ check-in thực tế được lưu riêng.
- UC-30..34: 6 cấu hình có change log, 10 mẫu email có version, mail SENT/RETRY/UNKNOWN/DLQ và delivery attempt; không gửi email thật. Reminder có lịch tương lai/đã gửi/đã hủy.
- Mở rộng đang có trong schema: KYC, VIP, loyalty, ví có lịch sử cân bằng, voucher còn hạn/hết hạn/chưa đến hạn, collection, AI/pricing suggestion giả lập; tỷ giá chỉ là fixture test. Không tạo workflow trùng ở room_calendar/refund_requests/otp_verifications/system_settings. Voucher chưa áp dụng vào booking để giữ tổng tiền snapshot nhất quán.

Dữ liệu chuẩn bị đầu vào để triển khai/test UC; nhiều UC hiện chưa có service/API hoàn chỉnh (xem PROJECT_SPEC_AUDIT.md). Dữ liệu seed không tự bổ sung các API đó.

## Kiểm chứng

Đã kiểm tra BCrypt, tổng giá/tax/fee từng đêm, khoảng [checkIn,checkOut), ownership property/type/room, không chồng allocation hoặc block, hạn mức refund/captured, review sau checkout, participant và chronology/history. `-VerifyOnly` kiểm tra fixture hiện tại; các thay đổi dữ liệu khi test có thể khiến một số điều kiện không còn đúng.

Kết quả chạy thực tế ngày 20/10/2026 trên `vivu` tại `localhost:5435`:

- `mvnw.cmd -B -ntp test`: BUILD SUCCESS, 22 test, 0 failure/error, 3 test tích hợp được bỏ qua theo cấu hình mặc định.
- Bật `DATPHONG_SCHEMA_VALIDATE=true` và chạy `EntitySchemaTest#everyMappedTableAndColumnValidatesAgainstLocalDatabase`: PASS, Hibernate validate schema và đọc các entity ở đủ 74 bảng. Các trường JSON dùng object tương thích `Map<String, Object>`.
- Gọi `POST /api/auth/login` thực tế: 10/10 tài khoản trả HTTP 200, JWT có username/role đúng; mật khẩu sai trả HTTP 401. Server tạm phục vụ kiểm tra ở cổng 18080 đã được dừng.
- Chạy lại seed: số dòng và digest nội dung của tất cả bảng `public`/`datphong_schema`, cùng nội dung file tài khoản đều giữ nguyên.
- Kiểm tra độc lập: chuỗi trạng thái liên tục, số khách không vượt sức chứa, đủ từng đêm, refund nối đúng payment/booking, sequence hội thoại và sender là participant đều hợp lệ.

Log tại `target/seed-audit/`: `maven-tests.log`, `live-mapping-test.log`, `login-smoke.json`, `idempotency.txt`, `verification.txt`. Thư mục `target/` có thể bị xóa bởi Maven clean; tài liệu này lưu lại kết quả bàn giao.

## Số bản ghi fixture đã thêm

Không bao gồm ADMIN/role có sẵn được tái dùng, hay role_permissions được gán qua ON CONFLICT.

| Bảng | Số dòng thêm |
|---|---:|
| ai_recommendations | 12 |
| amenities | 12 |
| analytics_refresh_state | 9 |
| audit_logs | 391 |
| bank_webhook_events | 90 |
| booking_nights | 260 |
| booking_status_history | 391 |
| bookings | 100 |
| cancellation_policies | 9 |
| cancellation_policy_rules | 27 |
| checkin_requests | 14 |
| collection_items | 12 |
| collections | 3 |
| complaint_tickets | 8 |
| config_change_logs | 6 |
| config_definitions | 6 |
| config_values | 6 |
| conversation_participants | 88 |
| conversations | 40 |
| currency_exchange_rates | 2 |
| dispute_evidence | 3 |
| dynamic_pricing_suggestions | 12 |
| email_template_versions | 10 |
| email_templates | 10 |
| host_profiles | 3 |
| host_vip_packages | 3 |
| host_vip_subscriptions | 3 |
| idempotency_records | 100 |
| kyc_documents | 3 |
| loyalty_ranks | 5 |
| mail_delivery_attempts | 88 |
| mail_events | 80 |
| media_assets | 58 |
| messages | 120 |
| notifications | 200 |
| outbox_events | 102 |
| payments | 90 |
| permissions | 20 |
| point_history | 30 |
| properties | 9 |
| read_receipts | 80 |
| reconciliation_cases | 5 |
| refresh_tokens | 3 |
| refunds | 14 |
| report_exports | 4 |
| review_media | 10 |
| reviews | 30 |
| roles | 3 |
| room_allocations | 100 |
| room_amenities | 162 |
| room_images | 54 |
| room_rates | 7317 |
| room_sale_blocks | 18 |
| room_type_amenities | 162 |
| room_types | 27 |
| room_units | 54 |
| rooms | 54 |
| scheduled_reminders | 14 |
| support_tickets | 3 |
| user_behavior_logs | 611 |
| user_roles | 9 |
| users | 9 |
| verification_tokens | 12 |
| vouchers | 6 |
| wallet_transactions | 12 |
| wallets | 6 |
| withdrawal_requests | 3 |
