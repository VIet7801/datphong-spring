package com.vn.datphong.service;

import com.vn.datphong.entity.*;
import com.vn.datphong.enums.*;
import com.vn.datphong.exception.DomainException;
import com.vn.datphong.repository.*;
import com.vn.datphong.service.Impl.*;
import com.vn.datphong.util.FakeBankFaults;
import com.vn.datphong.util.FakeBankWebhookDispatcher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** PostgreSQL + transaction + Fake Bank thật, chỉ ghi vào schema refund_test_<UUID>. */
@SpringJUnitConfig(RefundServiceIntegrationTest.Config.class)
@EnabledIfEnvironmentVariable(named = "DATPHONG_REFUND_INTEGRATION", matches = "true")
class RefundServiceIntegrationTest {
    static final String SCHEMA = "refund_test_" + UUID.randomUUID().toString().replace("-", "");
    static String url, username, password;

    @TestConfiguration
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableJpaRepositories(basePackageClasses = RefundRequestRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    RefundRequestRepository.class, BookingRepository.class, PaymentRepository.class,
                    BookingStatusHistoryRepository.class, RoomAllocationRepository.class, OutboxEventRepository.class,
                    FakeBankAccountRepository.class, FakeBankTransactionRepository.class, FakeBankWebhookEventRepository.class}))
    @Import({RefundServiceImpl.class, RefundProcessor.class, PaymentOutcomeServiceImpl.class,
            BookingLifecycle.class, OutboxServiceImpl.class,
            FakeBankServiceImpl.class, FakeBankGateway.class})
    static class Config {
        @Bean MutableClock clock() { return new MutableClock(); }
        @Bean FakeBankFaults faults() { return new FakeBankFaults(); }
        @Bean FakeBankWebhookDispatcher dispatcher() { return mock(FakeBankWebhookDispatcher.class); }

        @Bean
        DataSource dataSource() throws Exception {
            String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
            url = setting(yaml, "SPRING_DATASOURCE_URL");
            username = setting(yaml, "SPRING_DATASOURCE_USERNAME");
            password = setting(yaml, "SPRING_DATASOURCE_PASSWORD");
            try (Connection db = DriverManager.getConnection(url, username, password); Statement st = db.createStatement()) {
                st.execute("CREATE SCHEMA " + SCHEMA);
            }
            return new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA,
                    username, password);
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.vn.datphong.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create", "hibernate.show_sql", "false"));
            return factory;
        }

        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }

    static String setting(String yaml, String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) return value;
        var match = Pattern.compile(Pattern.quote(name) + ":([^}]+)").matcher(yaml);
        if (!match.find()) throw new IllegalArgumentException("Missing " + name);
        return match.group(1);
    }

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.now();
        void advance(long seconds) { now = now.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }

    @Autowired RefundService refunds;
    @Autowired RefundProcessor processor;
    @Autowired PaymentOutcomeService paymentOutcomes;
    @Autowired FakeBankService bank;
    @Autowired FakeBankFaults faults;
    @Autowired MutableClock clock;
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactions;
    @Autowired RefundRequestRepository repository;
    @PersistenceContext EntityManager em;
    TransactionTemplate tx;
    JdbcTemplate jdbc;

    record Fixture(Booking booking, Payment payment, String account, String debitKey) {}

    @BeforeEach
    void resetDatabase() {
        jdbc = new JdbcTemplate(dataSource);
        tx = new TransactionTemplate(transactions);
        assertEquals(SCHEMA, jdbc.queryForObject("select current_schema()", String.class));
        // Chỉ xóa fixture liên quan; TRUNCATE CASCADE toàn bộ catalog gây nhiều fsync không cần thiết.
        tx.executeWithoutResult(status -> jdbc.execute("DELETE FROM outbox_events; DELETE FROM refund_requests; "
                + "DELETE FROM refunds; DELETE FROM fakebank_webhook_events; DELETE FROM fakebank_transactions; "
                + "DELETE FROM fakebank_accounts; DELETE FROM booking_status_history; DELETE FROM room_allocations; "
                + "DELETE FROM payments; DELETE FROM bookings; DELETE FROM rooms; DELETE FROM room_types; "
                + "DELETE FROM properties; DELETE FROM users"));
        faults.set(0, 0);
    }

    @AfterAll
    static void dropTestSchema() throws Exception {
        if (url == null) return;
        if (!SCHEMA.matches("refund_test_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe schema");
        try (Connection db = DriverManager.getConnection(url, username, password); Statement st = db.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    Fixture fixture() {
        String key = UUID.randomUUID().toString().substring(0, 18);
        String account = "account-" + key;
        String debitKey = "debit:" + key;
        Fixture f = tx.execute(status -> {
            User user = User.builder().email(key + "@example.invalid").username(key).password("test-hash")
                    .fullName("Refund test").active(true).build();
            em.persist(user);
            Property property = Property.builder().owner(user).name("Refund test").city("Test").addressLine("Test")
                    .checkInFrom(LocalTime.of(14, 0)).checkOutUntil(LocalTime.NOON).build();
            em.persist(property);
            RoomTypeEntity type = RoomTypeEntity.builder().property(property).code(key).name("Double")
                    .bedType("DOUBLE").maxGuests(2).build();
            em.persist(type);
            Room room = Room.builder().property(property).roomTypeEntity(type).code(key).build();
            em.persist(room);
            Booking booking = Booking.builder().bookingCode(key).user(user).property(property).room(room).roomType(type)
                    .checkinDate(LocalDate.now(clock).plusDays(30)).checkoutDate(LocalDate.now(clock).plusDays(31))
                    .status(BookingStatusType.REFUND_PENDING).subtotalAmount(money("100")).totalPrice(money("100"))
                    .priceVersion(1L).expiresAt(LocalDateTime.now(clock).plusMinutes(10))
                    .cancellationPolicySnapshot("{\"revision\":1}").build();
            em.persist(booking);
            Payment payment = Payment.builder().booking(booking).amount(money("100")).capturedAmount(money("100"))
                    .status(PaymentStatusType.SUCCEEDED).idempotencyKey(debitKey).payerAccount(account)
                    .paidAt(LocalDateTime.now(clock)).capturedAt(LocalDateTime.now(clock)).build();
            em.persist(payment);
            FakeBankAccount bankAccount = new FakeBankAccount();
            bankAccount.setAccountNo(account);
            bankAccount.setHolderName("Refund test");
            bankAccount.setBalance(money("500"));
            em.persist(bankAccount);
            return new Fixture(booking, payment, account, debitKey);
        });
        var debit = bank.debit(debitKey, account, money("100"), "VND", FakeBankService.Scenario.CALLBACK_LOST);
        assertTrue(debit.success());
        tx.executeWithoutResult(status -> em.find(Payment.class, f.payment().getId()).setProviderReference(debit.reference()));
        return f;
    }

    RefundRequest request(Fixture f, String amount, String key) {
        return refunds.request(f.booking(), f.payment(), money(amount), RefundService.KIND_CANCELLATION, "Cancel", key);
    }

    static BigDecimal money(String value) { return new BigDecimal(value); }
    RefundRequest stored(Long id) { return repository.findById(id).orElseThrow(); }
    BigDecimal paidBack(Fixture f) { return jdbc.queryForObject("select refunded_amount from payments where id=?", BigDecimal.class, f.payment().getId()); }
    BigDecimal balance(Fixture f) { return jdbc.queryForObject("select balance from fakebank_accounts where account_no=?", BigDecimal.class, f.account()); }
    String bookingStatus(Fixture f) { return jdbc.queryForObject("select status from bookings where id=?", String.class, f.booking().getId()); }
    long count(String table) { return jdbc.queryForObject("select count(*) from " + table, Long.class); }

    @Test
    void requestReplaysSamePayloadAndRejectsChangedPayload() {
        Fixture f = fixture();
        var first = request(f, "80", "refund:cancel:1");
        assertEquals(first.getId(), request(f, "80.00", "refund:cancel:1").getId());
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(DomainException.class,
                () -> request(f, "70", "refund:cancel:1")).getCode());
        assertEquals(1, count("refund_requests"));
        assertEquals(1, count("outbox_events"));
    }

    @Test
    void refusesOverRefundAndFractionalVnd() {
        Fixture f = fixture();
        request(f, "80", "refund:a");
        assertEquals("REFUND_AMOUNT_EXCEEDED", assertThrows(DomainException.class,
                () -> request(f, "21", "refund:b")).getCode());
        assertEquals("VALIDATION_ERROR", assertThrows(DomainException.class,
                () -> request(f, "0.5", "refund:fraction")).getCode());
        assertEquals(1, count("refund_requests"));
    }

    @Test
    void refusesUncapturedOrWrongBookingPayment() {
        Fixture f = fixture();
        Fixture other = fixture();
        assertEquals("PAYMENT_BOOKING_MISMATCH", assertThrows(DomainException.class, () ->
                refunds.request(other.booking(), f.payment(), money("10"), RefundService.KIND_CANCELLATION, "Cancel", "refund:wrong")).getCode());
        tx.executeWithoutResult(status -> {
            Payment payment = em.find(Payment.class, f.payment().getId());
            payment.setStatus(PaymentStatusType.INITIATED);
            payment.setCapturedAmount(BigDecimal.ZERO);
        });
        assertEquals("PAYMENT_NOT_CAPTURED", assertThrows(DomainException.class,
                () -> request(f, "10", "refund:unpaid")).getCode());
    }

    @Test
    void concurrentSameKeyCreatesOneRequest() throws Exception {
        Fixture f = fixture();
        var ids = concurrently(() -> request(f, "80", "refund:same").getId(), () -> request(f, "80", "refund:same").getId());
        assertEquals(ids.get(0), ids.get(1));
        assertEquals(1, count("refund_requests"));
        assertEquals(1, count("outbox_events"));
    }

    @Test
    void concurrentDifferentKeysCannotReserveMoreThanCaptured() throws Exception {
        Fixture f = fixture();
        var outcomes = concurrently(() -> attemptRequest(f, "refund:a"), () -> attemptRequest(f, "refund:b"));
        assertEquals(Set.of("CREATED", "REFUND_AMOUNT_EXCEEDED"), new HashSet<>(outcomes));
        assertEquals(1, count("refund_requests"));
    }

    String attemptRequest(Fixture f, String key) {
        try { request(f, "70", key); return "CREATED"; }
        catch (DomainException e) { return e.getCode(); }
    }

    @Test
    void claimHonorsLeaseEvenWithForceAndRecoversAfterExpiry() {
        Fixture f = fixture();
        var r = request(f, "100", "refund:lease");
        var first = refunds.claim(r.getId(), false);
        assertNotNull(first);
        assertNull(refunds.claim(r.getId(), false));
        assertNull(refunds.claim(r.getId(), true));
        clock.advance(61);
        assertEquals(first, refunds.claim(r.getId(), false));
        assertEquals(2, stored(r.getId()).getAttempts());
    }

    @Test
    void completesOnceAndKeepsCapturedPaymentSucceeded() {
        Fixture f = fixture();
        var r = request(f, "80", "refund:success");
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals(0, money("80").compareTo(paidBack(f)));
        assertEquals(0, money("480").compareTo(balance(f)));
        assertEquals("REFUNDED", bookingStatus(f));
        assertEquals("SUCCEEDED", jdbc.queryForObject("select status from payments where id=?", String.class, f.payment().getId()));
        refunds.complete(r.getId(), bank.query("refund:success").orElseThrow());
        refunds.complete(r.getId(), new FakeBankService.TxResult("FAILED", null, "OLD_FAILURE", money("80")));
        processor.process(r.getId(), true);
        assertEquals(0, money("80").compareTo(paidBack(f)));
        assertEquals(2, count("fakebank_transactions"));
        assertEquals(2, count("outbox_events"));
        assertEquals(1, count("booking_status_history"));
    }

    @Test
    void bookingWaitsForAllCancellationRefunds() {
        Fixture f = fixture();
        var a = request(f, "20", "refund:a");
        var b = request(f, "30", "refund:b");
        processor.process(a.getId(), false);
        assertEquals("REFUND_PENDING", bookingStatus(f));
        processor.process(b.getId(), false);
        assertEquals("REFUNDED", bookingStatus(f));
        assertEquals(0, money("50").compareTo(paidBack(f)));
    }

    @Test
    void bankTimeoutDoesNotRefundTwiceAndRespectsRetryDelay() {
        Fixture f = fixture();
        var r = request(f, "100", "refund:timeout");
        faults.set(0, 1);
        processor.scan();
        assertEquals(RefundStatusType.UNKNOWN, stored(r.getId()).getStatus());
        assertEquals(0, money("500").compareTo(balance(f)));
        assertEquals(0, BigDecimal.ZERO.compareTo(paidBack(f)));
        processor.scan();
        assertEquals(1, stored(r.getId()).getAttempts());
        clock.advance(16);
        processor.scan();
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals(0, money("100").compareTo(paidBack(f)));
        assertEquals(0, money("500").compareTo(balance(f)));
        assertEquals(2, count("fakebank_transactions"));
    }

    @Test
    void transientFailureRetriesSameRequestAndStopsAfterFiveAttempts() {
        Fixture f = fixture();
        var r = request(f, "100", "refund:failure");
        faults.set(5, 0);
        for (int i = 0; i < 5; i++) { processor.scan(); clock.advance(301); }
        assertEquals(RefundStatusType.NEEDS_ATTENTION, stored(r.getId()).getStatus());
        assertEquals(5, stored(r.getId()).getAttempts());
        processor.scan();
        assertEquals(5, stored(r.getId()).getAttempts());
        assertEquals(0, money("400").compareTo(balance(f)));
        assertEquals(1, count("refund_requests"));
        processor.process(r.getId(), true);
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals(0, money("100").compareTo(paidBack(f)));
    }

    @Test
    void failureStillReservesMoneyForTheSameRequest() {
        Fixture f = fixture();
        var r = request(f, "80", "refund:a");
        faults.set(1, 0);
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.FAILED, stored(r.getId()).getStatus());
        assertEquals("REFUND_AMOUNT_EXCEEDED", assertThrows(DomainException.class,
                () -> request(f, "30", "refund:b")).getCode());
        processor.process(r.getId(), true);
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
    }

    @Test
    void bankCommitSurvivesLocalRollbackAndIsRecoveredWithoutAnotherCredit() {
        Fixture f = fixture();
        var r = request(f, "100", "refund:recover");
        var claim = refunds.claim(r.getId(), false);
        var result = bank.refund(claim.idempotencyKey(), claim.debitReference(), claim.amount());
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            refunds.complete(r.getId(), result);
            throw new IllegalStateException("Simulate local commit failure");
        }));
        assertEquals(RefundStatusType.PROCESSING, stored(r.getId()).getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(paidBack(f)));
        assertEquals(0, money("500").compareTo(balance(f)));
        assertEquals(1, count("outbox_events"));
        clock.advance(61);
        processor.scan();
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals(0, money("100").compareTo(paidBack(f)));
        assertEquals(2, count("fakebank_transactions"));
        assertEquals(2, count("outbox_events"));
    }

    @Test
    void concurrentProcessorsApplyOneCreditAndOneSuccessEvent() throws Exception {
        Fixture f = fixture();
        var r = request(f, "100", "refund:workers");
        concurrently(() -> { processor.process(r.getId(), false); return true; },
                () -> { processor.process(r.getId(), false); return true; });
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals(0, money("100").compareTo(paidBack(f)));
        assertEquals(0, money("500").compareTo(balance(f)));
        assertEquals(2, count("outbox_events"));
        assertEquals(2, count("fakebank_transactions"));
    }

    @Test
    void latePaymentRefundRestoresExpiredBooking() {
        Fixture f = fixture();
        tx.executeWithoutResult(status -> {
            em.find(Booking.class, f.booking().getId()).setStatus(BookingStatusType.EXPIRED);
            Payment payment = em.find(Payment.class, f.payment().getId());
            payment.setStatus(PaymentStatusType.INITIATED);
            payment.setCapturedAmount(BigDecimal.ZERO);
            payment.setProviderReference(null);
        });
        paymentOutcomes.apply(f.payment().getId(), new PaymentOutcomeService.Outcome(true,
                bank.query(f.debitKey()).orElseThrow().reference(), null, "TEST_LATE_PAYMENT"));
        assertEquals("NEEDS_ATTENTION", bookingStatus(f));
        assertEquals(1, count("refund_requests"));
        processor.scan();
        assertEquals("EXPIRED", bookingStatus(f));
        assertEquals(RefundStatusType.SUCCEEDED, repository.findByIdempotencyKey("refund:late:" + f.payment().getId())
                .orElseThrow().getStatus());
        assertEquals(0, money("100").compareTo(paidBack(f)));
    }

    @Test
    void refundDoesNotResolveUnrelatedBookingAttention() {
        Fixture f = fixture();
        tx.executeWithoutResult(status -> em.find(Booking.class, f.booking().getId()).setStatus(BookingStatusType.NEEDS_ATTENTION));
        var r = refunds.request(f.booking(), f.payment(), money("100"), RefundService.KIND_LATE_PAYMENT,
                "Late", "refund:other-attention");
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.SUCCEEDED, stored(r.getId()).getStatus());
        assertEquals("NEEDS_ATTENTION", bookingStatus(f));
    }

    @Test
    void mismatchedBankAmountRequiresAttentionAndDoesNotChangeAccounting() {
        Fixture f = fixture();
        var r = request(f, "80", "refund:bad-result");
        refunds.claim(r.getId(), false);
        refunds.complete(r.getId(), new FakeBankService.TxResult("SUCCESS", "wrong-result", null, money("70")));
        assertEquals(RefundStatusType.NEEDS_ATTENTION, stored(r.getId()).getStatus());
        assertEquals("BANK_RESULT_MISMATCH", stored(r.getId()).getFailureCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(paidBack(f)));
        assertEquals("REFUND_PENDING", bookingStatus(f));
    }

    @Test
    void permanentBankFailureIsNotAutomaticallyRetried() {
        Fixture f = fixture();
        tx.executeWithoutResult(status -> em.find(Payment.class, f.payment().getId()).setProviderReference("missing-debit"));
        var r = request(f, "80", "refund:missing-original");
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.NEEDS_ATTENTION, stored(r.getId()).getStatus());
        assertEquals("ORIGINAL_NOT_FOUND", stored(r.getId()).getFailureCode());
        clock.advance(301);
        processor.scan();
        assertEquals(1, stored(r.getId()).getAttempts());
    }

    @Test
    void walletRequestIsNotSentToBank() {
        Fixture f = fixture();
        var r = request(f, "80", "refund:wallet");
        tx.executeWithoutResult(status -> em.find(RefundRequest.class, r.getId()).setRefundDestination(RefundDestinationType.WALLET));
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.NEEDS_ATTENTION, stored(r.getId()).getStatus());
        assertEquals(0, stored(r.getId()).getAttempts());
        assertEquals(0, money("400").compareTo(balance(f)));
        assertEquals(1, count("fakebank_transactions"));
    }

    @Test
    void existingCoreRefundOrUnexplainedCounterRequiresReconciliation() {
        Fixture f = fixture();
        tx.executeWithoutResult(status -> em.find(Payment.class, f.payment().getId()).setRefundedAmount(money("10")));
        assertEquals("REFUND_RECONCILIATION_REQUIRED", assertThrows(DomainException.class,
                () -> request(f, "10", "refund:counter")).getCode());
        tx.executeWithoutResult(status -> {
            Payment payment = em.find(Payment.class, f.payment().getId());
            payment.setRefundedAmount(BigDecimal.ZERO);
            em.persist(Refund.builder().payment(payment).booking(payment.getBooking()).refundCode("legacy")
                    .provider(PaymentProvider.FAKE_BANK).merchantId("datphong").idempotencyKey("legacy-key")
                    .requestHash("legacy-hash").amount(money("20")).currency("VND").reason("Legacy")
                    .correlationId(UUID.randomUUID().toString()).build());
        });
        assertEquals("REFUND_RECONCILIATION_REQUIRED", assertThrows(DomainException.class,
                () -> request(f, "10", "refund:core")).getCode());
        assertEquals(0, count("refund_requests"));
    }

    @Test
    void requestAndOutboxRollbackTogether() {
        Fixture f = fixture();
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            request(f, "100", "refund:rollback");
            throw new IllegalStateException("Rollback");
        }));
        assertEquals(0, count("refund_requests"));
        assertEquals(0, count("outbox_events"));
    }

    @Test
    void debitKeyCannotBeMistakenForSuccessfulRefund() {
        Fixture f = fixture();
        var r = request(f, "100", f.debitKey());
        processor.process(r.getId(), false);
        assertEquals(RefundStatusType.NEEDS_ATTENTION, stored(r.getId()).getStatus());
        assertEquals("IDEMPOTENCY_CONFLICT", stored(r.getId()).getFailureCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(paidBack(f)));
        assertEquals(0, money("400").compareTo(balance(f)));
    }

    @Test
    void bankRejectsSameKeyWithChangedAmountOrOriginalDebit() {
        Fixture f = fixture();
        String reference = bank.query(f.debitKey()).orElseThrow().reference();
        bank.refund("refund:key", reference, money("20"));
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(DomainException.class,
                () -> bank.refund("refund:key", reference, money("30"))).getCode());
        assertEquals("IDEMPOTENCY_CONFLICT", assertThrows(DomainException.class,
                () -> bank.refund("refund:key", "other-debit", money("20"))).getCode());
        assertEquals(0, money("420").compareTo(balance(f)));
    }

    private <T> List<T> concurrently(Callable<T> a, Callable<T> b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<T> first = pool.submit(() -> { start.await(); return a.call(); });
            Future<T> second = pool.submit(() -> { start.await(); return b.call(); });
            start.countDown();
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
