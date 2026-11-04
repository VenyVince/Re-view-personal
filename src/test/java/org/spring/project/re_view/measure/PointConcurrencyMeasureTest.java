package org.spring.project.re_view.measure;

import com.review.shop.service.userinfo.other.PointService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class PointConcurrencyMeasureTest {

    private static final int USER_ID = 1;
    private static final String LOGIN_ID = "measure_user";
    private static final int EARN_REVIEW_ID = 1;
    private static final int BEST_REVIEW_ID = 2;
    private static final int START_BALANCE = 10_000;
    private static final int AMOUNT = 100;
    private static final int BEST_AMOUNT = 700;
    private static final int OPERATIONS = 20;
    private static final int ROUNDS = 5;
    private static final int BEST_THREADS = 10;

    @Autowired
    private PointService pointService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentEarnAndUseThenBestAward() throws Exception {
        resetBalance();
        runAsUser(() -> {
            pointService.addReviewPoint(USER_ID, EARN_REVIEW_ID);
            pointService.usePoint(USER_ID, AMOUNT);
        });
        assertEquals(START_BALANCE, point());
        assertEquals(START_BALANCE, historyNet());

        List<Boolean> earnFlags = mixedOperations();
        int balanceHistoryMismatches = 0;
        StringBuilder rounds = new StringBuilder();

        for (int round = 1; round <= ROUNDS; round++) {
            resetBalance();
            AtomicInteger earnSuccess = new AtomicInteger();
            AtomicInteger useSuccess = new AtomicInteger();
            List<String> failures = Collections.synchronizedList(new ArrayList<>());
            runTogether(earnFlags.size(), index -> () -> {
                boolean earn = earnFlags.get(index);
                runAsUser(() -> {
                    if (earn) {
                        pointService.addReviewPoint(USER_ID, EARN_REVIEW_ID);
                        earnSuccess.incrementAndGet();
                    } else {
                        pointService.usePoint(USER_ID, AMOUNT);
                        useSuccess.incrementAndGet();
                    }
                });
                return null;
            }, failures);

            int expected = START_BALANCE + earnSuccess.get() * AMOUNT - useSuccess.get() * AMOUNT;
            int point = point();
            int history = historyNet();
            if (point != history) {
                balanceHistoryMismatches++;
            }
            rounds.append("round ").append(round)
                    .append(" earnSuccess=").append(earnSuccess.get())
                    .append(" useSuccess=").append(useSuccess.get())
                    .append(" failures=").append(failures.size())
                    .append(" expected=").append(expected)
                    .append(" point=").append(point)
                    .append(" history=").append(history)
                    .append('\n');
            if (!failures.isEmpty()) {
                rounds.append(String.join("\n", failures)).append('\n');
            }
            assertEquals(OPERATIONS, earnSuccess.get() + useSuccess.get(), rounds.toString());
        }

        jdbcTemplate.update("""
                DELETE FROM point_history
                WHERE user_id = ? AND review_id = ? AND amount = ? AND type = 'EARN'
                """, USER_ID, BEST_REVIEW_ID, BEST_AMOUNT);
        List<String> bestFailures = Collections.synchronizedList(new ArrayList<>());
        runTogether(BEST_THREADS, index -> () -> {
            runAsUser(() -> pointService.addBestReviewPoint(USER_ID, BEST_REVIEW_ID));
            return null;
        }, bestFailures);
        int bestRows = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM point_history
                WHERE user_id = ? AND review_id = ? AND amount = ? AND type = 'EARN'
                """, Integer.class, USER_ID, BEST_REVIEW_ID, BEST_AMOUNT);

        String report = rounds
                + "balanceHistoryMismatches=" + balanceHistoryMismatches
                + " bestRows=" + bestRows
                + " bestFailures=" + bestFailures.size()
                + (bestFailures.isEmpty() ? "" : "\n" + String.join("\n", bestFailures));
        System.out.println(report);
        assertTrue(bestRows >= 0, report);
    }

    private List<Boolean> mixedOperations() {
        List<Boolean> flags = new ArrayList<>();
        for (int i = 0; i < OPERATIONS / 2; i++) {
            flags.add(true);
            flags.add(false);
        }
        Collections.shuffle(flags, new Random(20261104));
        return flags;
    }

    private void resetBalance() {
        jdbcTemplate.update("DELETE FROM point_history WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("UPDATE user_table SET point = ? WHERE user_id = ?", START_BALANCE, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO point_history (user_id, amount, type, description)
                VALUES (?, ?, 'EARN', '시드 잔액')
                """, USER_ID, START_BALANCE);
    }

    private int point() {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT point FROM user_table WHERE user_id = ?", Integer.class, USER_ID);
        return value == null ? -1 : value;
    }

    private int historyNet() {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT NVL(SUM(CASE
                    WHEN type = 'EARN' THEN amount
                    WHEN type = 'USE' THEN -amount
                    ELSE 0 END), 0)
                FROM point_history
                WHERE user_id = ?
                """, Integer.class, USER_ID);
        return value == null ? -1 : value;
    }

    private void runTogether(int count, TaskAt taskAt, List<String> failures) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Callable<Void> call = taskAt.at(i);
                futures.add(executor.submit(() -> {
                    start.await(30, TimeUnit.SECONDS);
                    try {
                        call.call();
                    } catch (Exception exception) {
                        failures.add(exception.getClass().getSimpleName() + ": " + exception.getMessage());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void runAsUser(Runnable action) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                LOGIN_ID,
                "measure",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @FunctionalInterface
    private interface TaskAt {
        Callable<Void> at(int index);
    }
}
