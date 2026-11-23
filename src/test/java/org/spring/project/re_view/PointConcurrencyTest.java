package org.spring.project.re_view;

import com.review.shop.exception.DatabaseException;
import com.review.shop.exception.WrongRequestException;
import com.review.shop.service.userinfo.other.PointService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Commits real transactions on REVIEWTEST. The class must not be transactional,
 * or the service calls would share the test transaction and release row locks too late.
 */
@SpringBootTest
@ActiveProfiles("test")
class PointConcurrencyTest {

    private static final String PREFIX = "point-concurrency-";
    private static final long LOCK_OBSERVATION_MILLIS = 2000L;

    @Autowired
    private PointService pointService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @BeforeEach
    void requireReviewTestDatabase() {
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String normalizedUrl = url.toLowerCase();
        assertTrue(normalizedUrl.contains(":1522") && normalizedUrl.contains("/reviewtest"), url);
        assertFalse(normalizedUrl.contains(":1521") || normalizedUrl.contains("xepdb1"), url);
        assertEquals("REVIEW_TEST", username);

        HikariDataSource pool = hikari();
        assertTrue(pool.getMaximumPoolSize() >= 5,
                "동시 커넥션을 받을 수 없는 풀 크기: " + pool.getMaximumPoolSize());
        System.out.println("POINT_TEST poolMaximum=" + pool.getMaximumPoolSize());
    }

    @AfterEach
    void removeLeftoverRows() {
        deleteByPrefix();
    }

