package com.vn.datphong.security;

import com.vn.datphong.config.SecurityConfig;
import com.vn.datphong.controller.*;
import com.vn.datphong.dto.request.UserStatusUpdateRequest;
import com.vn.datphong.dto.request.AuditLogSearchRequest;
import com.vn.datphong.entity.User;
import com.vn.datphong.exception.*;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.service.*;
import com.vn.datphong.service.Impl.*;
import com.vn.datphong.service.support.RbacPolicy;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.sql.DataSource;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * PostgreSQL thật + JPA + transaction + JWT + MockMvc.
 * Chỉ ghi vào schema rbac_test_<UUID>, không ghi dữ liệu vào public.
 */
@SpringJUnitConfig(UserRbacIntegrationTest.Config.class)
@WebAppConfiguration
@EnabledIfEnvironmentVariable(named = "DATPHONG_RBAC_INTEGRATION", matches = "true")
class UserRbacIntegrationTest {
    private static final String PASSWORD = "Test-password-2026";

    static class TestDatabase {
        static final String SCHEMA = "rbac_test_" + UUID.randomUUID().toString().replace("-", "");
        static String url;
        static String username;
        static String password;

        static DataSource create() throws Exception {
            String config = Files.readString(Path.of("src/main/resources/application.yaml"));
            url = setting(config, "SPRING_DATASOURCE_URL");
            username = setting(config, "SPRING_DATASOURCE_USERNAME");
            password = setting(config, "SPRING_DATASOURCE_PASSWORD");
            try (Connection db = DriverManager.getConnection(url, username, password);
                 Statement statement = db.createStatement()) {
                statement.execute("CREATE SCHEMA " + SCHEMA);
            }
            String scopedUrl = url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
            return new DriverManagerDataSource(scopedUrl, username, password);
        }

