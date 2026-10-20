import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import org.mindrot.jbcrypt.BCrypt;
import org.postgresql.util.PGobject;

/** Explicit, opt-in fixtures for the 36 use cases. Never runs at application startup. */
public class DemoDataTool {
    static final String DATASET = "uc-demo-v1";
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Path AUDIT = Path.of("target/seed-audit");
    static final String CONFIG;
    static { try { CONFIG = Files.readString(Path.of("src/main/resources/application.yaml")); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    final Connection db;
    final Map<String, Map<String, String>> columns = new HashMap<>();
    final Instant now = Instant.now();
    final LocalDate today = LocalDate.now(ZONE);
    final Instant catalogTime = today.minusDays(160).atStartOfDay(ZONE).toInstant();
    final List<Account> accounts = new ArrayList<>();
    final List<Room> rooms = new ArrayList<>();
    final List<Room> bookable = new ArrayList<>();
    final Map<String, Long> roles = new LinkedHashMap<>();
    final Map<String, Long> templateVersions = new LinkedHashMap<>();
    final Map<String, byte[]> media = new LinkedHashMap<>();
    final Map<String, Long> counts = new TreeMap<>();
    long admin;

    record Account(String username, String password, String role, String name, String email, long id) {}
    record Room(long id, long unit, long type, long property, long host, int propertyIndex,
                int typeIndex, BigDecimal base, int capacity, String code, String image, long asset) {}
    record Json(Object value) {}
    record Range(LocalDate from, LocalDate to) {}
    static Json json(Object... pairs) { return new Json(map(pairs)); }
    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String)pairs[i], pairs[i + 1]);
        return result;
    }

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "verify" : args[0];
        if (args.length > 1 || !Set.of("seed", "verify").contains(command))
            throw new IllegalArgumentException("seed | verify");
        Files.createDirectories(AUDIT);
        Properties props = new Properties();
        props.setProperty("user", setting("SPRING_DATASOURCE_USERNAME"));
        props.setProperty("password", setting("SPRING_DATASOURCE_PASSWORD"));
        props.setProperty("connectTimeout", "10");
        props.setProperty("ApplicationName", "DatphongDemoData");
        try (Connection db = DriverManager.getConnection(setting("SPRING_DATASOURCE_URL"), props)) {
            db.setAutoCommit(false);
            try (Statement st = db.createStatement()) {
                st.execute("SET LOCAL search_path TO public");
                st.execute("SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh'");
                st.execute("SET LOCAL lock_timeout='10s'");
                st.execute("SET LOCAL statement_timeout='120s'");
                if (command.equals("seed")) st.execute("SELECT pg_advisory_xact_lock(571043533)");
            }
            DemoDataTool tool = new DemoDataTool(db);
            if (command.equals("verify")) {
                tool.verify();
                db.rollback();
                return;
            }
            try {
                if (tool.alreadySeeded()) {
                    System.out.println("Bộ uc-demo-v1 đã có; bỏ qua, không tạo trùng hoặc đổi lại mật khẩu/ngày.");
                    db.rollback();
                    return;
                }
                tool.preflight();
                tool.writeAccounts(); // Plaintext credentials exist on disk before any password is hashed.
                tool.backupBootstrapAdmin();
                tool.identity();
                tool.configuration();
                tool.catalog();
                tool.bookings();
                tool.extensions();
                tool.uploadMedia();
                tool.verify();
                tool.exec("CREATE SCHEMA IF NOT EXISTS datphong_schema");
                tool.exec("CREATE TABLE IF NOT EXISTS datphong_schema.seed_history (dataset_key text PRIMARY KEY, seeded_at timestamptz NOT NULL, business_date date NOT NULL, counts jsonb NOT NULL)");
                tool.exec("INSERT INTO datphong_schema.seed_history VALUES (?,?,?,?::jsonb)", DATASET, tool.now, tool.today, serialize(tool.counts));
                String report = tool.report();
                // Prepare the report before commit; move it into place only after DB success.
                Path pending = AUDIT.resolve("seed-result.pending.md");
                Files.writeString(pending, report, StandardCharsets.UTF_8);
                db.commit();
                try {
                    Files.move(pending, Path.of("docs/DU_LIEU_TEST.md"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    System.err.println("DB đã commit; báo cáo vẫn ở target/seed-audit/seed-result.pending.md.");
                }
                System.out.println("Đã commit 100 booking. Tài khoản: docs/TAI_KHOAN_TEST.md; báo cáo: docs/DU_LIEU_TEST.md.");
            } catch (Exception e) {
                db.rollback();
                System.err.println("Seed thất bại; toàn bộ thay đổi DB đã rollback. File fixture MinIO trong demo/uc-demo-v1 có thể được dùng lại ở lần chạy sau.");
                throw e;
            }
        }
    }

    DemoDataTool(Connection db) throws SQLException {
        this.db = db;
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT table_name,column_name,data_type FROM information_schema.columns WHERE table_schema='public'")) {
            while (rs.next()) columns.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(rs.getString(2), rs.getString(3));
        }
        for (String role : List.of("CUSTOMER", "HOST", "SUPPORT")) {
            for (int n = 1; n <= 3; n++) {
                String username = role.toLowerCase(Locale.ROOT) + "%02d".formatted(n);
                String title = role.substring(0, 1) + role.substring(1).toLowerCase(Locale.ROOT);
                accounts.add(new Account(username, title + "%02d@2026!".formatted(n), role,
                        switch (role) { case "CUSTOMER" -> List.of("Nguyễn Minh Anh", "Trần Hoàng Nam", "Lê Thu Hà").get(n-1);
                            case "HOST" -> List.of("Phạm Quang Huy", "Võ Ngọc Linh", "Đặng Hải Yến").get(n-1);
                            default -> List.of("Bùi Thanh Tâm", "Đỗ Minh Khoa", "Hoàng Mai Chi").get(n-1); },
                        username + "@demo.vivu.test", 0));
            }
        }
    }

    boolean alreadySeeded() throws Exception {
        if (scalar("SELECT count(*) FROM information_schema.tables WHERE table_schema='datphong_schema' AND table_name='seed_history'") == 0) return false;
        return scalar("SELECT count(*) FROM datphong_schema.seed_history WHERE dataset_key=?", DATASET) == 1;
    }

    void preflight() throws Exception {
        if (columns.size() != 74) throw new IllegalStateException("Cần schema 74 bảng đã khởi tạo bằng khoi-tao-db.cmd.");
        if (scalar("SELECT count(*) FROM bookings WHERE booking_code LIKE 'DEMO-%'") != 0)
            throw new IllegalStateException("Đã có booking DEMO- nhưng thiếu marker seed; cần kiểm tra dữ liệu trước.");
        for (Account a : accounts)
            if (scalar("SELECT count(*) FROM users WHERE username=? OR lower(email)=lower(?)", a.username, a.email) != 0)
                throw new IllegalStateException("Username/email fixture đã tồn tại: " + a.username);
        long adminCount = scalar("SELECT count(DISTINCT u.id) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE r.code='ROLE_ADMIN'");
        if (adminCount > 1) throw new IllegalStateException("DB đã có nhiều ADMIN; script không thay đổi quyền của tài khoản hiện có.");
        if (adminCount == 1 && scalar("SELECT count(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE r.code='ROLE_ADMIN' AND u.username='admin' AND u.email='admin@localhost'") != 1)
            throw new IllegalStateException("ADMIN hiện có không phải admin bootstrap; script không tự đặt lại mật khẩu tài khoản đó.");
        if (adminCount == 0 && scalar("SELECT count(*) FROM users WHERE username='admin' OR lower(email)='admin@localhost'") != 0)
            throw new IllegalStateException("Username/email admin đã được sử dụng bởi tài khoản khác.");
        if (scalar("SELECT count(*) FROM properties WHERE slug LIKE 'demo-%'") != 0)
            throw new IllegalStateException("Slug demo- đã tồn tại; không ghi đè property hiện có.");
    }

    void writeAccounts() throws Exception {
        StringBuilder text = new StringBuilder("# Tài khoản test\n\nBộ dữ liệu `uc-demo-v1`, ngày tạo: " + today + " (Asia/Ho_Chi_Minh).\n\n");
        text.append("Đăng nhập bằng **username**, gọi `POST /api/auth/login`. Tất cả 10 tài khoản đều `ACTIVE`, `active=true`, không bật 2FA.\n\n");
        text.append("| Role | Username | Email | Mật khẩu trước khi hash |\n|---|---|---|---|\n");
        text.append("| ADMIN | admin | admin@localhost | `Admin@2026!Demo` |\n");
        for (Account a : accounts) text.append("| ").append(a.role).append(" | ").append(a.username).append(" | ").append(a.email).append(" | `").append(a.password).append("` |\n");
        text.append("\nMật khẩu DB nằm ở `users.password_hash`, dùng BCrypt cost 12 và salt riêng từng tài khoản. File này được ghi **trước khi hash**.\n\n");
        text.append("`GUEST` là khách chưa đăng nhập, không tạo user/role riêng. Enum `USER` cũ không phải role nghiệp vụ trong đặc tả.\n\n");
        text.append("Admin bootstrap có sẵn được tái dùng và đổi mật khẩu đúng một lần khi seed. Bản ghi trước thay đổi được lưu tại `target/seed-audit/bootstrap-admin-before.json`; không commit file backup này. Chạy lại script không đổi mật khẩu.\n\n");
        text.append("```json\n{\"username\":\"customer01\",\"password\":\"Customer01@2026!\"}\n```\n\n");
        text.append("Đây là thông tin cho DB phát triển/test. Khi cần kiểm tra LOCKED/PENDING_VERIFICATION/IN_ACTIVE, dùng một tài khoản test rồi khôi phục trạng thái sau khi thử.\n");
        Files.writeString(Path.of("docs/TAI_KHOAN_TEST.md"), text, StandardCharsets.UTF_8);
    }

    void backupBootstrapAdmin() throws Exception {
        List<Map<String, Object>> rows = query("SELECT * FROM users WHERE username='admin'");
        if (!rows.isEmpty()) Files.writeString(AUDIT.resolve("bootstrap-admin-before.json"), serialize(rows), StandardCharsets.UTF_8);
        Files.writeString(AUDIT.resolve("before-counts.json"), serialize(query("SELECT relname,n_live_tup FROM pg_stat_user_tables WHERE schemaname='public' ORDER BY relname")), StandardCharsets.UTF_8);
    }

    void identity() throws Exception {
        for (String role : List.of("CUSTOMER", "HOST", "SUPPORT", "ADMIN")) {
            List<Map<String, Object>> existing = query("SELECT id FROM roles WHERE code=?", "ROLE_" + role);
            roles.put(role, existing.isEmpty() ? insert("roles", "code", "ROLE_" + role, "name", role,
                    "description", "Vai trò " + role + " theo đặc tả v3.0") : ((Number)existing.get(0).get("id")).longValue());
        }
        String hash = BCrypt.hashpw("Admin@2026!Demo", BCrypt.gensalt(12));
        if (scalar("SELECT count(*) FROM users WHERE username='admin'") == 1) {
            exec("UPDATE users SET password_hash=?,status='ACTIVE',active=true,two_factor_enabled=false,two_factor_secret=NULL,verified_at=COALESCE(verified_at,?),token_version=token_version+1,updated_at=?,version=version+1 WHERE username='admin'", hash, now, now);
            admin = scalar("SELECT id FROM users WHERE username='admin'");
        } else {
            admin = insert("users", "username", "admin", "email", "admin@localhost", "full_name", "Quản trị test",
                    "password_hash", hash, "status", "ACTIVE", "active", true, "two_factor_enabled", false, "token_version", 0L, "verified_at", catalogTime);
            insert("user_roles", "user_id", admin, "role_id", roles.get("ADMIN"), "assigned_at", catalogTime);
        }
        for (int n = 0; n < accounts.size(); n++) {
            Account a = accounts.get(n);
            long id = insert("users", "username", a.username, "email", a.email, "full_name", a.name,
                    "phone", "09080000%02d".formatted(n+1), "password_hash", BCrypt.hashpw(a.password, BCrypt.gensalt(12)),
                    "status", "ACTIVE", "active", true, "two_factor_enabled", false, "token_version", 0L, "verified_at", catalogTime);
            accounts.set(n, new Account(a.username, a.password, a.role, a.name, a.email, id));
            insert("user_roles", "user_id", id, "role_id", roles.get(a.role), "assigned_at", catalogTime);
            if (a.role.equals("HOST")) insert("host_profiles", "user_id", id, "display_name", a.name,
                    "business_name", "Vivu Demo " + a.username, "bio", "Chủ cơ sở test " + a.username,
                    "host_status", "APPROVED", "is_active", true, "auto_booking_default", true);
            insert("verification_tokens", "user_id", id, "purpose", "EMAIL_VERIFICATION", "token_hash", sha("demo-used-verify:" + a.username),
                    "created_at", catalogTime.minusSeconds(600), "expires_at", catalogTime.plusSeconds(3600), "used_at", catalogTime, "attempts", 1);
        }
        Map<String, List<String>> grants = new LinkedHashMap<>();
        grants.put("CUSTOMER", List.of("PROFILE_SELF", "BOOKING_SELF", "PAYMENT_SELF", "REVIEW_SELF", "MESSAGE_SELF"));
        grants.put("HOST", List.of("PROFILE_SELF", "PROPERTY_OWN", "CATALOG_OWN", "BOOKING_OWN_PROPERTY", "STAY_OWN_PROPERTY", "REPORT_OWN", "MESSAGE_SELF"));
        grants.put("SUPPORT", List.of("SUPPORT_READ", "COMPLAINT_MANAGE", "REFUND_RECONCILE", "AUDIT_READ", "MAIL_OPERATE"));
        Set<String> all = new LinkedHashSet<>();
        grants.values().forEach(all::addAll);
        all.addAll(List.of("USER_MANAGE", "ROLE_MANAGE", "CONFIG_MANAGE", "TEMPLATE_MANAGE", "REPORT_ALL"));
        for (String code : all) {
            long permission = scalar("SELECT COALESCE(max(id),0) FROM permissions WHERE code=?", code);
            if (permission == 0) permission = insert("permissions", "code", code, "name", code, "description", "Quyền test " + code);
            for (String role : roles.keySet()) if (role.equals("ADMIN") || grants.getOrDefault(role, List.of()).contains(code))
                exec("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?) ON CONFLICT DO NOTHING", roles.get(role), permission);
        }
    }

    void configuration() throws Exception {
        Object[][] definitions = {
                {"auth.password.min_length", "INTEGER", "12", 8, 128, true},
                {"booking.hold_minutes", "INTEGER", "15", 1, 60, true},
                {"booking.tax_percent", "DECIMAL", "8", 0, 100, true},
                {"booking.service_fee_percent", "DECIMAL", "5", 0, 100, true},
                {"checkin.reminder_hours", "INTEGER", "24", 1, 168, true},
                {"mail.max_retries", "INTEGER", "3", 0, 10, false}
        };
        for (Object[] d : definitions) {
            insert("config_definitions", "config_key", d[0], "value_type", d[1], "description", "Cấu hình mẫu " + d[0],
                    "definition_version", 1, "min_value", d[3], "max_value", d[4], "publicly_readable", d[5],
                    "allowed_scopes", json("GLOBAL",true,"PROPERTY",true));
            long id = insert("config_values", "config_key", d[0], "value_type", d[1], "scope", "GLOBAL", "scope_id", null,
                    "value_text", d[2], "effective_at", catalogTime, "updated_by_id", admin, "policy_version", 1, "change_reason", "Khởi tạo dữ liệu test");
            insert("config_change_logs", "config_value_id", id, "new_value", d[2], "actor_id", admin,
                    "reason", "Khởi tạo dữ liệu test", "changed_at", catalogTime, "correlation_id", "demo-config:" + d[0]);
        }
        List<String> events = List.of("ACCOUNT_CREATED", "EMAIL_VERIFICATION", "BOOKING_CONFIRMED", "PAYMENT_SUCCEEDED", "PAYMENT_FAILED",
                "REFUND_SUCCEEDED", "REFUND_FAILED", "CHECKIN_REMINDER", "COMPLAINT_REPLY", "CHECKIN_CONFIRMED");
        for (String event : events) {
            long template = insert("email_templates", "template_code", event, "locale", "vi-VN", "active_version", null, "status", "DRAFT");
            long version = insert("email_template_versions", "template_id", template, "version_no", 1,
                    "subject", "[Vivu Test] " + event + " - {{reference}}",
                    "html_body", "<p>Xin chào {{fullName}},</p><p>Mã: <strong>{{reference}}</strong>.</p><p>{{message}}</p>",
                    "text_body", "Xin chào {{fullName}}. Mã: {{reference}}. {{message}}",
                    "allowed_variables", json("fullName","STRING","reference","STRING","message","STRING"),
                    "required_variables", json("fullName",true,"reference",true,"message",true), "published_at", catalogTime, "published_by_id", admin);
            templateVersions.put(event, version);
            exec("UPDATE email_templates SET active_version=1,status='ACTIVE' WHERE id=?",template);
        }
    }

    void catalog() throws Exception {
        List<Long> amenities = new ArrayList<>();
        String[][] names = {{"WIFI", "Wi-Fi", "wifi"}, {"AIR_CONDITIONING", "Điều hòa", "snowflake"}, {"PARKING", "Bãi đỗ xe", "car"},
                {"POOL", "Hồ bơi", "waves"}, {"BREAKFAST", "Bữa sáng", "coffee"}, {"KITCHEN", "Bếp", "cooking-pot"},
                {"BALCONY", "Ban công", "sun"}, {"SEA_VIEW", "Hướng biển", "sailboat"}, {"GYM", "Phòng tập", "dumbbell"},
                {"ELEVATOR", "Thang máy", "arrow-up-down"}, {"WASHER", "Máy giặt", "washing-machine"}, {"WORKSPACE", "Bàn làm việc", "laptop"}};
        for (String[] a : names) amenities.add(insert("amenities", "code", a[0], "name", a[1], "icon", a[2], "active", true));
        String[] cities = {"Hà Nội", "Đà Nẵng", "TP. Hồ Chí Minh", "Đà Lạt", "Nha Trang", "Phú Quốc", "Huế", "Hội An", "Vũng Tàu"};
        String[] titles = {"Vivu Hồ Tây", "Vivu Sông Hàn", "Vivu Sài Gòn", "Vivu Đồi Thông", "Vivu Biển Xanh", "Vivu Đảo Ngọc", "Vivu Cố Đô", "Vivu Phố Cổ", "Vivu Bãi Sau"};
        double[] lats = {21.07,16.07,10.78,11.94,12.24,10.28,16.46,15.88,10.34};
        double[] lons = {105.82,108.22,106.70,108.44,109.19,103.98,107.59,108.33,107.08};
        for (int p = 0; p < 9; p++) {
            long host = account("HOST", p < 6 ? p / 2 : p - 6).id;
            String status = p < 6 ? "PUBLISHED" : List.of("DRAFT", "PAUSED", "ARCHIVED").get(p-6);
            long property = insert("properties", "owner_id", host, "name", titles[p], "slug", "demo-property-" + (p+1),
                    "address", (10+p) + " đường Du Lịch, " + cities[p], "address_line", (10+p) + " đường Du Lịch, " + cities[p],
                    "city", cities[p], "country", "VN", "timezone", ZONE.getId(), "latitude", BigDecimal.valueOf(lats[p]), "longitude", BigDecimal.valueOf(lons[p]),
                    "description", "Cơ sở mẫu " + titles[p] + ", phục vụ kiểm thử tìm kiếm, giá, quyền chủ sở hữu và đặt phòng.",
                    "status", status, "checkin_local", LocalTime.of(14,0), "checkout_local", LocalTime.NOON,
                    "check_in_from", LocalTime.of(14,0), "check_out_until", LocalTime.NOON, "public_contact", "09090000%02d".formatted(p+1));
            Json policySnapshot = json("code", "DEMO-FLEX", "revision", 1, "timezone", ZONE.getId(), "rules",
                    List.of(map("daysBeforeCheckin", 7, "refundPercent", 100), map("daysBeforeCheckin", 2, "refundPercent", 50), map("daysBeforeCheckin", 0, "refundPercent", 0)));
            long policy = insert("cancellation_policies", "property_id", property, "host_id", host, "name", "Linh hoạt 7 ngày - " + titles[p],
                    "policy_json", policySnapshot, "effective_at", catalogTime, "active", true, "policy_revision", 1);
            exec("UPDATE properties SET cancellation_policy_id=? WHERE id=?", policy, property);
            for (int days : List.of(7,2,0)) insert("cancellation_policy_rules", "policy_id", policy, "days_before_checkin", days, "refund_percent", days==7 ? 100 : days==2 ? 50 : 0);
            for (int t=0; t<3; t++) {
                int capacity = t==2 ? 4 : 2;
                String typeCode = List.of("STD", "DLX", "FAM").get(t);
                BigDecimal base = BigDecimal.valueOf(450000L + p*70000L + t*300000L);
                long type = insert("room_types", "property_id", property, "code", typeCode, "name", List.of("Tiêu chuẩn", "Cao cấp", "Gia đình").get(t),
                        "bed_type", t==2 ? "2_DOUBLE" : "DOUBLE", "max_guests", capacity, "max_adults", capacity, "max_children", t==2 ? 2 : 1,
                        "description", "Loại phòng " + typeCode + " tại " + titles[p], "active", true, "cancellation_policy_id", policy);
                for (int a=0; a<amenities.size(); a++) if (a<3 || (a+t+p)%3==0)
                    insert("room_type_amenities", "room_type_id", type, "amenity_id", amenities.get(a));
                for (int d=-150; d<=120; d++) {
                    LocalDate date = today.plusDays(d);
                    insert("room_rates", "room_type_id", type, "stay_date", date, "amount", rate(base,date), "currency", "VND",
                            "price_version", 1L, "closed_to_sale", d==95 && t==2);
                }
                for (int r=0; r<2; r++) {
                    String code = typeCode + "-" + (r+1);
                    String roomStatus = p==8 ? "INACTIVE" : r==1 && t==1 ? "MAINTENANCE" : r==1 && t==2 ? "OUT_OF_SERVICE" : "ACTIVE";
                    long unit = insert("room_units", "property_id", property, "room_type_id", type, "code", code, "floor", ""+(t+1), "status", roomStatus);
                    String imageKey = "demo/" + DATASET + "/property-"+(p+1)+"/"+code+".png";
                    byte[] image = fixturePng(p,t,code);
                    media.put(imageKey, image);
                    String imageUrl = setting("MINIO_ENDPOINT") + "/" + setting("MINIO_BUCKET") + "/" + imageKey;
                    long room = insert("rooms", "property_id", property, "room_type_id", type, "room_unit_id", unit, "room_code", code,
                            "name", titles[p] + " " + code, "floor", ""+(t+1), "type", t==0 ? "DOUBLE" : t==1 ? "DELUXE" : "FAMILY",
                            "capacity", capacity, "status", roomStatus, "is_active", roomStatus.equals("ACTIVE"), "base_price", base,
                            "price_per_night", base.longValueExact(), "description", "Phòng " + code + ", dữ liệu mẫu " + status, "image_url", imageUrl);
                    long asset = insert("media_assets", "property_id", property, "room_id", room, "created_by_id", host,
                            "object_key", imageKey, "content_type", "image/png", "size_bytes", (long)image.length, "checksum_sha256", sha(image),
                            "status", "READY", "finalized_at", catalogTime.plusSeconds(60), "upload_expires_at", catalogTime.plusSeconds(600), "sort_order", 0);
                    insert("room_images", "room_id", room, "asset_id", asset, "media_type", "IMAGE", "sort_order", 0, "cover", true, "url", imageUrl);
                    for (int a=0; a<3; a++) insert("room_amenities", "room_id", room, "amenity_id", amenities.get(a));
                    Room value = new Room(room,unit,type,property,host,p,t,base,capacity,code,imageUrl,asset);
                    rooms.add(value);
                    if (p<6 && roomStatus.equals("ACTIVE")) bookable.add(value);
                    if (roomStatus.equals("MAINTENANCE") || roomStatus.equals("OUT_OF_SERVICE"))
                        insert("room_sale_blocks", "room_id", room, "date_from", today.minusDays(2), "date_to", today.plusDays(10), "reason", "Bảo trì định kỳ " + code, "created_by_id", host);
                }
            }
            insert("analytics_refresh_state", "property_id", property, "data_version", 1L, "refreshed_at", now, "source_cutoff_at", now);
        }
        Room sample = bookable.get(0);
        for (String state : List.of("PENDING", "PENDING_EXPIRED", "FAILED", "DELETED"))
            insert("media_assets", "property_id", sample.property, "created_by_id", sample.host, "object_key", "demo/"+DATASET+"/upload-"+state.toLowerCase(Locale.ROOT)+".png",
                    "content_type", "image/png", "size_bytes", 1024L, "status", state,
                    "upload_expires_at", state.equals("PENDING") ? now.plusSeconds(600) : now.minusSeconds(3600), "sort_order", 0,
                    "deleted_at", state.equals("DELETED") ? now.minusSeconds(300) : null);
        insert("room_sale_blocks", "room_id", sample.id, "date_from", today.plusDays(90), "date_to", today.plusDays(93), "reason", "Bảo trì tương lai", "created_by_id", sample.host);
        insert("room_sale_blocks", "room_id", sample.id, "date_from", today.plusDays(80), "date_to", today.plusDays(82), "reason", "Lịch bảo trì đã hủy", "created_by_id", sample.host,
                "cancelled_at", now.minusSeconds(3600), "cancelled_by_id", sample.host);
    }

    void bookings() throws Exception {
        List<String> statuses = new ArrayList<>();
        Object[][] distribution = {{"COMPLETED",30},{"CONFIRMED",18},{"CHECKED_IN",10},{"CANCELLED",10},
                {"REFUNDED",8},{"REFUND_PENDING",5},{"HOLD",5},{"PENDING_PAYMENT",3},{"EXPIRED",4},
                {"NEEDS_ATTENTION",3},{"NO_SHOW",2},{"CANCELLATION_PENDING",2}};
        for (Object[] group : distribution) for (int n=0; n<(int)group[1]; n++) statuses.add((String)group[0]);
        for (int i=1; i<=100; i++) {
            String status = statuses.get(i-1);
            String code = "DEMO-%03d".formatted(i);
            String correlation = "demo-booking:" + code;
            Room room = bookable.get((i-1)%bookable.size());
            Account customer = account("CUSTOMER", (i-1)%3);
            LocalDate in = switch (status) {
                case "COMPLETED" -> today.minusDays(120).plusDays(((i-1)/24)*10L);
                case "CONFIRMED" -> today.plusDays(5+i%7);
                case "CHECKED_IN" -> today.minusDays(1);
                case "NO_SHOW" -> today.minusDays(10);
                default -> today.plusDays(30+(i/24)*10L);
            };
            if (Set.of("REFUNDED","REFUND_PENDING").contains(status) && i%2==0) in = today.plusDays(4);
            int nights = status.equals("CHECKED_IN") ? 3+i%2 : 1+i%4;
            LocalDate out = in.plusDays(nights);
            Instant checkin = in.atTime(14,0).atZone(ZONE).toInstant();
            Instant checkout = out.atTime(12,0).atZone(ZONE).toInstant();
            boolean hold = status.equals("HOLD") || status.equals("PENDING_PAYMENT");
            boolean overdue = i==85 || i==86 || i==89 || status.equals("EXPIRED");
            Instant created = in.isBefore(today) ? checkin.minus(Duration.ofDays(20)) : now.minus(Duration.ofDays(2)).plusSeconds(i);
            if (hold || status.equals("EXPIRED")) created = now.minusSeconds(overdue ? 3600 : 300);
            Instant expires = created.plusSeconds(900);
            Instant captured = created.plusSeconds(300);
            Instant cancelled = now.minusSeconds(86400-i);
            boolean refund = status.equals("REFUNDED") || status.equals("REFUND_PENDING");
            boolean cancellation = refund || status.equals("CANCELLED");
            BigDecimal subtotal = BigDecimal.ZERO, tax = BigDecimal.ZERO, fees = BigDecimal.ZERO;
            for (LocalDate d=in; d.isBefore(out); d=d.plusDays(1)) {
                BigDecimal rate = rate(room.base,d);
                subtotal = subtotal.add(rate);
                tax = tax.add(percent(rate,8));
                fees = fees.add(percent(rate,5));
            }
            BigDecimal total = subtotal.add(tax).add(fees);
            BigDecimal fee = refund && i%2==0 ? total.divide(BigDecimal.valueOf(2),2,RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal refundable = refund ? total.subtract(fee) : BigDecimal.ZERO;
            String source = List.of("WEB","MOBILE","ADMIN_ASSISTED").get(i%3);
            String segment = i<=3 ? "NEW" : i%10==0 ? "UNKNOWN" : "RETURNING";
            Json snapshot = json("policyCode", "DEMO-FLEX", "revision",1, "timezone",ZONE.getId(), "checkinLocal","14:00",
                    "rules", List.of(map("daysBeforeCheckin",7,"refundPercent",100),map("daysBeforeCheckin",2,"refundPercent",50),map("daysBeforeCheckin",0,"refundPercent",0)),
                    "currency","VND","taxPercent",8,"serviceFeePercent",5);
            boolean paid = Set.of("COMPLETED","CONFIRMED","CHECKED_IN","REFUNDED","REFUND_PENDING","NO_SHOW","CANCELLATION_PENDING").contains(status);
            long booking = insert("bookings", "booking_code",code, "customer_id",customer.id, "property_id",room.property,
                    "room_type_id",room.type,"room_unit_id",room.unit,"room_id",room.id,"check_in",in,"check_out",out,
                    "guest_count",1+i%room.capacity,"status",status,"expires_at",expires,"subtotal_amount",subtotal,"tax_amount",tax,
                    "fee_amount",fees,"total_amount",total,"currency","VND","cancellation_policy_snapshot",snapshot,
                    "quote_version",1L,"price_version",1L,"idempotency_key","demo-hold:"+i,
                    "property_timezone_snapshot",ZONE.getId(),"checkin_local_snapshot",LocalTime.of(14,0),
                    "source_channel",source,"guest_segment_snapshot",segment,"created_at",created,"updated_at",now,
                    "confirmed_at",paid ? captured.plusSeconds(1) : null,
                    "actual_check_in",Set.of("COMPLETED","CHECKED_IN").contains(status) ? checkin.plusSeconds(1800) : null,
                    "actual_check_out",status.equals("COMPLETED") ? checkout.minusSeconds(1800) : null,
                    "checked_in_by_id",Set.of("COMPLETED","CHECKED_IN").contains(status) ? room.host : null,
                    "checked_out_by_id",status.equals("COMPLETED") ? room.host : null,
                    "checkin_verification_method",Set.of("COMPLETED","CHECKED_IN").contains(status) ? "BOOKING_CODE_AND_ID" : null,
                    "cancelled_at",cancellation ? cancelled : null,"cancelled_by",cancellation ? customer.id : null,
                    "cancel_reason",cancellation ? "Khách thay đổi lịch trình (dữ liệu test)" : null,
                    "cancellation_fee",fee,"refundable_amount",refundable,
                    "special_request",i%5==0 ? "Ưu tiên phòng yên tĩnh, không hút thuốc" : "Nhận phòng theo giờ tiêu chuẩn");
            for (LocalDate d=in; d.isBefore(out); d=d.plusDays(1)) {
                BigDecimal nightly = rate(room.base,d);
                insert("booking_nights","booking_id",booking,"stay_date",d,"nightly_rate",nightly,"tax_amount",percent(nightly,8),
                        "fee_amount",percent(nightly,5),"currency","VND","price_version",1L,"created_at",created);
            }
            boolean released = Set.of("COMPLETED","CANCELLED","REFUNDED","REFUND_PENDING","EXPIRED","NO_SHOW").contains(status) || i==94;
            Instant releasedAt = status.equals("COMPLETED") || status.equals("NO_SHOW") ? checkout : cancellation ? cancelled : expires;
            insert("room_allocations","booking_id",booking,"room_id",room.id,"stay",new Range(in,out),
                    "status",released ? "RELEASED" : hold || status.equals("NEEDS_ATTENTION") ? "HELD" : "CONFIRMED",
                    "created_at",created,"updated_at",released ? releasedAt : now,
                    "released_at",released ? releasedAt : null,"release_reason",released ? status : null);

            long payment = 0;
            String paymentStatus = null;
            if (paid || status.equals("NEEDS_ATTENTION") || status.equals("PENDING_PAYMENT") || status.equals("EXPIRED") || (status.equals("CANCELLED") && i>63)) {
                paymentStatus = paid || i==94 ? "SUCCEEDED" : i==95 ? "UNKNOWN" : i==96 ? "RECONCILING" : status.equals("PENDING_PAYMENT") ? "INITIATED" : i%2==0 ? "FAILED" : "TIMED_OUT";
                boolean capturedOk = paymentStatus.equals("SUCCEEDED");
                boolean open = Set.of("INITIATED","UNKNOWN","RECONCILING").contains(paymentStatus);
                String providerRef = capturedOk || !open ? "DEMO-BANK-PAY-%03d".formatted(i) : null;
                payment = insert("payments","booking_id",booking,"idempotency_key","demo-pay:"+i,"provider","FAKE_BANK",
                        "merchant_id","vivu-demo","method","FAKE_BANK_DEBIT","provider_reference",providerRef,"status",paymentStatus,
                        "amount",total,"captured_amount",capturedOk ? total : BigDecimal.ZERO,"currency","VND",
                        "request_hash",sha(code+":"+total),"correlation_id",correlation,"intent_expires_at",expires,"is_open",open,
                        "captured_at",capturedOk ? i==94 ? expires.plusSeconds(120) : captured : null,
                        "failure_code",paymentStatus.equals("FAILED") ? "INSUFFICIENT_FUNDS" : paymentStatus.equals("TIMED_OUT") ? "PROVIDER_TIMEOUT" : null,
                        "failure_message",paymentStatus.equals("FAILED") ? "Số dư mô phỏng không đủ" : paymentStatus.equals("TIMED_OUT") ? "Hết thời gian chờ ngân hàng" : null,
                        "created_at",created.plusSeconds(60),"updated_at",now);
                if (capturedOk || paymentStatus.equals("FAILED")) webhook(payment,0,i,"PAYMENT_"+(capturedOk ? "SUCCEEDED" : "FAILED"),providerRef,total,
                        i==94 ? "NEEDS_ATTENTION" : "PROCESSED", capturedOk ? i==94 ? expires.plusSeconds(120) : captured : created.plusSeconds(300));
            }
            long refundId = 0;
            if (refund || i==94) {
                String refundStatus = status.equals("REFUNDED") ? "SUCCEEDED" : i==94 ? "REQUESTED" : List.of("REQUESTED","PROCESSING","UNKNOWN","RECONCILING","FAILED").get(i-77);
                boolean reserved = !Set.of("SUCCEEDED","FAILED").contains(refundStatus);
                BigDecimal amount = i==94 ? total : refundable;
                String reference = refundStatus.equals("SUCCEEDED") ? "DEMO-BANK-REF-%03d".formatted(i) : null;
                refundId = insert("refunds","payment_id",payment,"booking_id",booking,"idempotency_key","demo-refund:"+i,
                        "refund_code","DEMO-REF-%03d".formatted(i),"provider","FAKE_BANK","merchant_id","vivu-demo","currency","VND",
                        "provider_reference",reference,"amount",amount,"status",refundStatus,"reason",i==94 ? "Bù trừ capture đến sau expiry" : "Hoàn tiền theo snapshot chính sách hủy",
                        "attempt_count",refundStatus.equals("REQUESTED") ? 0 : 1,"reservation_active",reserved,
                        "requested_by_id",account("SUPPORT",i%3).id,"request_hash",sha(code+":refund:"+amount),"correlation_id",correlation,
                        "processed_at",refundStatus.equals("SUCCEEDED") ? cancelled.plusSeconds(600) : null,
                        "next_attempt_at",reserved ? now.plusSeconds(3600) : null,"failure_code",refundStatus.equals("FAILED") ? "BANK_TEMPORARY_ERROR" : null,
                        "created_at",cancelled.plusSeconds(10),"updated_at",now);
                if (refundStatus.equals("SUCCEEDED")) webhook(payment,refundId,i,"REFUND_SUCCEEDED",reference,amount,"PROCESSED",cancelled.plusSeconds(600));
            }
            if (status.equals("NEEDS_ATTENTION") || (refund && Set.of(79,80).contains(i))) {
                insert("reconciliation_cases","booking_id",booking,"payment_id",payment,"refund_id",refundId==0 ? null : refundId,
                        "case_type",i==94 ? "LATE_CAPTURE" : refund ? "REFUND_UNKNOWN" : "PAYMENT_UNKNOWN","status",i%2==0 ? "PROCESSING" : "OPEN",
                        "attempt_count",1,"operation_key","demo-reconcile:"+i,"assigned_to_id",account("SUPPORT",i%3).id,
                        "detail",json("bookingCode",code,"scenario",i==94 ? "Capture sau khi giữ phòng hết hạn; không tự confirm" : "Cần hỏi lại kết quả từ Fake Bank"),
                        "next_attempt_at",now.plusSeconds(1800));
            }

            List<String> path = new ArrayList<>(List.of("HOLD"));
            if (payment!=0) path.add("PENDING_PAYMENT");
            if (paid) path.add("CONFIRMED");
            if (status.equals("COMPLETED")) { path.add("CHECKED_IN"); path.add("COMPLETED"); }
            else if (refund) { path.add("CANCELLED"); path.add("REFUND_PENDING"); if (status.equals("REFUNDED")) path.add("REFUNDED"); }
            else if (!path.get(path.size()-1).equals(status)) { if (i==94) path.add("EXPIRED"); path.add(status); }
            String previous = null;
            for (int step=0; step<path.size(); step++) {
                String next = path.get(step);
                Instant at = switch(next) {
                    case "HOLD" -> created;
                    case "PENDING_PAYMENT" -> created.plusSeconds(60);
                    case "CONFIRMED" -> captured.plusSeconds(1);
                    case "CHECKED_IN" -> checkin.plusSeconds(1800);
                    case "COMPLETED" -> checkout.minusSeconds(1800);
                    case "EXPIRED" -> expires;
                    case "CANCELLED" -> cancelled;
                    case "REFUND_PENDING" -> cancelled.plusSeconds(10);
                    case "REFUNDED" -> cancelled.plusSeconds(600);
                    case "NO_SHOW" -> checkout;
                    default -> i==94 ? expires.plusSeconds(121) : created.plusSeconds(600);
                };
                String eventSource = Set.of("CHECKED_IN","COMPLETED","NO_SHOW").contains(next) ? "HOST" : next.equals("EXPIRED") ? "SCHEDULER" : Set.of("CONFIRMED","REFUNDED").contains(next) ? "WEBHOOK" : "CUSTOMER";
                Long actor = eventSource.equals("HOST") ? Long.valueOf(room.host) : Set.of("SCHEDULER","WEBHOOK").contains(eventSource) ? null : Long.valueOf(customer.id);
                String actorType=actor==null ? next.equals("EXPIRED") ? "SYSTEM" : "PROVIDER" : "USER";
                UUID event = uuid(code+":status:"+step);
                insert("booking_status_history","booking_id",booking,"from_status",previous,"to_status",next,"actor_id",actor,
                        "actor_type",actorType,"source",eventSource,
                        "event_id",event,"correlation_id",correlation,"reason","Dữ liệu UC: " + next,"occurred_at",at,"created_at",at);
                insert("audit_logs","event_id",event,"actor_id",actor,"actor_type",actorType,
                        "action","BOOKING_STATUS_CHANGED","entity_type","BOOKING","entity_id",booking,"from_status",previous,"to_status",next,
                        "outcome","SUCCESS","request_id",correlation+":"+step,"correlation_id",correlation,"occurred_at",at,
                        "before_state",json("status",previous),"after_state",json("status",next),"reason","Fixture " + code,"created_at",at);
                previous = next;
            }
            insert("idempotency_records","scope","BOOKING_CREATE","actor_key","user:"+customer.id,"idempotency_key","demo-hold:"+i,
                    "request_hash",sha(code),"status","COMPLETED","resource_type","BOOKING","resource_id",booking,
                    "response_code",201,"response_body",json("bookingCode",code,"bookingId",booking),"completed_at",created.plusSeconds(1),
                    "expires_at",now.plus(Duration.ofDays(90)),"created_at",created);
            UUID event = uuid(code+":summary");
            insert("outbox_events","event_id",event,"event_type","BOOKING_"+status,"aggregate_type","BOOKING","aggregate_id",booking,
                    "payload",json("fixture",DATASET,"bookingId",booking,"bookingCode",code,"status",status),"status","PROCESSED",
                    "payload_version",1,"retry_count",0,"available_at",created,"processed_at",now,"created_at",created);
            for (long recipient : List.of(customer.id,room.host)) insert("notifications","event_id",event,"recipient_id",recipient,
                    "title",code+" - "+status,"content","Đơn đặt phòng " + code + " có trạng thái " + status,
                    "resource_type","BOOKING","resource_id",booking,"channel","IN_APP","event_type","BOOKING_"+status,"notif_type","BOOKING",
                    "is_read",i%3==0,"read_at",i%3==0 ? now.minusSeconds(60) : null,"created_at",created);
            if (paid || "FAILED".equals(paymentStatus)) {
                String mailType = status.equals("REFUNDED") ? "REFUND_SUCCEEDED" : paid ? "BOOKING_CONFIRMED" : "PAYMENT_FAILED";
                mail(customer,code,mailType,i,status.equals("REFUNDED") ? cancelled : captured);
            }
            List<String> funnel = new ArrayList<>(List.of("SEARCH_SHOWN","QUOTE_ISSUED","HOLD_CREATED"));
            if (payment!=0) funnel.add("PAYMENT_INITIATED");
            if (paid || i==94) funnel.add("PAYMENT_CAPTURED");
            if (paid) funnel.add("BOOKING_CONFIRMED");
            if (Set.of("COMPLETED","CHECKED_IN").contains(status)) funnel.add("CHECKIN_COMPLETED");
            for (int f=0; f<funnel.size(); f++) {
                String kind = funnel.get(f);
                Instant at = switch(kind) {
                    case "SEARCH_SHOWN" -> created.minusSeconds(120);
                    case "QUOTE_ISSUED" -> created.minusSeconds(30);
                    case "HOLD_CREATED" -> created;
                    case "PAYMENT_INITIATED" -> created.plusSeconds(60);
                    case "PAYMENT_CAPTURED" -> i==94 ? expires.plusSeconds(120) : captured;
                    case "BOOKING_CONFIRMED" -> captured.plusSeconds(1);
                    default -> checkin.plusSeconds(1800);
                };
                insert("user_behavior_logs","event_id",uuid(code+":funnel:"+kind),"event_type",kind,"action_type","HI",
                        "event_time",at,"ingested_at",at.plusSeconds(2),"actor_key","user:"+customer.id,"user_id",customer.id,
                        "session_key","demo-session:"+i,"request_id",correlation+":funnel:"+f,"correlation_id",correlation,
                        "booking_id",booking,"payment_id",payment==0 ? null : payment,"property_id",room.property,"room_id",room.id,"room_type_id",room.type,
                        "source_channel",source,"metadata",json("fixture",true,"status",status),"search_query",json("checkIn",in,"checkOut",out,"guests",2),"created_at",at);
            }
            if (status.equals("COMPLETED")) {
                String reviewStatus = i%10==0 ? "HIDDEN" : i%7==0 ? "PENDING_MODERATION" : "PUBLISHED";
                long review = insert("reviews","booking_id",booking,"customer_id",customer.id,"property_id",room.property,"room_id",room.id,
                        "rating",3+i%3,"content",List.of("Phòng sạch, nhận phòng nhanh và chủ nhà thân thiện.","Vị trí thuận tiện, giá theo đêm rõ ràng.","Không gian yên tĩnh, phù hợp nghỉ dưỡng.").get(i%3),
                        "status",reviewStatus,"host_reply",i%2==0 ? "Cảm ơn bạn đã lưu trú, hẹn gặp lại!" : null,
                        "host_replied_by_id",i%2==0 ? room.host : null,"host_replied_at",i%2==0 ? checkout.plusSeconds(7200) : null,
                        "moderated_by_id",reviewStatus.equals("HIDDEN") ? account("SUPPORT",i%3).id : null,
                        "moderated_at",reviewStatus.equals("HIDDEN") ? checkout.plusSeconds(10800) : null,
                        "moderation_reason",reviewStatus.equals("HIDDEN") ? "Ẩn mẫu để thử kiểm duyệt" : null,"created_at",checkout.plusSeconds(3600));
                if (i%3==0) insert("review_media","review_id",review,"media_type","IMAGE","url",room.image);
                insert("point_history","user_id",customer.id,"booking_id",booking,"points_change",total.divide(BigDecimal.valueOf(100000),0,RoundingMode.DOWN).intValue(),
                        "reason","Điểm cho lưu trú hoàn tất " + code,"created_at",checkout);
            }
            if (i<=12 || i>=31 && i<=36 || i>=49 && i<=58 || i>=77 && i<=81 || i>=94)
                conversation(i,booking,code,status,customer,room,in,out,created);
            if (i>=59 && i<=61) insert("support_tickets","booking_id",booking,"created_by",customer.id,"against_user_id",room.host,
                    "ticket_type","OTHER","status",i==59 ? "OPEN" : i==60 ? "INVESTIGATING" : "RESOLVED",
                    "description","Yêu cầu hướng dẫn hủy đơn " + code,"resolved_by",i==61 ? account("SUPPORT",0).id : null,
                    "resolution_note",i==61 ? "Đã giải thích chính sách" : null,"resolved_at",i==61 ? now.minusSeconds(600) : null,"created_at",cancelled.plusSeconds(600));
        }
        // Anonymous sessions stop before creating a hold: useful for funnel conversion denominators.
        for (int n=1; n<=20; n++) for (String event : n<=10 ? List.of("SEARCH_SHOWN","QUOTE_ISSUED") : List.of("SEARCH_SHOWN")) {
            Room room=bookable.get(n%bookable.size());
            insert("user_behavior_logs","event_id",uuid("guest:"+n+":"+event),"event_type",event,"action_type","HI",
                    "event_time",now.minusSeconds(n*3600L),"ingested_at",now.minusSeconds(n*3600L-2),"actor_key","guest:demo-"+n,
                    "session_key","demo-guest-session:"+n,"request_id","demo-guest:"+n+":"+event,"correlation_id","demo-guest:"+n,
                    "property_id",room.property,"room_type_id",room.type,"room_id",room.id,"source_channel",n%2==0 ? "WEB" : "MOBILE","metadata",json("fixture",true));
        }
    }

    void webhook(long payment, long refund, int index, String type, String reference, BigDecimal amount, String status, Instant at) throws Exception {
        Json payload = json("eventId","demo-bank:"+type+":"+index,"reference",reference,"amount",amount,"currency","VND","type",type,"fixture",true);
        insert("bank_webhook_events","event_id","demo-bank:"+type+":"+index,"provider","FAKE_BANK","merchant_id","vivu-demo",
                "provider_reference",reference,"payment_id",payment,"refund_id",refund==0 ? null : refund,"event_type",type,"payload",payload,
                "payload_hash",sha(serialize(payload.value)),"verified",true,"processing_status",status,"attempt_count",1,
                "provider_occurred_at",at,"received_at",at.plusSeconds(1),"processed_at",status.equals("PROCESSED") ? at.plusSeconds(2) : null,
                "last_error",status.equals("NEEDS_ATTENTION") ? "LATE_CAPTURE_AFTER_HOLD_EXPIRY" : null,"created_at",at.plusSeconds(1));
    }

    void mail(Account recipient, String reference, String event, int index, Instant created) throws Exception {
        String status = index%17==0 ? "DLQ" : index%13==0 ? "UNKNOWN" : index%11==0 ? "RETRY" : "SENT";
        long mail = insert("mail_events","event_id",uuid(reference+":mail:"+event),"business_key","demo-mail:"+reference+":"+event,
                "event_type",event,"template_version_id",templateVersions.get(event),"recipient_id",recipient.id,
                "recipient_identity",recipient.email,"recipient_address",recipient.email,"recipient_masked",recipient.username.substring(0,2)+"***@demo.vivu.test",
                "payload_snapshot",json("fullName",recipient.name,"reference",reference,"message","Dữ liệu kiểm thử " + event),
                "status",status,"retry_count",status.equals("DLQ") ? 3 : status.equals("SENT") ? 0 : 1,"due_at",created.plusSeconds(600),
                "sent_at",status.equals("SENT") ? created.plusSeconds(610) : null,"expires_at",now.plus(Duration.ofDays(30)),
                "correlation_id","demo-mail:"+reference,"last_error_code",status.equals("SENT") ? null : "SIMULATED_SMTP_TIMEOUT",
                "dead_letter_reason",status.equals("DLQ") ? "Đã hết số lần gửi thử (fixture, không gửi email thật)" : null,"created_at",created.plusSeconds(600));
        int attempts = status.equals("DLQ") ? 3 : 1;
        for (int n=1; n<=attempts; n++) insert("mail_delivery_attempts","mail_event_id",mail,"attempt_no",n,
                "provider_message_id",status.equals("SENT") ? "demo-smtp:"+reference : null,
                "result",status.equals("SENT") ? "SUCCEEDED" : status.equals("UNKNOWN") ? "UNKNOWN" : "TEMPORARY_FAILURE",
                "error_code",status.equals("SENT") ? null : "SIMULATED_TIMEOUT","attempted_at",created.plusSeconds(600+n*5L),
                "completed_at",created.plusSeconds(601+n*5L),"created_at",created.plusSeconds(600+n*5L));
    }

    void conversation(int i,long booking,String code,String bookingStatus,Account customer,Room room,LocalDate in,LocalDate out,Instant created) throws Exception {
        boolean complaint = i<=4 || i>=49 && i<=52;
        boolean checkin = !complaint && (i<=6 || i>=31 && i<=36 || i>=49 && i<=58);
        boolean closed = bookingStatus.equals("COMPLETED") && !complaint;
        Instant at = complaint ? in.atTime(15,0).atZone(ZONE).toInstant() : created.plusSeconds(3600);
        long conversation = insert("conversations","booking_id",booking,"property_id",room.property,"room_id",room.id,
                "customer_id",customer.id,"host_id",room.host,"type","BOOKING_SUPPORT","status",closed ? "CLOSED" : "OPEN",
                "last_message_seq",3L,"closed_at",closed ? out.atTime(13,0).atZone(ZONE).toInstant() : null,"created_at",at);
        for (long user : List.of(customer.id,room.host)) insert("conversation_participants","conversation_id",conversation,"user_id",user,
                "participant_role",user==customer.id ? "CUSTOMER" : "HOST","joined_at",at);
        String purpose = complaint ? "COMPLAINT" : checkin ? "CHECKIN_REQUEST" : "NORMAL";
        for (int seq=1; seq<=3; seq++) {
            String body = seq==1 ? "Xin chào, tôi muốn trao đổi về đơn " + code : seq==2 ? "Chào bạn, cơ sở đã nhận được tin nhắn." : complaint ? "Tôi cần hỗ trợ về điều hòa trong phòng." : checkin ? "Tôi dự kiến nhận phòng lúc 15 giờ, nhờ chủ nhà xác nhận." : "Cảm ơn chủ nhà đã hỗ trợ.";
            long message = insert("messages","conversation_id",conversation,"sender_id",seq==2 ? room.host : customer.id,
                    "client_message_id","demo-msg:"+code+":"+seq,"seq",(long)seq,"body",body,"content_type","TEXT",
                    "purpose",seq==3 ? purpose : "NORMAL","status","SENT","correlation_id","demo-chat:"+code,"request_hash",sha(body),"created_at",at.plusSeconds(seq*60L));
            if (seq==3 && complaint) {
                long support = account("SUPPORT",i%3).id;
                String complaintStatus = List.of("OPEN","ACKNOWLEDGED","IN_PROGRESS","RESOLVED","CLOSED").get(i%5);
                boolean resolved = Set.of("RESOLVED","CLOSED").contains(complaintStatus);
                insert("conversation_participants","conversation_id",conversation,"user_id",support,"participant_role","SUPPORT","joined_at",at.plusSeconds(300),"assigned_by_id",admin);
                insert("complaint_tickets","conversation_id",conversation,"booking_id",booking,"origin_message_id",message,
                        "created_by_id",customer.id,"assigned_support_id",support,"status",complaintStatus,"issue_type","ROOM_FACILITY",
                        "severity",i%2==0 ? "HIGH" : "NORMAL","reason","Điều hòa cần được kiểm tra","response_due_at",at.plusSeconds(7200),
                        "acknowledged_at",complaintStatus.equals("OPEN") ? null : at.plusSeconds(600),"resolved_at",resolved ? at.plusSeconds(1200) : null,
                        "resolved_by_id",resolved ? support : null,"resolution",resolved ? "Đã kiểm tra và xử lý thiết bị" : null,
                        "customer_confirmed_at",complaintStatus.equals("CLOSED") ? at.plusSeconds(1800) : null,
                        "closed_at",complaintStatus.equals("CLOSED") ? at.plusSeconds(1800) : null,"created_at",at.plusSeconds(180));
            }
            if (seq==3 && checkin) {
                String requestStatus = bookingStatus.equals("COMPLETED") ? "CLOSED" : bookingStatus.equals("CHECKED_IN") ? "APPROVED" : List.of("PENDING","COUNTER_PROPOSED","APPROVED","REJECTED","APPROVED","CANCELLED").get(i-31);
                Instant scheduled = in.atTime(15,0).atZone(ZONE).toInstant();
                long request = insert("checkin_requests","booking_id",booking,"conversation_id",conversation,"origin_message_id",message,
                        "requested_by_id",customer.id,"proposed_at",scheduled,"status",requestStatus,"note","Khách hẹn nhận phòng lúc 15 giờ",
                        "counter_proposed_at",requestStatus.equals("COUNTER_PROPOSED") ? scheduled.plusSeconds(3600) : null,
                        "agreed_checkin_at",Set.of("APPROVED","CLOSED").contains(requestStatus) ? scheduled : null,
                        "responded_by_id",requestStatus.equals("PENDING") ? null : room.host,"responded_at",requestStatus.equals("PENDING") ? null : at.plusSeconds(300),
                        "customer_accepted_at",Set.of("APPROVED","CLOSED").contains(requestStatus) ? at.plusSeconds(360) : null,
                        "closed_at",requestStatus.equals("CLOSED") ? scheduled.plusSeconds(1800) : null,"created_at",at.plusSeconds(180));
                Instant due = scheduled.minus(Duration.ofHours(24));
                String reminderStatus = bookingStatus.equals("COMPLETED") || bookingStatus.equals("CHECKED_IN") ? "SENT" : requestStatus.equals("CANCELLED") || requestStatus.equals("REJECTED") ? "CANCELLED" : "SCHEDULED";
                insert("scheduled_reminders","booking_id",booking,"checkin_request_id",request,"reminder_type","CHECKIN_REMINDER",
                        "window_code","H24","policy_version",1,"scheduled_checkin_at",scheduled,"due_at",due,"cutoff_at",scheduled,
                        "policy_snapshot",json("hoursBefore",24,"timezone",ZONE.getId(),"revision",1),"status",reminderStatus,
                        "sent_at",reminderStatus.equals("SENT") ? due.plusSeconds(60) : null,
                        "cancel_reason",reminderStatus.equals("CANCELLED") ? "Hẹn check-in không còn hiệu lực" : null,"created_at",at.plusSeconds(360));
            }
        }
        for (long user : List.of(customer.id,room.host)) insert("read_receipts","conversation_id",conversation,"user_id",user,
                "last_read_seq",user==customer.id ? 2L : i%2==0 ? 3L : 1L,"updated_at",at.plusSeconds(300));
    }

    void extensions() throws Exception {
        for (int n=0; n<3; n++) {
            Account host = account("HOST",n);
            insert("kyc_documents","user_id",host.id,"doc_type","DEMO_BUSINESS_LICENSE","file_url",rooms.get(n*12).image,"status","APPROVED","reviewed_by",admin);
            long pack = insert("host_vip_packages","name","Demo VIP " + (n+1),"price",BigDecimal.valueOf(199000L*(n+1)),"duration_days",30,
                    "benefits",json("testOnly",true,"featured",n>0,"prioritySupport",true));
            insert("host_vip_subscriptions","host_id",host.id,"package_id",pack,
                    "start_date",today.minusDays(n==0 ? 10 : 60),"end_date",today.plusDays(n==0 ? 20 : -30),
                    "status",List.of("ACTIVE","EXPIRED","CANCELLED").get(n),"payment_ref","DEMO-VIP-"+(n+1));
            insert("withdrawal_requests","host_id",host.id,"amount",BigDecimal.valueOf(200000),"requested_at",now.minusSeconds(3600),
                    "status",List.of("PENDING","APPROVED","REJECTED").get(n),"bank_name","FAKE_BANK","bank_account_no","DEMO-HOST-"+(n+1),
                    "bank_account_name",host.name,"processed_at",n==0 ? null : now.minusSeconds(600));
        }
        int rank=0;
        for (String name : List.of("MEMBER","SILVER","GOLD","PLATINUM","DIAMOND")) {
            insert("loyalty_ranks","name",name,"min_points",rank*1000,"discount_percent",rank*2,"benefits",json("testOnly",true,"priority",rank));
            rank++;
        }
        for (Account a : accounts) if (Set.of("CUSTOMER","HOST").contains(a.role)) {
            long wallet = insert("wallets","owner_id",a.id,"owner_type",a.role.equals("HOST") ? "HOST" : "USER","currency","VND","balance",BigDecimal.valueOf(4000000));
            insert("wallet_transactions","wallet_id",wallet,"tx_type","CREDIT","amount",BigDecimal.valueOf(5000000),"balance_after",BigDecimal.valueOf(5000000),
                    "reference_type","DEMO_OPENING_BALANCE","created_at",catalogTime);
            insert("wallet_transactions","wallet_id",wallet,"tx_type","DEBIT","amount",BigDecimal.valueOf(1000000),"balance_after",BigDecimal.valueOf(4000000),
                    "reference_type","DEMO_ADJUSTMENT","created_at",catalogTime.plusSeconds(3600));
        }
        for (int n=1; n<=6; n++) insert("vouchers","code","DEMO-SAVE-"+n,"discount_type",n%2==0 ? "FIXED_AMOUNT" : "PERCENT",
                "discount_value",BigDecimal.valueOf(n%2==0 ? 50000 : 10),"owner_type",n<=3 ? "SYSTEM" : "HOST",
                "owner_id",n<=3 ? null : account("HOST",n%3).id,"min_nights",2,"min_order_value",BigDecimal.valueOf(500000),
                "valid_from",n==6 ? now.plus(Duration.ofDays(7)) : now.minus(Duration.ofDays(30)),
                "valid_to",n==5 ? now.minus(Duration.ofDays(1)) : now.plus(Duration.ofDays(60)),"usage_limit_total",100,"usage_limit_per_user",2);
        for (int n=0; n<3; n++) {
            Account customer=account("CUSTOMER",n);
            long collection=insert("collections","user_id",customer.id,"name","Demo - Kỳ nghỉ của " + customer.name,"is_public",n%2==0,
                    "share_token",n%2==0 ? "demo-collection-"+n : null);
            for (int r=0; r<4; r++) {
                Room room=bookable.get(n*4+r);
                insert("collection_items","collection_id",collection,"room_id",room.id,"added_at",now.minusSeconds(3600));
                insert("ai_recommendations","user_id",customer.id,"room_id",room.id,"score",0.95-r*0.1,"model_version","DEMO-NOT-A-MODEL","generated_at",now);
                insert("dynamic_pricing_suggestions","room_id",room.id,"target_date",today.plusDays(14+r),"suggested_price",room.base.multiply(new BigDecimal("1.1")),
                        "reason","Gợi ý giả lập cho cuối tuần, không áp dụng vào giá hiện tại","model_version","DEMO-NOT-A-MODEL","applied",false,"created_at",now);
            }
            insert("refresh_tokens","user_id",customer.id,"token_hash",sha("demo-revoked-refresh:"+n),"family_id",uuid("refresh:"+n),
                    "device_label","Demo revoked device","created_at",now.minus(Duration.ofDays(1)),"expires_at",now.plus(Duration.ofDays(7)),"revoked_at",now.minusSeconds(3600));
            insert("verification_tokens","user_id",customer.id,"purpose","PASSWORD_RESET","token_hash",sha("demo-expired-reset:"+n),
                    "created_at",now.minusSeconds(7200),"expires_at",now.minusSeconds(3600),"attempts",0);
        }
        for (Map<String,Object> ticket : query("SELECT id,created_by FROM support_tickets WHERE description LIKE '%DEMO-%'"))
            insert("dispute_evidence","ticket_id",ticket.get("id"),"uploaded_by",ticket.get("created_by"),"evidence_type","IMAGE","url",bookable.get(0).image,"created_at",now);
        insert("currency_exchange_rates","base_currency","USD","target_currency","VND","rate",BigDecimal.valueOf(25000));
        insert("currency_exchange_rates","base_currency","EUR","target_currency","VND","rate",BigDecimal.valueOf(27000));
        String csv="booking_code,status,total_amount,currency\r\n";
        StringBuilder export = new StringBuilder(csv);
        for (Map<String,Object> row : query("SELECT booking_code,status,total_amount,currency FROM bookings WHERE booking_code LIKE 'DEMO-%' ORDER BY booking_code"))
            export.append(row.get("booking_code")).append(',').append(row.get("status")).append(',').append(row.get("total_amount")).append(",VND\r\n");
        String exportKey="demo/"+DATASET+"/exports/bookings.csv";
        media.put(exportKey,export.toString().getBytes(StandardCharsets.UTF_8));
        for (String state : List.of("QUEUED","COMPLETED","FAILED","EXPIRED"))
            insert("report_exports","requested_by_id",admin,"request_key","demo-export:"+state,"request_hash",sha(state+today),
                    "query_snapshot",json("from",today.minusDays(150),"to",today.plusDays(120),"fixturePrefix","DEMO-"),
                    "scope_snapshot",json("role","ADMIN","allProperties",true),"status",state,"requested_at",now.minusSeconds(3600),"created_at",now.minusSeconds(3600),
                    "as_of",now,"expires_at",state.equals("EXPIRED") ? now.minusSeconds(600) : now.plus(Duration.ofDays(7)),
                    "finished_at",state.equals("QUEUED") ? null : now.minusSeconds(1800),"row_count",state.equals("COMPLETED") ? 100L : null,
                    "object_key",state.equals("COMPLETED") || state.equals("EXPIRED") ? exportKey : null,
                    "failure_code",state.equals("FAILED") ? "SIMULATED_EXPORT_ERROR" : null);
        for (String state : List.of("RETRY","DLQ")) insert("outbox_events","event_id",uuid("outbox:"+state),"event_type","DEMO_OPERATIONAL_RETRY",
                "aggregate_type","SYSTEM","aggregate_id",0L,"payload",json("fixture",true,"state",state),"payload_version",1,
                "status",state,"retry_count",state.equals("DLQ") ? 5 : 1,"available_at",now.plusSeconds(3600),"last_error","SIMULATED_CONSUMER_FAILURE");
        insert("bank_webhook_events","event_id","demo-bank:bad-signature","provider","FAKE_BANK","merchant_id","vivu-demo",
                "provider_reference","DEMO-BAD-SIGNATURE","event_type","PAYMENT_FAILED","payload",json("fixture",true),"payload_hash",sha("invalid-signature-fixture"),
                "verified",false,"processing_status","REJECTED","attempt_count",1,"received_at",now,"last_error","INVALID_SIGNATURE");
    }

    void verify() throws Exception {
        check("100 booking",scalar("SELECT count(*) FROM bookings WHERE booking_code LIKE 'DEMO-%'")==100);
        check("1 ADMIN + 3 CUSTOMER/HOST/SUPPORT",scalar("SELECT count(*) FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.code='ROLE_ADMIN'")==1);
        List<Account> all = new ArrayList<>(accounts);
        all.add(new Account("admin","Admin@2026!Demo","ADMIN","","",admin));
        for (Account a : all) {
            List<Map<String,Object>> row=query("SELECT u.password_hash,u.status,u.active,r.code FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.username=?",a.username);
            check("login fixture "+a.username,row.size()==1 && row.get(0).get("code").equals("ROLE_"+a.role)
                    && row.get(0).get("status").equals("ACTIVE") && Boolean.TRUE.equals(row.get(0).get("active"))
                    && BCrypt.checkpw(a.password,row.get(0).get("password_hash").toString())
                    && row.get(0).get("password_hash").toString().startsWith("$2a$12$"));
        }
        String fixture=" b.booking_code LIKE 'DEMO-%' ";
        checkZero("snapshot tổng tiền và số đêm", "SELECT count(*) FROM bookings b LEFT JOIN (SELECT booking_id,count(*) nights,sum(nightly_rate) subtotal,sum(tax_amount) tax,sum(fee_amount) fee FROM booking_nights GROUP BY booking_id) n ON n.booking_id=b.id WHERE "+fixture+
                "AND (n.nights IS DISTINCT FROM b.check_out-b.check_in OR n.subtotal IS DISTINCT FROM b.subtotal_amount OR n.tax IS DISTINCT FROM b.tax_amount OR n.fee IS DISTINCT FROM b.fee_amount OR b.total_amount<>n.subtotal+n.tax+n.fee)");
        checkZero("ngày từng đêm", "SELECT count(*) FROM booking_nights n JOIN bookings b ON b.id=n.booking_id WHERE "+fixture+"AND (n.stay_date<b.check_in OR n.stay_date>=b.check_out)");
        checkZero("scope property/type/room", "SELECT count(*) FROM bookings b JOIN rooms r ON r.id=b.room_id JOIN room_types t ON t.id=b.room_type_id WHERE "+fixture+"AND (b.property_id<>r.property_id OR b.room_type_id<>r.room_type_id OR t.property_id<>b.property_id)");
        checkZero("không trùng tồn kho", "SELECT count(*) FROM room_allocations a JOIN room_allocations x ON a.id<x.id AND a.room_id=x.room_id AND a.stay&&x.stay WHERE a.released_at IS NULL AND x.released_at IS NULL");
        checkZero("range allocation", "SELECT count(*) FROM room_allocations a JOIN bookings b ON b.id=a.booking_id WHERE "+fixture+"AND (lower(a.stay)<>b.check_in OR upper(a.stay)<>b.check_out OR a.room_id<>b.room_id)");
        checkZero("allocation và trạng thái cuối", "SELECT count(*) FROM bookings b JOIN room_allocations a ON a.booking_id=b.id WHERE "+fixture+"AND ((b.status IN ('COMPLETED','CANCELLED','REFUNDED','REFUND_PENDING','EXPIRED','NO_SHOW') AND a.released_at IS NULL) OR (b.status IN ('CONFIRMED','CHECKED_IN','CANCELLATION_PENDING') AND a.status<>'CONFIRMED'))");
        checkZero("không bán phòng bảo trì/ẩn", "SELECT count(*) FROM bookings b JOIN rooms r ON r.id=b.room_id JOIN properties p ON p.id=b.property_id WHERE "+fixture+"AND (r.status NOT IN ('ACTIVE','OCCUPIED') OR p.status<>'PUBLISHED')");
        checkZero("không giữ chỗ trong lịch khóa", "SELECT count(*) FROM room_allocations a JOIN bookings b ON b.id=a.booking_id JOIN room_sale_blocks s ON s.room_id=a.room_id WHERE "+fixture+"AND a.released_at IS NULL AND s.cancelled_at IS NULL AND a.stay&&daterange(s.date_from,s.date_to,'[)')");
        checkZero("hoàn + giữ hạn mức <= captured", "SELECT count(*) FROM payments p JOIN (SELECT payment_id,sum(CASE WHEN status='SUCCEEDED' OR reservation_active THEN amount ELSE 0 END) amount FROM refunds GROUP BY payment_id) r ON r.payment_id=p.id JOIN bookings b ON b.id=p.booking_id WHERE "+fixture+"AND r.amount>p.captured_amount");
        checkZero("đánh giá sau hoàn tất", "SELECT count(*) FROM reviews r JOIN bookings b ON b.id=r.booking_id WHERE "+fixture+"AND (b.status<>'COMPLETED' OR r.customer_id<>b.customer_id OR r.property_id<>b.property_id OR r.created_at<b.actual_check_out)");
        checkZero("conversation đúng chủ/khách", "SELECT count(*) FROM conversations c JOIN bookings b ON b.id=c.booking_id JOIN properties p ON p.id=c.property_id WHERE "+fixture+"AND (c.customer_id<>b.customer_id OR c.host_id<>p.owner_id OR c.property_id<>b.property_id)");
        checkZero("history đúng thứ tự thời gian", "SELECT count(*) FROM (SELECT h.occurred_at,lag(h.occurred_at) OVER (PARTITION BY h.booking_id ORDER BY h.id) previous FROM booking_status_history h JOIN bookings b ON b.id=h.booking_id WHERE "+fixture+") x WHERE occurred_at<previous");
        checkZero("status khớp history cuối", "SELECT count(*) FROM bookings b JOIN LATERAL (SELECT to_status FROM booking_status_history WHERE booking_id=b.id ORDER BY id DESC LIMIT 1) h ON true WHERE "+fixture+"AND b.status<>h.to_status");
        for(var table:columns.entrySet()) for(var column:table.getValue().entrySet()) if(column.getValue().equals("jsonb"))
            checkZero("JSON object tương thích Map tại "+table.getKey()+"."+column.getKey(),"SELECT count(*) FROM "+table.getKey()+" WHERE "+column.getKey()+" IS NOT NULL AND jsonb_typeof("+column.getKey()+")<>'object'");
        Object seedTime=alreadySeeded() ? query("SELECT seeded_at FROM datphong_schema.seed_history WHERE dataset_key=?",DATASET).get(0).get("seeded_at") : now;
        check("2 HOLD + 1 PENDING_PAYMENT quá hạn lúc seed để test scheduler",scalar("SELECT count(*) FROM bookings b WHERE "+fixture+"AND b.status IN ('HOLD','PENDING_PAYMENT') AND b.expires_at<?",seedTime)==3);
        Files.writeString(AUDIT.resolve("verification.txt"),"PASS: credentials, 100 bookings, nightly snapshots, inventory, ownership, refunds, reviews, messaging, history.\nVerified at: "+Instant.now()+"\n",StandardCharsets.UTF_8);
        System.out.println("PASS: 10 tài khoản BCrypt; 100 booking; giá/đêm, tồn kho, scope, refund, review, hội thoại và history hợp lệ.");
    }

    String report() throws Exception {
        StringBuilder out = new StringBuilder("# Dữ liệu test UC\n\n");
        out.append("Đã seed vào DB hiện tại lúc ").append(now).append(". Ngày nghiệp vụ: **").append(today).append("**, múi giờ `Asia/Ho_Chi_Minh`.\n\n");
        out.append("Tài khoản và mật khẩu: [TAI_KHOAN_TEST.md](TAI_KHOAN_TEST.md).\n\n");
        out.append("## Chạy lại và kiểm tra\n\n```powershell\n.\\tao-du-lieu-test.cmd\n.\\tao-du-lieu-test.cmd -VerifyOnly\n```\n\n");
        out.append("Script đọc cấu hình `application.yaml` và các biến môi trường giống backend. Cần JDK 17+, PostgreSQL đã có 74 bảng, và MinIO đang chạy. Nếu DB mới, chạy `khoi-tao-db.cmd` trước. Không tự seed khi ứng dụng khởi động.\n\n");
        out.append("Lịch sử nằm tại `datphong_schema.seed_history`; chạy lại bỏ qua toàn bộ seed, giữ nguyên ngày và các chỉnh sửa test. Không xóa marker để seed chồng. Toàn bộ ghi DB nằm trong một transaction và được kiểm tra trước commit; lỗi rollback DB. Ảnh PNG và CSV fixture nằm trong prefix MinIO `demo/uc-demo-v1/`, có thể được ghi lại nếu lần trước rollback. Bucket giữ nguyên chính sách truy cập; ảnh ở bucket private cần URL ký qua API media.\n\n");
        out.append("## 100 booking theo trạng thái\n\n| Mã booking | Trạng thái | Số lượng |\n|---|---|---|\n");
        for (Map<String,Object> row:query("SELECT min(booking_code) first,max(booking_code) last,status,count(*) count FROM bookings WHERE booking_code LIKE 'DEMO-%' GROUP BY status ORDER BY min(booking_code)"))
            out.append("| ").append(row.get("first")).append(" - ").append(row.get("last")).append(" | ").append(row.get("status")).append(" | ").append(row.get("count")).append(" |\n");
        out.append("\n## Các ca có thể dùng ngay\n\n");
        out.append("- UC-01/02/03/20: 10 tài khoản ACTIVE, role/permission, 9 token xác minh đã dùng, 3 token reset hết hạn và 3 refresh token đã thu hồi. API login hiện dùng username.\n");
        out.append("- UC-04/05/06/07/08: 9 property (6 PUBLISHED, 1 DRAFT, 1 PAUSED, 1 ARCHIVED), 27 loại phòng, 54 phòng/room unit, 12 tiện ích, giá từ D-150 đến D+120 có chênh cuối tuần; phòng bảo trì/ngừng bán, block còn hiệu lực/đã hủy; 54 ảnh PNG thật trong MinIO, thêm 4 metadata upload PENDING/EXPIRED/FAILED/DELETED.\n");
        out.append("- UC-09/10/11: DEMO-082..084 HOLD còn hạn lúc seed; DEMO-085/086 HOLD quá hạn và DEMO-089 PENDING_PAYMENT quá hạn; DEMO-087/088 chờ thanh toán còn hạn; DEMO-090..093 EXPIRED và allocation đã giải phóng. Hold còn hạn sẽ tự quá hạn theo thời gian thực.\n");
        out.append("- UC-12/13/35/36: DEMO-069..076 hoàn thành hoàn tiền (đơn chẵn hoàn 50%, đơn lẻ hoàn 100% theo mốc hủy trong snapshot); DEMO-077..081 refund REQUESTED/PROCESSING/UNKNOWN/RECONCILING/FAILED. DEMO-094 capture trễ sau expiry, không CONFIRMED, đã giải phóng phòng và có yêu cầu bù trừ. DEMO-095/096 payment UNKNOWN/RECONCILING. Có webhook sai chữ ký bị REJECTED và ca đối soát. Không có schema Fake Bank độc lập trong repo; dữ liệu bank ở đây là payment/webhook/refund phía Booking, wallet không thay ledger bank.\n");
        out.append("- UC-14/15/16: DEMO-031..048 sắp nhận phòng, DEMO-049..058 đang ở, DEMO-001..030 hoàn tất có review PUBLISHED/PENDING_MODERATION/HIDDEN và phản hồi host; DEMO-097/098 NO_SHOW; DEMO-099/100 CANCELLATION_PENDING.\n");
        out.append("- UC-17/18/19/21..26: notification đã đọc/chưa đọc, outbox PROCESSED/RETRY/DLQ, audit và status history, nguồn WEB/MOBILE/ADMIN_ASSISTED, phân khúc khách, funnel gồm 20 phiên GUEST không tạo hold; snapshot từng đêm phục vụ ADR/RevPAR; export QUEUED/COMPLETED/FAILED/EXPIRED có CSV thật.\n");
        out.append("- UC-27/28/29: hội thoại có participant/read receipt/message, 8 complaint có người hỗ trợ, hẹn check-in nhiều trạng thái và reminder. Giờ hẹn và giờ check-in thực tế được lưu riêng.\n");
        out.append("- UC-30..34: 6 cấu hình có change log, 10 mẫu email có version, mail SENT/RETRY/UNKNOWN/DLQ và delivery attempt; không gửi email thật. Reminder có lịch tương lai/đã gửi/đã hủy.\n");
        out.append("- Mở rộng đang có trong schema: KYC, VIP, loyalty, ví có lịch sử cân bằng, voucher còn hạn/hết hạn/chưa đến hạn, collection, AI/pricing suggestion giả lập; tỷ giá chỉ là fixture test. Không tạo workflow trùng ở room_calendar/refund_requests/otp_verifications/system_settings. Voucher chưa áp dụng vào booking để giữ tổng tiền snapshot nhất quán.\n\n");
        out.append("Dữ liệu chuẩn bị đầu vào để triển khai/test UC; nhiều UC hiện chưa có service/API hoàn chỉnh (xem PROJECT_SPEC_AUDIT.md). Dữ liệu seed không tự bổ sung các API đó.\n\n");
        out.append("## Kiểm chứng\n\nĐã kiểm tra BCrypt, tổng giá/tax/fee từng đêm, khoảng [checkIn,checkOut), ownership property/type/room, không chồng allocation hoặc block, hạn mức refund/captured, review sau checkout, participant và chronology/history. `-VerifyOnly` kiểm tra fixture hiện tại; các thay đổi dữ liệu khi test có thể khiến một số điều kiện không còn đúng.\n\n");
        out.append("## Số bản ghi fixture đã thêm\n\nKhông bao gồm ADMIN/role có sẵn được tái dùng, hay role_permissions được gán qua ON CONFLICT.\n\n| Bảng | Số dòng thêm |\n|---|---:|\n");
        counts.forEach((table,count)->out.append("| ").append(table).append(" | ").append(count).append(" |\n"));
        return out.toString();
    }

    Account account(String role,int index) { return accounts.stream().filter(a->a.role.equals(role)).toList().get(index); }
    static BigDecimal rate(BigDecimal base,LocalDate date) {
        int day=date.getDayOfWeek().getValue();
        return base.multiply(day>=6 ? new BigDecimal("1.20") : BigDecimal.ONE).setScale(2,RoundingMode.HALF_UP);
    }
    static BigDecimal percent(BigDecimal amount,int percent) { return amount.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP); }
    static UUID uuid(String name) { return UUID.nameUUIDFromBytes((DATASET+":"+name).getBytes(StandardCharsets.UTF_8)); }
    static String setting(String name) {
        String env=System.getenv(name);
        if (env!=null && !env.isBlank()) return env;
        Matcher match=Pattern.compile(Pattern.quote(name)+":([^}]+)").matcher(CONFIG);
        if (!match.find()) throw new IllegalArgumentException("Thiếu cấu hình "+name);
        return match.group(1);
    }
    long insert(String table,Object... pairs) throws Exception {
        Map<String,Object> values=map(pairs);
        Map<String,String> schema=columns.get(table);
        if (schema==null) throw new IllegalArgumentException("Bảng không tồn tại: "+table);
        if (schema.containsKey("created_at")) values.putIfAbsent("created_at",catalogTime);
        if (schema.containsKey("updated_at")) values.putIfAbsent("updated_at",values.getOrDefault("created_at",catalogTime));
        if (schema.containsKey("version")) values.putIfAbsent("version",0L);
        for (String name:values.keySet()) if (!schema.containsKey(name)) throw new IllegalArgumentException("Cột không tồn tại: "+table+"."+name);
        String sql="INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        boolean returning=schema.containsKey("id");
        try (PreparedStatement st=db.prepareStatement(sql+(returning ? " RETURNING id" : ""))) {
            int index=1;
            for (var entry:values.entrySet()) bind(st,index++,entry.getValue(),schema.get(entry.getKey()));
            long id=0;
            if (returning) { try (ResultSet rs=st.executeQuery()) { rs.next(); id=rs.getLong(1); } }
            else st.executeUpdate();
            counts.merge(table,1L,Long::sum);
            return id;
        } catch (SQLException e) { throw new SQLException("Insert thất bại: "+table+"; SQLSTATE="+e.getSQLState()+"; "+e.getMessage(),e); }
    }
    void bind(PreparedStatement st,int index,Object value,String type) throws Exception {
        if (value instanceof Json json) { PGobject object=new PGobject(); object.setType("jsonb"); object.setValue(serialize(json.value)); st.setObject(index,object); }
        else if (value instanceof Range range) { PGobject object=new PGobject(); object.setType("daterange"); object.setValue("["+range.from+","+range.to+")"); st.setObject(index,object); }
        else if (value instanceof Instant instant) st.setObject(index,"timestamp without time zone".equals(type) ? LocalDateTime.ofInstant(instant,ZONE) : instant.atOffset(ZoneOffset.UTC));
        else st.setObject(index,value);
    }
    List<Map<String,Object>> query(String sql,Object... args) throws Exception {
        try (PreparedStatement st=db.prepareStatement(sql)) {
            for (int n=0;n<args.length;n++) bind(st,n+1,args[n],null);
            try (ResultSet rs=st.executeQuery()) {
                List<Map<String,Object>> rows=new ArrayList<>();
                ResultSetMetaData meta=rs.getMetaData();
                while (rs.next()) { Map<String,Object> row=new LinkedHashMap<>(); for(int n=1;n<=meta.getColumnCount();n++) row.put(meta.getColumnLabel(n),rs.getObject(n)); rows.add(row); }
                return rows;
            }
        }
    }
    long scalar(String sql,Object... args) throws Exception { return ((Number)query(sql,args).get(0).values().iterator().next()).longValue(); }
    void exec(String sql,Object... args) throws Exception {
        try (PreparedStatement st=db.prepareStatement(sql)) { for(int n=0;n<args.length;n++) bind(st,n+1,args[n],null); st.execute(); }
    }
    void checkZero(String label,String sql) throws Exception { check(label,scalar(sql)==0); }
    static void check(String label,boolean good) { if(!good) throw new IllegalStateException("Kiểm chứng thất bại: "+label); }
    static String serialize(Object value) {
        if (value==null) return "null";
        if (value instanceof Json json) return serialize(json.value);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?,?> map) { List<String> parts=new ArrayList<>(); map.forEach((k,v)->parts.add(serialize(k.toString())+":"+serialize(v))); return "{"+String.join(",",parts)+"}"; }
        if (value instanceof Iterable<?> list) { List<String> parts=new ArrayList<>(); for(Object item:list) parts.add(serialize(item)); return "["+String.join(",",parts)+"]"; }
        StringBuilder out=new StringBuilder("\"");
        for (char c:value.toString().toCharArray()) switch(c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if(c<32) out.append("\\u%04x".formatted((int)c)); else out.append(c); }
        }
        return out.append('"').toString();
    }

    static byte[] fixturePng(int property,int type,String code) throws Exception {
        BufferedImage image=new BufferedImage(640,400,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(new Color(225-property*5,235-type*12,242)); g.fillRect(0,0,640,400);
        g.setColor(new Color(35+property*9,80+type*15,110)); g.fillRect(0,280,640,120);
        g.setColor(new Color(250,247,236)); g.fillRoundRect(110,140,420,150,20,20);
        g.setColor(Color.WHITE); g.fillRoundRect(130,150,170,60,12,12); g.fillRoundRect(340,150,170,60,12,12);
        g.setColor(new Color(55,85,105)); g.setFont(new Font("SansSerif",Font.BOLD,30)); g.drawString("VIVU DEMO - P"+(property+1),35,55);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.PLAIN,24)); g.drawString("ROOM "+code+" | TEST FIXTURE",35,350);
        g.dispose();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes); return bytes.toByteArray();
    }
    void uploadMedia() throws Exception {
        String bucket=setting("MINIO_BUCKET");
        int head=s3("HEAD","/"+bucket,new byte[0],null);
        if (head==404) check("tạo bucket MinIO",s3("PUT","/"+bucket,new byte[0],null)==200);
        else check("MinIO bucket truy cập được (HTTP "+head+")",head==200);
        for(var item:media.entrySet()) check("upload fixture "+item.getKey(),s3("PUT","/"+bucket+"/"+item.getKey(),item.getValue(),item.getKey().endsWith(".png") ? "image/png" : "text/csv; charset=utf-8")==200);
        System.out.println("Đã tải "+media.size()+" fixture ảnh/CSV lên MinIO.");
    }
    static int s3(String method,String path,byte[] body,String contentType) throws Exception {
        String endpoint=setting("MINIO_ENDPOINT").replaceAll("/+$","");
        URI uri=URI.create(endpoint+path);
        String stamp=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
        String date=stamp.substring(0,8),region=setting("MINIO_REGION"),hash=sha(body);
        String signed="host;x-amz-content-sha256;x-amz-date";
        String headers="host:"+uri.getRawAuthority()+"\nx-amz-content-sha256:"+hash+"\nx-amz-date:"+stamp+"\n";
        String canonical=method+"\n"+uri.getRawPath()+"\n\n"+headers+"\n"+signed+"\n"+hash;
        String scope=date+"/"+region+"/s3/aws4_request";
        byte[] key=hmac(("AWS4"+setting("MINIO_SECRET_KEY")).getBytes(StandardCharsets.UTF_8),date);
        key=hmac(key,region); key=hmac(key,"s3"); key=hmac(key,"aws4_request");
        String signature=HexFormat.of().formatHex(hmac(key,"AWS4-HMAC-SHA256\n"+stamp+"\n"+scope+"\n"+sha(canonical)));
        HttpRequest.Builder request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                .header("x-amz-content-sha256",hash).header("x-amz-date",stamp)
                .header("Authorization","AWS4-HMAC-SHA256 Credential="+setting("MINIO_ACCESS_KEY")+"/"+scope+", SignedHeaders="+signed+", Signature="+signature);
        if(contentType!=null) request.header("Content-Type",contentType);
        request.method(method,body.length==0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(request.build(),HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    static String sha(String text) throws Exception { return sha(text.getBytes(StandardCharsets.UTF_8)); }
    static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    static byte[] hmac(byte[] key,String text) throws Exception { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256")); return mac.doFinal(text.getBytes(StandardCharsets.UTF_8)); }
}
