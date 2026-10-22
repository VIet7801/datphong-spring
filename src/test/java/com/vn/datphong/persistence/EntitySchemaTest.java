package com.vn.datphong.persistence;

import com.vn.datphong.entity.*;
import com.vn.datphong.enums.*;
import jakarta.persistence.Entity;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.time.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EntitySchemaTest {
    private Configuration models() throws Exception {
        Configuration config = new Configuration();
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        try (var files = Files.list(Path.of("src/main/java/com/vn/datphong/entity"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Class<?> model = Class.forName("com.vn.datphong.entity." + file.getFileName().toString().replace(".java", ""));
                if (model.isAnnotationPresent(Entity.class)) config.addAnnotatedClass(model);
            }
        }
        return config;
    }

    @Test
    void allModelsBootstrapAndExportPostgresSchemaWithoutChangingDatabase() throws Exception {
        Path script = Path.of("target/schema-audit/target-schema.sql").toAbsolutePath();
        Files.createDirectories(script.getParent());
        Files.deleteIfExists(script);
        Configuration config = models();
        config.setProperty("jakarta.persistence.schema-generation.database.action", "none");
        config.setProperty("jakarta.persistence.schema-generation.scripts.action", "create");
        config.setProperty("jakarta.persistence.schema-generation.scripts.create-target", script.toString());
        try (SessionFactory factory = config.buildSessionFactory()) {
            assertEquals(74, factory.getMetamodel().getEntities().size());
        }
        String sql = Files.readString(script);
        assertTrue(sql.contains("create table room_allocations"));
        assertTrue(sql.contains("daterange"));
        assertTrue(sql.contains("create table verification_tokens"));
    }

    @Test
    void stayRangePreservesHalfOpenFiniteDates() {
        StayRange range = new StayRange(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3));
        assertEquals(range, StayRange.parse("[2027-01-01,2027-01-03)"));
        assertThrows(IllegalArgumentException.class, () -> new StayRange(range.to(), range.from()));
        assertThrows(IllegalArgumentException.class, () -> StayRange.parse("(2027-01-01,2027-01-03]"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DATPHONG_SCHEMA_VALIDATE", matches = "true")
    void everyMappedTableAndColumnValidatesAgainstLocalDatabase() throws Exception {
        Configuration config = liveConfiguration();
        try (SessionFactory factory = config.buildSessionFactory(); var session = factory.openSession()) {
            for (Class<?> type : factory.getMetamodel().getEntities().stream().map(t -> t.getJavaType()).toList()) {
                // Empty SELECTs exercise all column names and custom types, not only Hibernate's metadata validator.
                session.createSelectionQuery("from " + type.getSimpleName(), type).setMaxResults(1).getResultList();
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DATPHONG_SCHEMA_VALIDATE", matches = "true")
    void postgresRangeBindingAndFreshSchemaBaselineAreUsable() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        String schema = "entity_catalog_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(setting(yaml, "SPRING_DATASOURCE_URL"),
                setting(yaml, "SPRING_DATASOURCE_USERNAME"), setting(yaml, "SPRING_DATASOURCE_PASSWORD"))) {
            StayRange range = new StayRange(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3));
            StayRangeType rangeType = new StayRangeType();
            try (PreparedStatement statement = connection.prepareStatement("select ?::daterange")) {
                rangeType.nullSafeSet(statement, range, 1, (org.hibernate.type.descriptor.WrapperOptions) null);
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(range, rangeType.nullSafeGet(result, 1, (org.hibernate.type.descriptor.WrapperOptions) null));
                }
            }
            try {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE SCHEMA " + schema);
                    statement.execute("SET LOCAL search_path TO " + schema + ", public");
                    statement.execute(Files.readString(Path.of("scripts/sql/V000__empty_database_baseline.sql")));
                    statement.execute(Files.readString(Path.of("scripts/sql/V002__complete_catalog_constraints.sql")));
                }
                connection.commit();
                Configuration config = liveConfiguration();
                config.setProperty("hibernate.default_schema", schema);
                try (SessionFactory factory = config.buildSessionFactory()) {
                    assertEquals(74, factory.getMetamodel().getEntities().size());
                }
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
                // Only this test's freshly created, UUID-named schema is removed.
                if (!schema.matches("entity_catalog_test_[a-f0-9]{32}")) throw new IllegalStateException("Unexpected schema name");
                try (Statement statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                }
            }
        }
    }

    private Configuration liveConfiguration() throws Exception {
        Configuration config = models();
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.connection.url", setting(yaml, "SPRING_DATASOURCE_URL"));
        config.setProperty("hibernate.connection.username", setting(yaml, "SPRING_DATASOURCE_USERNAME"));
        config.setProperty("hibernate.connection.password", setting(yaml, "SPRING_DATASOURCE_PASSWORD"));
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        config.setProperty("hibernate.connection.pool_size", "2");
        return config;
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DATPHONG_SCHEMA_VALIDATE", matches = "true")
    void canonicalEntitiesAndRangeRoundTripWithoutLeavingBusinessRows() throws Exception {
        try (SessionFactory factory = liveConfiguration().buildSessionFactory(); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                String key = UUID.randomUUID().toString().replace("-", "");
                User customer = User.builder().email(key + "@example.invalid").password("test-hash")
                        .fullName("Schema mapping test").active(true).build();
                session.persist(customer);
                Role role = Role.builder().code(key).name("Mapping test").build();
                session.persist(role);
                session.persist(UserRole.builder().id(new UserRoleId(customer.getId(), role.getId()))
                        .user(customer).role(role).build());
                Property property = Property.builder().owner(customer).name("Mapping test").city("Test")
                        .addressLine("Test address").checkInFrom(LocalTime.of(14, 0)).checkOutUntil(LocalTime.NOON).build();
                session.persist(property);
                RoomTypeEntity roomType = RoomTypeEntity.builder().property(property).code(key)
                        .name("Double").bedType("DOUBLE").maxGuests(2).build();
                session.persist(roomType);
                Room room = Room.builder().property(property).roomTypeEntity(roomType).code(key).build();
                session.persist(room);
                RoomRate rate = RoomRate.builder().roomType(roomType).stayDate(LocalDate.of(2027, 1, 1))
                        .nightlyRate(new BigDecimal("100.00")).build();
                session.persist(rate);
                Booking booking = Booking.builder().bookingCode(key).user(customer).property(property)
                        .roomType(roomType).room(room).checkinDate(LocalDate.of(2027, 1, 1))
                        .checkoutDate(LocalDate.of(2027, 1, 2)).expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(600))
                        .subtotalAmount(new BigDecimal("100.00")).totalPrice(new BigDecimal("100.00"))
                        .priceVersion(1L).cancellationPolicySnapshot("{\"revision\":1}").build();
                session.persist(booking);
                session.persist(BookingNight.builder().booking(booking).stayDate(booking.getCheckinDate())
                        .nightlyRate(new BigDecimal("100.00")).taxAmount(BigDecimal.ZERO).feeAmount(BigDecimal.ZERO)
                        .priceVersion(1L).build());
                StayRange stay = new StayRange(booking.getCheckinDate(), booking.getCheckoutDate());
                RoomAllocation allocation = RoomAllocation.builder().booking(booking).room(room)
                        .checkinDate(stay.from()).checkoutDate(stay.to()).build();
                session.persist(allocation);
                Payment payment = Payment.builder().booking(booking).merchantId("schema-test").amount(new BigDecimal("100.00"))
                        .idempotencyKey(key).requestHash(key).intentExpiresAt(Instant.now().plusSeconds(600))
                        .correlationId(key).build();
                session.persist(payment);
                session.persist(VerificationToken.builder().user(customer).purpose(VerificationPurpose.EMAIL_VERIFICATION)
                        .tokenHash(key).expiresAt(Instant.now().plusSeconds(600)).build());
                session.persist(RefreshToken.builder().user(customer).familyId(UUID.randomUUID())
                        .tokenHash(key).expiresAt(Instant.now().plusSeconds(600)).build());
                session.flush();
                Long allocationId = allocation.getId();
                session.clear();
                RoomAllocation storedAllocation = session.find(RoomAllocation.class, allocationId);
                assertEquals(stay.from(), storedAllocation.getCheckinDate());
                assertEquals(stay.to(), storedAllocation.getCheckoutDate());
                assertNotNull(session.find(UserRole.class, new UserRoleId(customer.getId(), role.getId())).getAssignedAt());
            } finally {
                transaction.rollback();
            }
        }
    }

    private String setting(String yaml, String name) {
        String override = System.getenv(name);
        if (override != null && !override.isBlank()) return override;
        Matcher matcher = Pattern.compile(Pattern.quote(name) + ":([^}]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalArgumentException("Missing " + name);
        return matcher.group(1);
    }
}