        static String setting(String config, String key) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) return value;
            var matcher = Pattern.compile(Pattern.quote(key) + ":([^}]+)").matcher(config);
            if (!matcher.find()) throw new IllegalArgumentException("Thiếu cấu hình " + key);
            return matcher.group(1);
        }
    }

    @TestConfiguration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({SecurityConfig.class, CustomUserDetailsService.class, AuthenticationService.class,
            AuthenticationController.class, UserProfileController.class, AdminUserController.class,
            AdminRoleController.class, AdminPermissionController.class, AuditLogController.class,
            UserProfileService.class, AdminUserService.class, AdminRoleService.class, PermissionService.class,
            AuditLogServiceImpl.class, AuditLogWriter.class, CurrentUser.class, RbacPolicy.class,
            GlobalExceptionHandler.class})
    static class Config {
        @Bean
        DataSource dataSource() throws Exception { return TestDatabase.create(); }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.vn.datphong.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create",
                    "hibernate.show_sql", "false", "hibernate.jdbc.time_zone", "UTC"));
            return factory;
        }

        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            return new JwtTokenProvider(Base64.getEncoder().encodeToString(key), 60_000L);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private PasswordEncoder encoder;
    @Autowired private org.springframework.security.core.userdetails.UserDetailsService details;
    @Autowired private JwtTokenProvider tokens;
    @Autowired private AdminUserService adminUsers;
    @Autowired private AuditLogService auditService;
    @Autowired private UserRepository users;
    @Autowired private RbacPolicy policy;
    @Autowired private PlatformTransactionManager transactions;
    private MockMvc mvc;
    private JdbcTemplate jdbc;
    private static String passwordHash;

    @BeforeEach
    void setup() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        String catalog = Files.readString(Path.of("scripts/sql/V003__user_rbac_catalog.sql"));
        String supportAudit = Files.readString(Path.of("scripts/sql/V004__support_audit_read.sql"));
        if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            // Các bảng khác trong schema test luôn rỗng; DELETE giữ fixture nhỏ và tránh TRUNCATE CASCADE 74 bảng.
            jdbc.execute("DELETE FROM audit_logs; DELETE FROM user_roles; DELETE FROM role_permissions; "
                    + "DELETE FROM users; DELETE FROM permissions; DELETE FROM roles");
            jdbc.execute(catalog);
            jdbc.execute(supportAudit);
            addUser(1, "admin", "ROLE_ADMIN");
            addUser(2, "admin2", "ROLE_ADMIN");
            addUser(34, "customer34", "ROLE_CUSTOMER");
            addUser(35, "customer35", "ROLE_CUSTOMER");
            addUser(50, "support", "ROLE_SUPPORT");
            jdbc.execute("SELECT setval(pg_get_serial_sequence('users','id'), 100, true)");
        });
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (TestDatabase.url == null) return;
        if (!TestDatabase.SCHEMA.matches("rbac_test_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe schema");
        try (Connection db = DriverManager.getConnection(TestDatabase.url, TestDatabase.username, TestDatabase.password);
             Statement statement = db.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + TestDatabase.SCHEMA + " CASCADE");
        }
    }

    private void addUser(long id, String username, String role) {
        jdbc.update("""
                INSERT INTO users(id, username, full_name, email, phone, password_hash,
                                  active, status, two_factor_enabled, token_version, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, '0901234567', ?, true, 'ACTIVE', false, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, username, username, username + "@example.test", passwordHash);
        jdbc.update("INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE code=?", id, role);
    }

    private String token(String username) {
        var principal = details.loadUserByUsername(username);
        return tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private long id(String table, String code) {
        // table/code chỉ lấy từ literal của test.
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code=?", Long.class, code);
    }

    private long auditCount(String action) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action=?", Long.class, action);
    }

    private ResultActions jsonRequest(MockHttpServletRequestBuilder request, String username, String body) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token(username))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private MockHttpServletRequestBuilder auditRequest(String path) {
        return get(path).param("from", OffsetDateTime.now().minusHours(1).toString())
                .param("to", OffsetDateTime.now().plusHours(1).toString());
    }

    @Test
    void selfProfileUsesPrincipalAndHidesAdministrationAndSecrets() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token("customer34")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(34))
                .andExpect(jsonPath("$.data.email").value("customer34@example.test"))
                .andExpect(jsonPath("$.data.roles").doesNotExist())
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.twoFactorSecret").doesNotExist());
    }

    @Test
    void oldMixedUserEndpointsAreNoLongerExposed() throws Exception {
        mvc.perform(get("/users/findAll").header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isNotFound());
        jsonRequest(put("/users/update/35"), "customer34",
                """
                {"version":0,"fullName":"Not the owner"}
                """).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "roles", "roleIds", "roleId", "status", "active", "password",
            "username", "email", "ownerId", "tokenVersion"})
    void massAssignmentIsRejectedWithoutChangingProfile(String field) throws Exception {
        // Dùng giá trị đúng kiểu: tránh trường hợp test chỉ bắt lỗi type thay vì chặn mass assignment.
        String value = switch (field) {
            case "id", "ownerId" -> "35";
            case "roles" -> "[{\"id\":" + id("roles", "ROLE_ADMIN") + ",\"code\":\"ROLE_ADMIN\"}]";
            case "roleIds", "roleId" -> "[" + id("roles", "ROLE_ADMIN") + "]";
            case "active" -> "true";
            case "status" -> "\"ACTIVE\"";
            case "password" -> "\"Injected-password-2026\"";
            case "username" -> "\"other_user\"";
            case "email" -> "\"owner@example.test\"";
            case "tokenVersion" -> "1";
            default -> throw new IllegalArgumentException(field);
        };
        jsonRequest(patch("/api/v1/users/me"), "customer34",
                "{\"version\":0,\"fullName\":\"Injected\",\"" + field + "\":" + value + "}")
                .andExpect(status().isBadRequest());
        assertEquals("customer34", jdbc.queryForObject("SELECT full_name FROM users WHERE id=34", String.class));
        assertEquals(0, auditCount("USER_PROFILE_UPDATED"));
    }

    @Test
    void profileUpdateAuditsOnlyRealChangesAndChecksVersion() throws Exception {
        jsonRequest(patch("/api/v1/users/me"), "customer34",
                """
                {"version":0,"fullName":"Tên mới","phone":"0909999999"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        assertEquals("customer35", jdbc.queryForObject("SELECT full_name FROM users WHERE id=35", String.class));
        String event = jdbc.queryForObject("SELECT after_state::text FROM audit_logs WHERE action='USER_PROFILE_UPDATED'", String.class);
        assertTrue(event.contains("[REDACTED]"));
        assertFalse(event.contains("0909999999"));
        jsonRequest(patch("/api/v1/users/me"), "customer34", """
                {"version":1,"fullName":"Tên mới","phone":"0909999999"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        assertEquals(1, auditCount("USER_PROFILE_UPDATED"));
        jsonRequest(patch("/api/v1/users/me"), "customer34", """
                {"version":0,"fullName":"Stale"}
                """).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VERSION_CONFLICT"));
    }

    @Test
    void auditFailureRollsBackAnAlreadyFlushedProfileUpdate() throws Exception {
        jdbc.execute("ALTER TABLE audit_logs ADD CONSTRAINT reject_profile_test CHECK (action <> 'USER_PROFILE_UPDATED')");
        try {
            jsonRequest(patch("/api/v1/users/me"), "customer34", """
                    {"version":0,"fullName":"Must rollback"}
                    """).andExpect(status().isConflict());
            assertEquals("customer34", jdbc.queryForObject("SELECT full_name FROM users WHERE id=34", String.class));
            assertEquals(0L, jdbc.queryForObject("SELECT version FROM users WHERE id=34", Long.class));
            assertEquals(0, auditCount("USER_PROFILE_UPDATED"));
        } finally {
            jdbc.execute("ALTER TABLE audit_logs DROP CONSTRAINT reject_profile_test");
        }
    }

    @Test
    void administrationRequiresRoleAndSpecificPermissionAtService() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token("customer34")))
                .andExpect(status().isForbidden());
        String adminToken = token("admin");
        jdbc.update("""
                DELETE FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='ROLE_ADMIN')
                AND permission_id=(SELECT id FROM permissions WHERE code='USER_READ')
                """);
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void directServiceCallCannotBypassPermission() {
        assertThrows(AccessDeniedException.class, () -> adminUsers.search(null, null, 0, 20));
    }

    @Test
    void adminListIsPaginatedMaskedAndValidatesPageSize() throws Exception {
        mvc.perform(get("/api/v1/admin/users").param("size", "2")
                .header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(50))
                .andExpect(jsonPath("$.data.items[0].email").value("s***@example.test"))
                .andExpect(jsonPath("$.data.items[0].phone").value("***567"))
                .andExpect(jsonPath("$.data.items[0].password").doesNotExist());
        mvc.perform(get("/api/v1/admin/users").param("size", "101")
                .header("Authorization", "Bearer " + token("admin"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/users").param("status", "ACTIVE").param("keyword", "customer")
                .header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    void adminCanCreateInactiveCustomerWithHashedPasswordAndSeparateRoleApi() throws Exception {
        jsonRequest(post("/api/v1/admin/users"), "admin", """
                {"username":"new_customer","fullName":"New Customer","email":"new@example.test",
                 "password":"New-password-2026","reason":"Tạo tài khoản theo yêu cầu"}
                """).andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("IN_ACTIVE"))
                .andExpect(jsonPath("$.data.roles", contains("ROLE_CUSTOMER")));
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username='new_customer'", String.class);
        assertTrue(encoder.matches("New-password-2026", hash));
        String audit = jdbc.queryForObject("SELECT after_state::text FROM audit_logs WHERE action='USER_CREATED'", String.class);
        assertFalse(audit.contains("password"));
        assertFalse(audit.contains("new@example.test"));
        assertFalse(audit.contains(hash));
    }

    @Test
    void lockThenUnlockDoesNotReviveOldJwt() throws Exception {
        String oldCustomerToken = token("customer34");
        jsonRequest(patch("/api/v1/admin/users/34/status"), "admin",
                """
                {"version":0,"status":"LOCKED","reason":"Khóa để kiểm tra"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldCustomerToken))
                .andExpect(status().isUnauthorized());
        jsonRequest(patch("/api/v1/admin/users/34/status"), "admin",
                """
                {"version":1,"status":"ACTIVE","reason":"Mở khóa"}
                """).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldCustomerToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token("customer34")))
                .andExpect(status().isOk());
        assertEquals(2, auditCount("USER_STATUS_CHANGED"));
    }

    @Test
    void cannotChangeOwnStatusOrOwnRoles() throws Exception {
        jsonRequest(patch("/api/v1/admin/users/1/status"), "admin", """
                {"version":0,"status":"LOCKED","reason":"Self lock"}
                """).andExpect(status().isForbidden());
        jsonRequest(put("/api/v1/admin/users/1/roles"), "admin",
                "{\"version\":0,\"roleIds\":[" + id("roles", "ROLE_CUSTOMER") + "],\"reason\":\"Self role\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void lastActiveAdminIsProtectedUnderDatabaseLock() {
        jdbc.update("UPDATE users SET active=false, status='LOCKED' WHERE id=2");
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            policy.lockAdministration();
            User lastAdmin = users.findById(1L).orElseThrow();
            BusinessException error = assertThrows(BusinessException.class,
                    () -> policy.preserveLastAdmin(lastAdmin, false));
            assertEquals(409, error.getStatus());
        });
    }

    @Test
    void assigningRolesRevokesOldJwtAndHasItsOwnAudit() throws Exception {
        String oldToken = token("customer34");
        jsonRequest(put("/api/v1/admin/users/34/roles"), "admin",
                "{\"version\":0,\"roleIds\":[" + id("roles", "ROLE_SUPPORT") + "],\"reason\":\"Đổi bộ phận\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roles", contains("ROLE_SUPPORT")));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        assertEquals(1, auditCount("USER_ROLES_CHANGED"));
        assertEquals(1L, jdbc.queryForObject("SELECT token_version FROM users WHERE id=34", Long.class));
    }

    @Test
    void passwordChangeVerifiesOldPasswordAndRevokesJwt() throws Exception {
        String oldToken = token("customer34");
        jsonRequest(patch("/api/v1/users/me/password"), "customer34", """
                {"version":0,"currentPassword":"wrong","newPassword":"Changed-password-2026"}
                """).andExpect(status().isBadRequest());
        jsonRequest(patch("/api/v1/users/me/password"), "customer34",
                "{\"version\":0,\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Changed-password-2026\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        String stored = jdbc.queryForObject("SELECT password_hash FROM users WHERE id=34", String.class);
        assertTrue(encoder.matches("Changed-password-2026", stored));
        String event = jdbc.queryForObject("SELECT after_state::text FROM audit_logs WHERE action='USER_PASSWORD_CHANGED'", String.class);
        assertEquals("{\"passwordChanged\": true}", event);
    }

    @Test
    void roleAndPermissionCatalogWorkflowAuditsAndProtectsUsedEntries() throws Exception {
        assertTrue(details.loadUserByUsername("admin").getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGE".equals(a.getAuthority())));
        String payload = new String(Base64.getUrlDecoder().decode(token("admin").split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(payload.contains("\"ROLE_MANAGE\""));
        jsonRequest(post("/api/v1/admin/roles"), "admin",
                """
                {"code":"ROLE_MANAGE","name":"Không được trùng permission","reason":"Kiểm tra namespace"}
                """).andExpect(status().isConflict());
        jsonRequest(post("/api/v1/admin/roles"), "admin",
                """
                {"code":"ROLE_REVIEWER","name":"Reviewer","reason":"Tạo role"}
                """).andExpect(status().isCreated());
        long roleId = id("roles", "ROLE_REVIEWER");
        jsonRequest(put("/api/v1/admin/roles/" + roleId + "/permissions"), "admin",
                "{\"version\":0,\"permissionIds\":[" + id("permissions", "USER_READ") + "],\"reason\":\"Cấp quyền đọc\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.permissions", contains("USER_READ")));
        jsonRequest(patch("/api/v1/admin/roles/" + roleId), "admin",
                """
                {"version":1,"name":"Reviewer mới","reason":"Đổi tên"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2));
        jsonRequest(delete("/api/v1/admin/roles/" + roleId), "admin",
                """
                {"version":2,"reason":"Xóa role chưa sử dụng"}
                """).andExpect(status().isNoContent());
        jsonRequest(delete("/api/v1/admin/roles/" + id("roles", "ROLE_CUSTOMER")), "admin",
                """
                {"version":0,"reason":"Không được xóa"}
                """).andExpect(status().isConflict());
        assertEquals(1, auditCount("ROLE_PERMISSIONS_CHANGED"));
        assertEquals(1, auditCount("ROLE_DELETED"));

        jsonRequest(post("/api/v1/admin/permissions"), "admin",
                """
                {"code":"FEATURE_READ","name":"Đọc chức năng mới","reason":"Tạo catalog"}
                """).andExpect(status().isCreated());
        long permissionId = id("permissions", "FEATURE_READ");
        jsonRequest(patch("/api/v1/admin/permissions/" + permissionId), "admin",
                """
                {"version":0,"name":"Đọc chức năng thử nghiệm","reason":"Đổi tên"}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        jsonRequest(delete("/api/v1/admin/permissions/" + permissionId), "admin",
                """
                {"version":1,"reason":"Xóa mã chưa sử dụng"}
                """).andExpect(status().isNoContent());
        assertEquals(1, auditCount("PERMISSION_CREATED"));
        assertEquals(1, auditCount("PERMISSION_UPDATED"));
        assertEquals(1, auditCount("PERMISSION_DELETED"));
    }

    @Test
    void cannotModifyOwnRolePermissionsOrDelegateUnownedPermission() throws Exception {
        jsonRequest(put("/api/v1/admin/roles/" + id("roles", "ROLE_ADMIN") + "/permissions"), "admin",
                """
                {"version":0,"permissionIds":[],"reason":"Self escalation"}
                """).andExpect(status().isForbidden());
        jdbc.update("""
                DELETE FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='ROLE_ADMIN')
                AND permission_id=(SELECT id FROM permissions WHERE code='AUDIT_READ')
                """);
        jsonRequest(put("/api/v1/admin/roles/" + id("roles", "ROLE_SUPPORT") + "/permissions"), "admin",
                "{\"version\":1,\"permissionIds\":[],\"reason\":\"Thu hồi vượt quyền\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void permissionCannotImpersonateRoleEvenWithBadLegacyDatabaseRow() throws Exception {
        jsonRequest(post("/api/v1/admin/permissions"), "admin",
                """
                {"code":"ROLE_SUPERUSER","name":"Forbidden namespace","reason":"Test"}
                """).andExpect(status().isBadRequest());
        jdbc.update("""
                INSERT INTO permissions(code,name,version,created_at,updated_at)
                VALUES ('ROLE_ADMIN','Bad legacy permission',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)",
                id("roles", "ROLE_SUPPORT"), id("permissions", "ROLE_ADMIN"));
        assertFalse(details.loadUserByUsername("support").getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token("support")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/admin/audit-logs", "/api/admin/audit-logs"})
    void adminAndSupportWithAuditReadCanSearchAudit(String path) throws Exception {
        mvc.perform(auditRequest(path)).andExpect(status().isUnauthorized());
        mvc.perform(auditRequest(path).header("Authorization", "Bearer " + token("support")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
        assertEquals(50L, jdbc.queryForObject(
                "SELECT actor_id FROM audit_logs WHERE action='AUDIT_LOG_SEARCHED'", Long.class));
        assertEquals("Tra cứu Audit Log", jdbc.queryForObject(
                "SELECT reason FROM audit_logs WHERE action='AUDIT_LOG_SEARCHED'", String.class));
        mvc.perform(auditRequest(path).header("Authorization", "Bearer " + token("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isArray());
        assertEquals(2, auditCount("AUDIT_LOG_SEARCHED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/admin/audit-logs", "/api/admin/audit-logs"})
    void supportLosingAuditReadIsDeniedEvenWithPreviouslyIssuedJwt(String path) throws Exception {
        String previouslyIssuedToken = token("support");
        jdbc.update("""
                DELETE FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='ROLE_SUPPORT')
                AND permission_id=(SELECT id FROM permissions WHERE code='AUDIT_READ')
                """);
        mvc.perform(auditRequest(path).header("Authorization", "Bearer " + previouslyIssuedToken))
                .andExpect(status().isForbidden());
        assertEquals(0, auditCount("AUDIT_LOG_SEARCHED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CUSTOMER", "ROLE_HOST"})
    void customerAndHostCannotSearchAuditEvenWithAuditRead(String roleCode) throws Exception {
        jdbc.update("DELETE FROM user_roles WHERE user_id=35");
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (35,?)", id("roles", roleCode));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)",
                id("roles", roleCode), id("permissions", "AUDIT_READ"));
        String token = token("customer35");
        for (String path : List.of("/api/v1/admin/audit-logs", "/api/admin/audit-logs")) {
            mvc.perform(auditRequest(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        assertEquals(0, auditCount("AUDIT_LOG_SEARCHED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/admin/users", "/api/v1/admin/roles", "/api/v1/admin/permissions"})
    void supportCannotAccessOtherAdminApisEvenWithTheirPermissions(String path) throws Exception {
        jdbc.update("""
                INSERT INTO role_permissions(role_id,permission_id)
                SELECT ?, p.id FROM permissions p ON CONFLICT DO NOTHING
                """, id("roles", "ROLE_SUPPORT"));
        mvc.perform(get(path).header("Authorization", "Bearer " + token("support")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "support", authorities = {"ROLE_SUPPORT", "AUDIT_READ"})
    void supportCanCallAuditServiceDirectlyWithPermission() {
        AuditLogSearchRequest request = new AuditLogSearchRequest();
        request.setFrom(OffsetDateTime.now().minusHours(1));
        request.setTo(OffsetDateTime.now().plusHours(1));
        assertNotNull(auditService.search(request));
        assertEquals(1, auditCount("AUDIT_LOG_SEARCHED"));
    }

    @Test
    @WithMockUser(username = "support", authorities = "ROLE_SUPPORT")
    void supportCannotCallAuditServiceDirectlyWithoutPermission() {
        assertThrows(AccessDeniedException.class, () -> auditService.search(new AuditLogSearchRequest()));
    }

    @Test
    @WithMockUser(username = "customer34", authorities = {"ROLE_CUSTOMER", "AUDIT_READ"})
    void customerCannotBypassRoleCheckByCallingAuditServiceDirectly() {
        assertThrows(AccessDeniedException.class, () -> auditService.search(new AuditLogSearchRequest()));
    }

    @Test
    @WithMockUser(username = "customer35", authorities = {"ROLE_HOST", "AUDIT_READ"})
    void hostCannotBypassRoleCheckByCallingAuditServiceDirectly() {
        assertThrows(AccessDeniedException.class, () -> auditService.search(new AuditLogSearchRequest()));
    }

    @Test
    void auditAccessFailsWhenItsAccessEventCannotBeSaved() throws Exception {
        jdbc.execute("ALTER TABLE audit_logs ADD CONSTRAINT reject_audit_search_test CHECK (action <> 'AUDIT_LOG_SEARCHED')");
        try {
            mvc.perform(auditRequest("/api/v1/admin/audit-logs")
                    .header("Authorization", "Bearer " + token("support")))
                    .andExpect(status().isConflict());
            assertEquals(0, auditCount("AUDIT_LOG_SEARCHED"));
        } finally {
            jdbc.execute("ALTER TABLE audit_logs DROP CONSTRAINT reject_audit_search_test");
        }
    }

    @Test
    void adminWithoutAuditReadCannotQueryAudit() throws Exception {
        jdbc.update("""
                DELETE FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='ROLE_ADMIN')
                AND permission_id=(SELECT id FROM permissions WHERE code='AUDIT_READ')
                """);
        mvc.perform(get("/api/v1/admin/audit-logs")
                .param("from", OffsetDateTime.now().minusHours(1).toString())
                .param("to", OffsetDateTime.now().plusHours(1).toString())
                .header("Authorization", "Bearer " + token("admin"))).andExpect(status().isForbidden());
        assertEquals(0, auditCount("AUDIT_LOG_SEARCHED"));
    }

    @Test
    void catalogSeedIsIdempotent() throws Exception {
        long before = jdbc.queryForObject("SELECT count(*) FROM audit_logs", Long.class);
        jdbc.execute(Files.readString(Path.of("scripts/sql/V003__user_rbac_catalog.sql")));
        jdbc.execute(Files.readString(Path.of("scripts/sql/V004__support_audit_read.sql")));
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM audit_logs", Long.class));
        assertEquals(9L, jdbc.queryForObject("SELECT count(*) FROM role_permissions", Long.class));
        assertEquals(5L, jdbc.queryForObject("SELECT count(*) FROM users", Long.class));
    }

    @Test
    void supportMigrationAddsMissingGrantAndPreservesExistingGrants() throws Exception {
        jdbc.update("""
                DELETE FROM role_permissions WHERE role_id=(SELECT id FROM roles WHERE code='ROLE_SUPPORT')
                AND permission_id=(SELECT id FROM permissions WHERE code='AUDIT_READ')
                """);
        long supportVersion = jdbc.queryForObject(
                "SELECT version FROM roles WHERE code='ROLE_SUPPORT'", Long.class);
        long beforeEvents = auditCount("AUDIT_ACCESS_POLICY_UPDATED");
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            try {
                jdbc.execute(Files.readString(Path.of("scripts/sql/V004__support_audit_read.sql")));
            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
        });
        assertEquals(9L, jdbc.queryForObject("SELECT count(*) FROM role_permissions", Long.class));
        assertEquals(supportVersion + 1, jdbc.queryForObject(
                "SELECT version FROM roles WHERE code='ROLE_SUPPORT'", Long.class));
        assertEquals(beforeEvents + 1, auditCount("AUDIT_ACCESS_POLICY_UPDATED"));
        assertEquals(5L, jdbc.queryForObject("SELECT count(*) FROM users", Long.class));
    }
}