    @Test
    @DisplayName("A-01 동일 사용자 700P 차감 두 건")
    void sameUserTwoDeductionsOf700() throws Exception {
        UserFixture user = insertUser(1000);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<CallResult> first = executor.submit(() -> usePoint(user, 700, barrier));
            Future<CallResult> second = executor.submit(() -> usePoint(user, 700, barrier));
            CallResult left = first.get(30, TimeUnit.SECONDS);
            CallResult right = second.get(30, TimeUnit.SECONDS);

            List<CallResult> results = List.of(left, right);
            long successes = results.stream().filter(CallResult::success).count();
            List<CallResult> failures = results.stream().filter(result -> !result.success()).toList();

            Integer balance = pointOf(user.userId());
            List<HistoryRow> history = historyOf(user.userId());
            System.out.println("POINT_TEST A-01 balance=" + balance
                    + " history=" + history
                    + " success=" + successes
                    + " failure=" + failures);

            assertEquals(1, successes, "성공 건수. results=" + results);
            assertEquals(1, failures.size(), "실패 건수. results=" + results);
            assertInsufficientPoints(failures.get(0).error());
            assertEquals(300, balance);
            assertEquals(List.of(new HistoryRow("USE", 700)), history);
        } finally {
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("A-02 동일 사용자 적립과 차감")
    void sameUserEarnAndUse() throws Exception {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<CallResult> use = executor.submit(() -> usePoint(user, 400, barrier));
            Future<CallResult> earn = executor.submit(() -> addReviewPoint(user, reviewId, barrier));
            CallResult useResult = use.get(30, TimeUnit.SECONDS);
            CallResult earnResult = earn.get(30, TimeUnit.SECONDS);

            Integer balance = pointOf(user.userId());
            List<HistoryRow> history = historyOf(user.userId());
            System.out.println("POINT_TEST A-02 balance=" + balance
                    + " history=" + history
                    + " use=" + useResult
                    + " earn=" + earnResult);

            assertTrue(useResult.success(), "차감 실패: " + useResult);
            assertTrue(earnResult.success(), "적립 실패: " + earnResult);
            assertEquals(700, balance);
            assertEquals(2, history.size(), history.toString());
            assertTrue(history.contains(new HistoryRow("USE", 400)), history.toString());
            assertTrue(history.contains(new HistoryRow("EARN", 100)), history.toString());
        } finally {
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("A-04 다른 트랜잭션의 행 락을 기다린다")
    void usePointWaitsForForeignRowLock() throws Exception {
        UserFixture target = insertUser(1000);
        UserFixture control = insertUser(500);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Connection holder = null;
        Future<CallResult> targetCall = null;
        try {
            holder = dataSource.getConnection();
            holder.setAutoCommit(false);
            lockUserRow(holder, target.userId(), 1000);

            targetCall = executor.submit(() -> usePoint(target, 100, null));
            waitUntilTargetHoldsConnection(targetCall);
            Thread.sleep(LOCK_OBSERVATION_MILLIS);
            assertFalse(targetCall.isDone(), "락을 유지하는 동안 대상 차감이 끝났습니다: " + resultNow(targetCall));
            assertEquals(0, threadsWaitingForConnection(), "대상 호출이 커넥션 풀 대기 중입니다.");
            assertTrue(hikari().getHikariPoolMXBean().getActiveConnections() >= 2,
                    "대상 호출이 행 락 대기가 아니라 시작 전에 멈춰 있습니다.");
            assertEquals(1000, pointOf(target.userId()));

            Future<CallResult> controlCall = executor.submit(() -> usePoint(control, 100, null));
            CallResult controlResult = controlCall.get(10, TimeUnit.SECONDS);
            assertTrue(controlResult.success(), "대조 사용자 차감 실패: " + controlResult);
            assertFalse(targetCall.isDone(), "대조 호출이 끝나기 전에 대상 차감이 락을 통과했습니다.");
            assertEquals(400, pointOf(control.userId()));
            assertEquals(1000, pointOf(target.userId()));

            holder.rollback();
            holder.close();
            holder = null;

            CallResult targetResult = targetCall.get(20, TimeUnit.SECONDS);
            Integer targetBalance = pointOf(target.userId());
            Integer controlBalance = pointOf(control.userId());
            List<HistoryRow> targetHistory = historyOf(target.userId());
            List<HistoryRow> controlHistory = historyOf(control.userId());
            System.out.println("POINT_TEST A-04 targetBalance=" + targetBalance
                    + " targetHistory=" + targetHistory
                    + " controlBalance=" + controlBalance
                    + " controlHistory=" + controlHistory
                    + " target=" + targetResult
                    + " control=" + controlResult);

            assertTrue(targetResult.success(), "락 해제 후 대상 차감 실패: " + targetResult);
            assertEquals(900, targetBalance);
            assertEquals(400, controlBalance);
            assertEquals(List.of(new HistoryRow("USE", 100)), targetHistory);
            assertEquals(List.of(new HistoryRow("USE", 100)), controlHistory);
        } finally {
            if (holder != null) {
                try {
                    holder.rollback();
                } catch (SQLException ignored) {
                    // The observation connection is closed below even if rollback fails.
                }
                try {
                    holder.close();
                } catch (SQLException ignored) {
                    // Closing returns the connection to the pool.
                }
            }
            if (targetCall != null && !targetCall.isDone()) {
                try {
                    targetCall.get(20, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException ignored) {
                    targetCall.cancel(true);
                }
            }
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("A-03 다른 사용자 차감은 행 잠금을 기다리지 않는다")
    void otherUserDoesNotWaitForRowLock() throws Exception {
        UserFixture userA = insertUser(1000);
        UserFixture userB = insertUser(500);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Connection holder = null;
        try {
            holder = dataSource.getConnection();
            holder.setAutoCommit(false);
            lockUserRow(holder, userA.userId(), 1000);

            long started = System.nanoTime();
            Future<CallResult> userBCall = executor.submit(() -> usePoint(userB, 100, null));
            CallResult userBResult = userBCall.get(5, TimeUnit.SECONDS);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertFalse(holder.isClosed(), "사용자 B 완료 전에 사용자 A의 잠금 커넥션이 닫혔습니다.");

            Integer balanceA = pointOf(userA.userId());
            Integer balanceB = pointOf(userB.userId());
            List<HistoryRow> historyA = historyOf(userA.userId());
            List<HistoryRow> historyB = historyOf(userB.userId());
            System.out.println("POINT_TEST A-03 elapsedMillis=" + elapsedMillis
                    + " balanceA=" + balanceA
                    + " historyA=" + historyA
                    + " balanceB=" + balanceB
                    + " historyB=" + historyB
                    + " userB=" + userBResult);

            assertTrue(userBResult.success(), "사용자 B 차감 실패: " + userBResult);
            assertEquals(400, balanceB);
            assertEquals(List.of(new HistoryRow("USE", 100)), historyB);
            assertEquals(1000, balanceA);
            assertEquals(List.of(), historyA);

            holder.rollback();
            holder.close();
            holder = null;
            assertEquals(1000, lockAndReadPoint(userA.userId()));
        } finally {
            if (holder != null) {
                try {
                    holder.rollback();
                } catch (SQLException ignored) {
                    // The lock connection is closed below even if rollback fails.
                }
                try {
                    holder.close();
                } catch (SQLException ignored) {
                    // Closing returns the connection to the pool.
                }
            }
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("A-05 NULL 잔액 사용자에게 리뷰 포인트를 적립한다")
    void earnPointWhenBalanceIsNull() {
        UserFixture user = insertUserWithNullPoint();
        assertNull(pointOf(user.userId()));
        int reviewId = insertReview(user);

        CallResult result = addReviewPoint(user, reviewId, null);
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_TEST A-05 initialPoint=null balance=" + balance
                + " history=" + history
                + " result=" + result);

        assertTrue(result.success(), "NULL 잔액 적립 실패: " + result);
        assertEquals(100, balance);
        assertEquals(List.of(new HistoryRow("EARN", 100)), history);
    }

    private CallResult usePoint(UserFixture user, int amount, CyclicBarrier barrier) {
        if (!pass(barrier)) {
            return CallResult.failed(new IllegalStateException("시작 장벽을 통과하지 못했습니다."));
        }
        return invoke(user.loginId(), () -> pointService.usePoint(user.userId(), amount));
    }

    private CallResult addReviewPoint(UserFixture user, int reviewId, CyclicBarrier barrier) {
        if (!pass(barrier)) {
            return CallResult.failed(new IllegalStateException("시작 장벽을 통과하지 못했습니다."));
        }
        return invoke(user.loginId(), () -> pointService.addReviewPoint(user.userId(), reviewId));
    }

    private boolean pass(CyclicBarrier barrier) {
        if (barrier == null) {
            return true;
        }
        try {
            barrier.await(20, TimeUnit.SECONDS);
            return true;
        } catch (Exception barrierFailure) {
            return false;
        }
    }

    private CallResult invoke(String loginId, ServiceCall call) {
        SecurityContextHolder.clearContext();
        try {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    loginId,
                    "test-only",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            call.run();
            return CallResult.succeeded();
        } catch (Throwable error) {
            return CallResult.failed(error);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertInsufficientPoints(Throwable error) {
        assertNotNull(error);
        boolean insufficient = false;
        boolean connectionOrLock = false;
        StringBuilder chain = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            chain.append(current.getClass().getName()).append(": ").append(current.getMessage()).append(" || ");
            if (current instanceof WrongRequestException && "포인트가 부족합니다.".equals(current.getMessage())) {
                insufficient = true;
            }
            if (current instanceof SQLException || isConnectionOrLock(current)) {
                connectionOrLock = true;
            }
        }
        assertFalse(connectionOrLock, "락 대기나 연결 오류를 잔액 부족으로 판정했습니다: " + chain);
        assertTrue(error instanceof DatabaseException, chain.toString());
        assertTrue(insufficient, chain.toString());
    }

    private boolean isConnectionOrLock(Throwable error) {
        String type = error.getClass().getName();
        String message = String.valueOf(error.getMessage());
        return type.contains("CannotGetJdbcConnection")
                || type.contains("QueryTimeout")
                || type.contains("PessimisticLock")
                || type.contains("TransientDataAccess")
                || message.contains("ORA-00054")
                || message.contains("ORA-30006")
                || message.contains("ORA-01013")
                || message.contains("ORA-17002")
                || message.contains("ORA-17008");
    }

    private void lockUserRow(Connection holder, int userId, int expectedPoint) throws SQLException {
        try (PreparedStatement statement = holder.prepareStatement(
                "SELECT point FROM user_table WHERE user_id = ? FOR UPDATE")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next(), "잠글 사용자 행이 없습니다.");
                assertEquals(expectedPoint, rows.getInt("point"));
                assertFalse(rows.wasNull());
            }
        }
    }

    private void waitUntilTargetHoldsConnection(Future<CallResult> targetCall) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (targetCall.isDone()) {
                fail("락을 유지하는 동안 대상 차감이 끝났습니다: " + targetCall.get());
            }
            HikariPoolMXBean pool = hikari().getHikariPoolMXBean();
            if (pool != null && pool.getActiveConnections() >= 2 && pool.getThreadsAwaitingConnection() == 0) {
                return;
            }
            Thread.sleep(50);
        }
        fail("대상 호출이 10초 안에 커넥션을 잡고 행 락에서 대기하지 않았습니다.");
    }

    private String resultNow(Future<CallResult> call) {
        if (!call.isDone()) {
            return "미완료";
        }
        try {
            return String.valueOf(call.get());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return interrupted.toString();
        } catch (ExecutionException execution) {
            return execution.toString();
        }
    }

    private int threadsWaitingForConnection() {
        HikariPoolMXBean pool = hikari().getHikariPoolMXBean();
        assertNotNull(pool, "풀 사용 현황을 아직 읽을 수 없습니다.");
        return pool.getThreadsAwaitingConnection();
    }

    private HikariDataSource hikari() {
        DataSource current = dataSource;
        if (current instanceof DelegatingDataSource delegating) {
            current = delegating.getTargetDataSource();
        }
        if (current instanceof HikariDataSource hikari) {
            return hikari;
        }
        fail("Hikari 풀이 아닙니다: " + current.getClass().getName());
        return null;
    }

    private UserFixture insertUser(int point) {
        Long baumannMax = jdbc.queryForObject("SELECT NVL(MAX(baumann_id), 0) FROM baumann", Long.class);
        int baumannId = Math.toIntExact(baumannMax + 1);
        String token = PREFIX + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO baumann (baumann_id, type, first, second, third, fourth)
                VALUES (?, ?, 'O', 'R', 'N', 'T')
                """, baumannId, token);
        jdbc.update("""
                INSERT INTO user_table (baumann_id, password, id, name, email, nickname, role, point)
                VALUES (?, 'test-only', ?, 'tester', ?, 'ptest', 'USER', ?)
                """, baumannId, token, token + "@test.local", point);
        Integer userId = jdbc.queryForObject("SELECT user_id FROM user_table WHERE id = ?", Integer.class, token);
        assertNotNull(userId);
        return new UserFixture(userId, token, baumannId);
    }

    private UserFixture insertUserWithNullPoint() {
        Long baumannMax = jdbc.queryForObject("SELECT NVL(MAX(baumann_id), 0) FROM baumann", Long.class);
        int baumannId = Math.toIntExact(baumannMax + 1);
        String token = PREFIX + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO baumann (baumann_id, type, first, second, third, fourth)
                VALUES (?, ?, 'O', 'R', 'N', 'T')
                """, baumannId, token);
        jdbc.update("""
                INSERT INTO user_table (baumann_id, password, id, name, email, nickname, role)
                VALUES (?, 'test-only', ?, 'tester', ?, 'ptest', 'USER')
                """, baumannId, token, token + "@test.local");
        Integer userId = jdbc.queryForObject("SELECT user_id FROM user_table WHERE id = ?", Integer.class, token);
        assertNotNull(userId);
        assertNull(pointOf(userId));
        return new UserFixture(userId, token, baumannId);
    }

    private int lockAndReadPoint(int userId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT point FROM user_table WHERE user_id = ? FOR UPDATE")) {
                statement.setQueryTimeout(5);
                statement.setInt(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next(), "잠금 해제 후 사용자 행을 읽지 못했습니다.");
                    int point = rows.getInt("point");
                    assertFalse(rows.wasNull());
                    return point;
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private int insertReview(UserFixture user) {
        String productName = PREFIX + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (prd_name, prd_brand, ingredient, price, category, baumann_id)
                VALUES (?, 'test-brand', 'test-ingredient', 1000, 'test', ?)
                """, productName, user.baumannId());
        Integer productId = jdbc.queryForObject(
                "SELECT product_id FROM product WHERE prd_name = ?", Integer.class, productName);
        assertNotNull(productId);
        jdbc.update("""
                INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at)
                VALUES (?, ?, ?, 5, 0, '0', SYSDATE)
                """, productId, user.userId(), productName);
        Integer reviewId = jdbc.queryForObject(
                "SELECT review_id FROM review WHERE product_id = ? AND user_id = ?",
                Integer.class, productId, user.userId());
        assertNotNull(reviewId);
        return reviewId;
    }

    private Integer pointOf(int userId) {
        return jdbc.queryForObject("SELECT point FROM user_table WHERE user_id = ?", Integer.class, userId);
    }

    private List<HistoryRow> historyOf(int userId) {
        return jdbc.query("""
                SELECT type, amount
                FROM point_history
                WHERE user_id = ?
                ORDER BY point_history_id
                """, (rs, row) -> new HistoryRow(rs.getString("type"), rs.getInt("amount")), userId);
    }

    private void deleteByPrefix() {
        List<Integer> userIds = jdbc.queryForList(
                "SELECT user_id FROM user_table WHERE id LIKE ?", Integer.class, PREFIX + "%");
        List<Integer> productIds = jdbc.queryForList(
                "SELECT product_id FROM product WHERE prd_name LIKE ?", Integer.class, PREFIX + "%");
        if (!userIds.isEmpty()) {
            deleteIn("DELETE FROM point_history WHERE user_id IN (%s)", userIds);
            deleteIn("DELETE FROM review WHERE user_id IN (%s)", userIds);
            deleteIn("DELETE FROM user_table WHERE user_id IN (%s)", userIds);
        }
        if (!productIds.isEmpty()) {
            deleteIn("DELETE FROM review WHERE product_id IN (%s)", productIds);
            deleteIn("DELETE FROM product WHERE product_id IN (%s)", productIds);
        }
        jdbc.update("DELETE FROM baumann WHERE type LIKE ?", PREFIX + "%");
    }

    private void deleteIn(String sql, List<Integer> ids) {
        String placeholders = String.join(", ", ids.stream().map(id -> "?").toList());
        jdbc.update(String.format(sql, placeholders), ids.toArray());
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdownNow();
        try {
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS), "서비스 스레드가 종료되지 않았습니다.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail("스레드 정리 중 인터럽트가 발생했습니다.");
        }
    }

    @FunctionalInterface
    private interface ServiceCall {
        void run();
    }

    private record UserFixture(int userId, String loginId, int baumannId) {
    }

    private record HistoryRow(String type, int amount) {
    }

    private record CallResult(boolean success, Throwable error) {
        static CallResult succeeded() {
            return new CallResult(true, null);
        }

        static CallResult failed(Throwable error) {
            return new CallResult(false, error);
        }

        @Override
        public String toString() {
            if (success) {
                return "success";
            }
            List<String> chain = new ArrayList<>();
            for (Throwable current = error; current != null; current = current.getCause()) {
                chain.add(current.getClass().getSimpleName() + ": " + current.getMessage());
            }
            return String.join(" <- ", chain);
        }
    }
}
