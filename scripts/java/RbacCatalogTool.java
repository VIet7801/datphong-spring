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
                        for (String migration : List.of("V003__user_rbac_catalog.sql", "V004__support_audit_read.sql")) {
                            statement.execute(Files.readString(Path.of("scripts/sql", migration)));
                        }
                    }
                }
                Set<String> adminPermissions = rolePermissions(db, "ROLE_ADMIN");
                Set<String> supportPermissions = rolePermissions(db, "ROLE_SUPPORT");
                Set<String> missing = new TreeSet<>(REQUIRED);
                missing.removeAll(adminPermissions);
                Set<String> supportMissing = new TreeSet<>(Set.of("AUDIT_READ"));
                supportMissing.removeAll(supportPermissions);
                Map<String, Long> after = counts(db);
                if (!Objects.equals(before.get("users"), after.get("users"))) {
                    throw new IllegalStateException("Migration không được thay đổi số tài khoản");
                }
                System.out.println("BEFORE " + before);
                System.out.println("AFTER " + after);
                System.out.println("ADMIN_PERMISSIONS " + adminPermissions);
                System.out.println("MISSING_REQUIRED " + missing);
                System.out.println("SUPPORT_PERMISSIONS " + supportPermissions);
                System.out.println("SUPPORT_MISSING_REQUIRED " + supportMissing);
                if (!command.equals("inspect") && (!missing.isEmpty() || !supportMissing.isEmpty())) {
                    throw new IllegalStateException("ROLE_ADMIN/ROLE_SUPPORT còn thiếu permission bắt buộc");
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

    private static Set<String> rolePermissions(Connection db, String roleCode) throws SQLException {
        Set<String> codes = new TreeSet<>();
        try (PreparedStatement statement = db.prepareStatement("""
                     SELECT p.code FROM permissions p
                     JOIN role_permissions rp ON rp.permission_id=p.id
                     JOIN roles r ON r.id=rp.role_id WHERE r.code=?
                     """)) {
            statement.setString(1, roleCode);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) codes.add(result.getString(1));
            }
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
