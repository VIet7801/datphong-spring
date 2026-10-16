import java.io.PrintStream;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

/** Công cụ khởi tạo schema; đọc thông tin kết nối từ môi trường/application.yaml, không ghi mật khẩu vào log. */
public class SchemaTool {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "inspect" : args[0];
        if (!(command.equals("inspect") && args.length <= 1)
                && !(command.equals("initialize") && args.length == 1)
                && !(command.equals("apply") && args.length == 2)) {
            throw new IllegalArgumentException("inspect | initialize | apply <sql-file>");
        }
        Map<String, String> migrations = command.equals("initialize") ? verifiedMigrations() : Map.of();
        String config = Files.readString(Path.of("src/main/resources/application.yaml"));
        Properties props = new Properties();
        props.setProperty("user", setting(config, "SPRING_DATASOURCE_USERNAME"));
        props.setProperty("password", setting(config, "SPRING_DATASOURCE_PASSWORD"));
        props.setProperty("connectTimeout", "10");
        props.setProperty("ApplicationName", "DatphongSchemaMigration");
        String url = setting(config, "SPRING_DATASOURCE_URL");
        try (Connection connection = connect(url, props, command.equals("initialize"))) {
            connection.setAutoCommit(false);
            if (command.equals("inspect")) {
                connection.setReadOnly(true);
                inspect(connection);
                connection.rollback();
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '10s'");
                statement.execute("SET LOCAL statement_timeout = '120s'");
                statement.execute("SELECT pg_advisory_xact_lock(571043532)");
            }
            if (command.equals("initialize")) {
                initialize(connection, migrations);
                return;
            }
            Map<String, Long> before = counts(connection);
            try (Statement statement = connection.createStatement()) {
                statement.execute(Files.readString(Path.of(args[1])));
            }
            Map<String, Long> after = counts(connection);
            unchangedRows(before, after);
            connection.commit();
            System.out.println("Đã áp dụng " + Path.of(args[1]).getFileName() + "; số bảng=" + after.size() + "; số dòng hiện có giữ nguyên; bảng mới không có dữ liệu.");
        }
    }

    static Connection connect(String url, Properties props, boolean createIfAbsent) throws Exception {
        try {
            return DriverManager.getConnection(url, props);
        } catch (SQLException missingDatabase) {
            // Chỉ tạo database khi máy chủ xác nhận database chưa tồn tại; không xử lý lỗi mật khẩu bằng cách tạo lại.
            if (!createIfAbsent || !"3D000".equals(missingDatabase.getSQLState())) throw missingDatabase;
            URI address = URI.create(url.substring("jdbc:".length()));
            String database = address.getPath().substring(1);
            if (database.isBlank()) throw new IllegalArgumentException("URL chưa chỉ định tên database.");
            int pathEnd = url.indexOf('?', "jdbc:postgresql://".length());
            String endpoint = pathEnd < 0 ? url : url.substring(0, pathEnd);
            String adminUrl = endpoint.substring(0, endpoint.lastIndexOf('/') + 1) + "postgres"
                    + (pathEnd < 0 ? "" : url.substring(pathEnd));
            try (Connection admin = DriverManager.getConnection(adminUrl, props); Statement statement = admin.createStatement()) {
                try {
                    statement.executeUpdate("CREATE DATABASE \"" + database.replace("\"", "\"\"") + "\"");
                    System.out.println("Đã tạo database " + database + ".");
                } catch (SQLException failure) {
                    if (!"42P04".equals(failure.getSQLState())) throw failure;
                }
            }
            return DriverManager.getConnection(url, props);
        }
    }

    static Map<String, String> verifiedMigrations() throws Exception {
        Path directory = Path.of("scripts/sql");
        Map<String, String> migrations = new LinkedHashMap<>();
        List<String> expected = List.of("V000__empty_database_baseline.sql", "V001__initialize_entity_catalog.sql", "V002__complete_catalog_constraints.sql");
        List<String> checksums = Files.readAllLines(directory.resolve("checksums.sha256"));
        for (String name : expected) {
            byte[] sql = Files.readAllBytes(directory.resolve(name));
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(sql));
            boolean matches = checksums.stream().map(line -> line.trim().split("\\s+", 2))
                    .anyMatch(parts -> parts.length == 2 && parts[1].equals(name) && parts[0].equalsIgnoreCase(digest));
            if (!matches) throw new IllegalStateException("Checksum không khớp: " + name + ". Hãy kiểm tra lại các tệp SQL đã pull.");
            migrations.put(name, new String(sql, java.nio.charset.StandardCharsets.UTF_8));
        }
        return migrations;
    }

    static void initialize(Connection connection, Map<String, String> migrations) throws Exception {
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL search_path TO public");
                statement.execute("SET LOCAL TIME ZONE 'UTC'");
            }
            // Lịch sử kỹ thuật nằm ngoài public, không làm tăng số bảng nghiệp vụ hay thêm dữ liệu nghiệp vụ.
            if (alreadyInitialized(connection, migrations)) {
                connection.rollback();
                System.out.println("DB đã được khởi tạo đúng phiên bản; không cần chạy lại SQL.");
                return;
            }
            Map<String, Long> before = counts(connection);
            for (var entry : before.entrySet()) {
                if (entry.getValue() != 0) throw new IllegalStateException("Bảng " + entry.getKey()
                        + " đang có dữ liệu. Script chỉ khởi tạo DB trống; dữ liệu hiện có được giữ nguyên.");
            }
            Path audit = Path.of("target/schema-audit");
            Files.createDirectories(audit);
            try (PrintStream output = new PrintStream(Files.newOutputStream(audit.resolve("before-initialization.txt")), false, java.nio.charset.StandardCharsets.UTF_8)) {
                inspect(connection, output);
            }
            String initial = before.isEmpty() ? "V000__empty_database_baseline.sql" : "V001__initialize_entity_catalog.sql";
            List<String> selected = List.of(initial, "V002__complete_catalog_constraints.sql");
            for (String name : selected) {
                System.out.println("Đang áp dụng " + name + "...");
                try (Statement statement = connection.createStatement()) {
                    statement.execute(migrations.get(name));
                }
            }
            Map<String, Long> after = counts(connection);
            unchangedRows(before, after);
            try (PrintStream output = new PrintStream(Files.newOutputStream(audit.resolve("after-initialization.txt")), false, java.nio.charset.StandardCharsets.UTF_8)) {
                inspect(connection, output);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS datphong_schema");
                statement.execute("CREATE TABLE datphong_schema.initialization_history ("
                        + "file_name text PRIMARY KEY, sha256 varchar(64) NOT NULL, applied_at timestamptz NOT NULL DEFAULT now())");
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO datphong_schema.initialization_history(file_name,sha256) VALUES (?,?)")) {
                for (String name : selected) {
                    statement.setString(1, name);
                    statement.setString(2, sqlChecksum(migrations.get(name)));
                    statement.executeUpdate();
                }
            }
            connection.commit();
            System.out.println("Đã khởi tạo " + after.size() + " bảng rỗng; không thêm dữ liệu nghiệp vụ.");
            try {
                StringBuilder applied = new StringBuilder();
                for (String name : selected) {
                    String digest = sqlChecksum(migrations.get(name));
                    applied.append(java.time.Instant.now()).append(' ').append(name).append(" SHA256=").append(digest).append('\n');
                }
                Files.writeString(audit.resolve("applied-versions.txt"), applied, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception auditFailure) {
                System.err.println("DB đã khởi tạo thành công nhưng không ghi được nhật ký phiên bản trong target/schema-audit.");
            }
        } catch (Exception failure) {
            connection.rollback();
            throw failure;
        }
    }

    static boolean alreadyInitialized(Connection connection, Map<String, String> migrations) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT to_regclass('datphong_schema.initialization_history') IS NOT NULL")) {
            result.next();
            if (!result.getBoolean(1)) return false;
        }
        Set<String> applied = new HashSet<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT file_name,sha256 FROM datphong_schema.initialization_history")) {
            while (result.next()) {
                String name = result.getString(1);
                if (!migrations.containsKey(name) || !sqlChecksum(migrations.get(name)).equals(result.getString(2))) {
                    throw new IllegalStateException("Phiên bản SQL đã thay đổi so với DB: " + name + ". Cần migration mới thay vì chạy lại khởi tạo.");
                }
                applied.add(name);
            }
        }
        if (applied.size() != 2 || !applied.contains("V002__complete_catalog_constraints.sql")
                || !(applied.contains("V000__empty_database_baseline.sql") || applied.contains("V001__initialize_entity_catalog.sql"))) {
            throw new IllegalStateException("Lịch sử khởi tạo chưa đầy đủ; cần kiểm tra trước khi tiếp tục.");
        }
        return true;
    }

    static String sqlChecksum(String sql) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    static void unchangedRows(Map<String, Long> before, Map<String, Long> after) {
        for (var entry : before.entrySet()) {
            if (!entry.getValue().equals(after.get(entry.getKey()))) throw new IllegalStateException("Số dòng thay đổi: " + entry.getKey());
        }
        for (var entry : after.entrySet()) {
            if (!before.containsKey(entry.getKey()) && entry.getValue() != 0) throw new IllegalStateException("Bảng mới đang có dữ liệu: " + entry.getKey());
        }
    }
    static String setting(String config, String key) {
        String override = System.getenv(key);
        if (override != null && !override.isBlank()) return override;
        Matcher matcher = Pattern.compile(Pattern.quote(key) + ":([^}]+)").matcher(config);
        if (!matcher.find()) throw new IllegalStateException("Thiếu cấu hình: " + key);
        return matcher.group(1);
    }
    static Map<String, Long> counts(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")) {
            while (rs.next()) names.add(rs.getString(1));
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String name : names) {
            try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT count(*) FROM public.\"" + name.replace("\"", "\"\"") + "\"")) {
                rs.next(); counts.put(name, rs.getLong(1));
            }
        }
        return counts;
    }
    static void inspect(Connection connection) throws SQLException {
        inspect(connection, System.out);
    }
    static void inspect(Connection connection, PrintStream output) throws SQLException {
        counts(connection).forEach((table, count) -> output.println("ROWS " + table + " " + count));
        String[] queries = {
            "SELECT table_name,column_name,udt_name,is_nullable,coalesce(column_default,'') FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position",
            "SELECT c.relname,con.conname,pg_get_constraintdef(con.oid) FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' ORDER BY c.relname,con.conname",
            "SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname"
        };
        for (int i=0;i<queries.length;i++) {
            try (Statement s=connection.createStatement();ResultSet rs=s.executeQuery(queries[i])) {
                while(rs.next()) {
                    List<String> fields=new ArrayList<>();
                    for(int c=1;c<=rs.getMetaData().getColumnCount();c++) fields.add(rs.getString(c));
                    output.println((i==0?"COLUMN ":i==1?"CONSTRAINT ":"INDEX ")+String.join(" | ",fields));
                }
            }
        }
    }
}
