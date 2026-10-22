import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Khởi tạo RBAC riêng, không yêu cầu database rỗng và không ghi credentials ra log. */
public class RbacCatalogTool {
    private static final List<String> REQUIRED = List.of("USER_READ", "USER_MANAGE", "USER_ROLE_MANAGE",
            "ROLE_READ", "ROLE_MANAGE", "PERMISSION_READ", "PERMISSION_MANAGE", "AUDIT_READ");

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "verify" : args[0];
        if (!Set.of("inspect", "verify", "apply").contains(command) || args.length > 1) {
            throw new IllegalArgumentException("inspect | verify | apply");
        }
        String config = Files.readString(Path.of("src/main/resources/application.yaml"));
        Properties properties = new Properties();
        properties.setProperty("user", SchemaTool.setting(config, "SPRING_DATASOURCE_USERNAME"));
        properties.setProperty("password", SchemaTool.setting(config, "SPRING_DATASOURCE_PASSWORD"));
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("ApplicationName", "DatphongUserRbac");
        try (Connection db = DriverManager.getConnection(
                SchemaTool.setting(config, "SPRING_DATASOURCE_URL"), properties)) {
            db.setAutoCommit(false);
            db.setReadOnly(!command.equals("apply"));
            try {
                Map<String, Long> before = counts(db);
                if (command.equals("apply")) {
                    try (Statement statement = db.createStatement()) {
                        statement.execute("SET LOCAL lock_timeout = '5s'");
                        statement.execute("SET LOCAL statement_timeout = '30s'");
                        statement.execute(Files.readString(Path.of("scripts/sql/V003__user_rbac_catalog.sql")));
                    }
                }
                Set<String> adminPermissions = adminPermissions(db);
                Set<String> missing = new TreeSet<>(REQUIRED);
                missing.removeAll(adminPermissions);
                Map<String, Long> after = counts(db);
                if (!Objects.equals(before.get("users"), after.get("users"))) {
                    throw new IllegalStateException("Migration không được thay đổi số tài khoản");
                }
                System.out.println("BEFORE " + before);
                System.out.println("AFTER " + after);
                System.out.println("ADMIN_PERMISSIONS " + adminPermissions);
                System.out.println("MISSING_REQUIRED " + missing);
                if (!command.equals("inspect") && !missing.isEmpty()) {
                    throw new IllegalStateException("ROLE_ADMIN còn thiếu permission bắt buộc");
                }
                if (command.equals("apply")) {
                    db.commit();
                    System.out.println("RBAC_APPLIED: dữ liệu User và mật khẩu được giữ nguyên.");
                } else {
                    db.rollback();
                    System.out.println(command.equals("verify") ? "RBAC_VERIFIED" : "RBAC_INSPECTED");
                }
            } catch (Exception failure) {
                db.rollback();
                throw failure;
            }
        }
    }

    private static Set<String> adminPermissions(Connection db) throws SQLException {
        Set<String> codes = new TreeSet<>();
        try (Statement statement = db.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT p.code FROM permissions p
                     JOIN role_permissions rp ON rp.permission_id=p.id
                     JOIN roles r ON r.id=rp.role_id WHERE r.code='ROLE_ADMIN'
                     """)) {
            while (result.next()) codes.add(result.getString(1));
        }
        return codes;
    }

    private static Map<String, Long> counts(Connection db) throws SQLException {
        Map<String, Long> values = new LinkedHashMap<>();
        for (String table : List.of("users", "roles", "permissions", "role_permissions", "audit_logs")) {
            try (Statement statement = db.createStatement();
                 ResultSet result = statement.executeQuery("SELECT count(*) FROM " + table)) {
                result.next();
                values.put(table, result.getLong(1));
            }
        }
        return values;
    }
}
