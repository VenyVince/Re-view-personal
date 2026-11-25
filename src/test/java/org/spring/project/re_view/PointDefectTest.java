package org.spring.project.re_view;

import com.review.shop.exception.DatabaseException;
import com.review.shop.exception.ResourceNotFoundException;
import com.review.shop.exception.WrongRequestException;
import com.review.shop.service.admin.AdminReviewService;
import com.review.shop.service.admin.AdminUserService;
import com.review.shop.service.userinfo.other.PointService;
import com.review.shop.util.ReviewScheduler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
class PointDefectTest {

    private static final String PREFIX = "point-defect-";
    private static final int MISSING_USER_ID = -1;
    private static final String ADMIN_HISTORY_PROBE = "CHK_C04_ADMIN_HIST_TEST";

    @Autowired
    private PointService pointService;

    @Autowired
    private AdminReviewService adminReviewService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private ReviewScheduler reviewScheduler;

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
        dropAdminHistoryProbe();
        assertEquals("REVIEW_TEST", username);
    }

    @AfterEach
    void removeRowsCreatedByThisClass() {
        dropAdminHistoryProbe();
        deleteByPrefix();
    }

    @Test
    @DisplayName("C-01-1 존재하지 않는 사용자 포인트 적립")
    void earnForMissingUserDoesNotWriteHistory() {
        UserFixture actor = insertUser(1000);
        int reviewId = insertReview(actor, 0, null);
        assertEquals(0, count("SELECT COUNT(*) FROM user_table WHERE user_id = ?", MISSING_USER_ID));
        String userForeignKey = constraintStatus("FK_USER_TO_POINT_HISTORY");
        assertEquals("DISABLED", userForeignKey);

        Throwable error = call(actor, () -> pointService.addReviewPoint(MISSING_USER_ID, reviewId));
        int missingUsers = count("SELECT COUNT(*) FROM user_table WHERE user_id = ?", MISSING_USER_ID);
        List<HistoryRow> orphanHistory = historyOf(MISSING_USER_ID);
        System.out.println("POINT_DEFECT C-01-1 fk=" + userForeignKey
                + " missingUsers=" + missingUsers
                + " orphanHistory=" + orphanHistory
                + " actorPoint=" + pointOf(actor.userId())
                + " actorHistory=" + historyOf(actor.userId())
                + " error=" + chain(error));

        assertNotNull(error);
        assertTrue(error instanceof ResourceNotFoundException, chain(error).toString());
        assertTrue(chainContains(error, "포인트를 변경할 회원을 찾을 수 없습니다."), chain(error).toString());
        assertEquals(0, missingUsers);
        assertEquals(List.of(), orphanHistory);
        assertEquals(1000, pointOf(actor.userId()));
        assertEquals(List.of(), historyOf(actor.userId()));
    }

    @Test
    @DisplayName("C-01-2 존재하는 사용자 정상 적립")
    void earnForExistingUserUpdatesBalanceAndHistory() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);
        Throwable error = call(user, () -> pointService.addReviewPoint(user.userId(), reviewId));
        Integer balance = pointOf(user.userId());
        List<AdminHistory> history = adminHistoryOf(user.userId());
        System.out.println("POINT_DEFECT C-01-2 balance=" + balance + " history=" + history + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1100, balance);
        assertEquals(List.of(new AdminHistory("EARN", 100, "리뷰 작성 보상")), history);
    }

    @Test
    @DisplayName("C-01-3 이력 저장 실패 시 잔액 적립이 롤백된다")
    void earnRollsBackWhenHistoryInsertFails() {
        assertEquals("ENABLED", constraintStatus("FK_POINT_HISTORY_REVIEW"));
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> pointService.addReviewPoint(user.userId(), -1));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-01-3 balance=" + balance
                + " history=" + history
                + " error=" + chain(error));

        assertNotNull(error);
        assertTrue(error instanceof DatabaseException, chain(error).toString());
        assertTrue(chainContains(error, "ORA-02291") || chainContains(error, "FK_POINT_HISTORY_REVIEW"),
                chain(error).toString());
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-01-4 NULL 잔액 사용자 적립")
    void earnForNullBalanceTreatsPointAsZero() {
        UserFixture user = insertUserWithNullPoint();
        assertNull(pointOf(user.userId()));
        int reviewId = insertReview(user, 0, null);
        Throwable error = call(user, () -> pointService.addReviewPoint(user.userId(), reviewId));
        Integer balance = pointOf(user.userId());
        List<AdminHistory> history = adminHistoryOf(user.userId());
        System.out.println("POINT_DEFECT C-01-4 balance=" + balance + " history=" + history + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(100, balance);
        assertEquals(List.of(new AdminHistory("EARN", 100, "리뷰 작성 보상")), history);
    }

    @Test
    @DisplayName("C-02 베스트 리뷰 중복 지급 경쟁")
    void bestReviewAwardRacesPastTheDuplicateCheck() throws Exception {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Connection holder = null;
        try {
            holder = dataSource.getConnection();
            holder.setAutoCommit(false);
            lockPoint(holder, user.userId());

            Future<CallResult> first = executor.submit(() -> bestAward(user, reviewId));
            Future<CallResult> second = executor.submit(() -> bestAward(user, reviewId));
            boolean raceEstablished = waitUntilBothWaitOnUserLock(first, second, user.userId());
            if (!raceEstablished) {
                fail("경쟁 상태를 만들지 못했습니다. firstDone=" + first.isDone()
                        + " secondDone=" + second.isDone()
                        + " history=" + historyOf(user.userId())
                        + " active=" + activeConnections()
                        + " waitingForPool=" + threadsWaitingForConnection());
            }

            holder.rollback();
            holder.close();
            holder = null;

            CallResult left = first.get(20, TimeUnit.SECONDS);
            CallResult right = second.get(20, TimeUnit.SECONDS);
            Integer balance = pointOf(user.userId());
            List<HistoryRow> history = historyOf(user.userId());
            int earned = history.stream().filter(row -> "EARN".equals(row.type())).mapToInt(HistoryRow::amount).sum();
            System.out.println("POINT_DEFECT C-02 balance=" + balance
                    + " history=" + history
                    + " earnedSum=" + earned
                    + " first=" + left
                    + " second=" + right);

            assertTrue(left.success(), "첫 지급 실패: " + left);
            assertTrue(right.success(), "둘째 지급 실패: " + right);
            assertEquals(1700, balance);
            assertEquals(List.of(new HistoryRow("EARN", 700)), history);
            assertEquals(700, earned);
        } finally {
            release(holder);
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("C-03-1 같은 리뷰 반복 채택")
    void repeatedSelectionPaysTwice() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);

        Throwable first = call(user, () -> adminReviewService.setReviewSelection(reviewId, 1));
        Integer balanceAfterFirst = pointOf(user.userId());
        List<HistoryRow> historyAfterFirst = historyOf(user.userId());
        Throwable second = call(user, () -> adminReviewService.setReviewSelection(reviewId, 1));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-03-1 afterFirst=" + balanceAfterFirst
                + " historyAfterFirst=" + historyAfterFirst
                + " afterSecond=" + balance
                + " history=" + history
                + " selected=" + selected(reviewId)
                + " first=" + chain(first)
                + " second=" + chain(second));

        assertNull(first, chain(first).toString());
        assertEquals("1", selected(reviewId));
        assertEquals(1500, balanceAfterFirst);
        assertEquals(List.of(new HistoryRow("EARN", 500)), historyAfterFirst);
        assertNull(second, chain(second).toString());
        assertEquals(1500, balance);
        assertEquals(List.of(new HistoryRow("EARN", 500)), history);
    }

    @Test
    @DisplayName("C-03-2 채택 해제 요청")
    void clearingSelectionStillPays() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);
        jdbc.update("UPDATE review SET is_selected = '1' WHERE review_id = ?", reviewId);

        Throwable error = call(user, () -> adminReviewService.setReviewSelection(reviewId, 0));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-03-2 balance=" + balance
                + " history=" + history
                + " selected=" + selected(reviewId)
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals("0", selected(reviewId));
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-03-3 이미 해제된 리뷰를 다시 해제")
    void clearingSelectionAgainDoesNotPay() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);

        Throwable error = call(user, () -> adminReviewService.setReviewSelection(reviewId, 0));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-03-3 balance=" + balance
                + " history=" + history
                + " selected=" + selected(reviewId)
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals("0", selected(reviewId));
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-03-4 없거나 삭제된 리뷰 채택")
    void missingOrDeletedSelectionIsNotFound() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);

        Throwable missing = call(user, () -> adminReviewService.setReviewSelection(-1, 1));
        assertTrue(missing instanceof ResourceNotFoundException, chain(missing).toString());
        assertTrue(chainContains(missing, "설정할 리뷰를 찾을 수 없습니다."), chain(missing).toString());

        jdbc.update("UPDATE review SET deleted_at = SYSDATE WHERE review_id = ?", reviewId);
        Throwable deleted = call(user, () -> adminReviewService.setReviewSelection(reviewId, 1));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-03-4 missing=" + chain(missing)
                + " deleted=" + chain(deleted)
                + " balance=" + balance
                + " history=" + history
                + " selected=" + selected(reviewId));

        assertTrue(deleted instanceof ResourceNotFoundException, chain(deleted).toString());
        assertTrue(chainContains(deleted, "설정할 리뷰를 찾을 수 없습니다."), chain(deleted).toString());
        assertEquals("0", selected(reviewId));
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-03-5 채택 포인트 실패 시 상태 롤백")
    void selectionRollsBackWhenPointAwardFails() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);
        SecurityContextHolder.clearContext();

        Throwable error;
        try {
            adminReviewService.setReviewSelection(reviewId, 1);
            error = null;
        } catch (Throwable thrown) {
            error = thrown;
        } finally {
            SecurityContextHolder.clearContext();
        }

        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-03-5 balance=" + balance
                + " history=" + history
                + " selected=" + selected(reviewId)
                + " error=" + chain(error));

        assertNotNull(error);
        assertTrue(error instanceof DatabaseException, chain(error).toString());
        assertTrue(hasCause(error, WrongRequestException.class), chain(error).toString());
        assertTrue(chainContains(error, "로그인이 필요합니다."), chain(error).toString());
        assertEquals("0", selected(reviewId));
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-04 오래된 기준 잔액은 거절된다")
    void adminStaleExpectedPointsIsRejected() {
        UserFixture user = insertUser(1000);
        int readByAdmin = callValue(user, () -> adminUserService.getMemberPoints(user.userId()));
        assertEquals(1000, readByAdmin);

        Throwable useError = call(user, () -> pointService.usePoint(user.userId(), 700));
        int balanceAfterUse = pointOf(user.userId());
        List<HistoryRow> historyAfterUse = historyOf(user.userId());
        assertNull(useError, chain(useError).toString());
        assertEquals(300, balanceAfterUse);
        assertEquals(List.of(new HistoryRow("USE", 700)), historyAfterUse);

        Throwable adminError = call(user, () ->
                adminUserService.updateMemberPoints(user.userId(), readByAdmin, readByAdmin));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-04 read=" + readByAdmin
                + " afterUse=" + balanceAfterUse
                + " afterAdmin=" + balance
                + " history=" + history
                + " adminError=" + chain(adminError));

        assertTrue(adminError instanceof WrongRequestException, chain(adminError).toString());
        assertTrue(chainContains(adminError, "현재 잔액이 요청한 기준 잔액과 다릅니다."), chain(adminError).toString());
        assertEquals(300, balance);
        assertEquals(List.of(new HistoryRow("USE", 700)), history);
    }

    @Test
    @DisplayName("C-04-1 기준 잔액이 맞으면 증가분만큼 적립한다")
    void adminIncreaseWritesEarnHistory() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 1500));
        System.out.println("POINT_DEFECT C-04-1 balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1500, pointOf(user.userId()));
        assertEquals(List.of(new AdminHistory("EARN", 500, "관리자 잔액 수정")), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-2 기준 잔액이 맞으면 감소분만큼 사용 이력을 남긴다")
    void adminDecreaseWritesUseHistory() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 700));
        System.out.println("POINT_DEFECT C-04-2 balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(700, pointOf(user.userId()));
        assertEquals(List.of(new AdminHistory("USE", 300, "관리자 잔액 수정")), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-2b 목표 잔액 0은 전액 사용으로 기록한다")
    void adminCanSetBalanceToZero() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 0));
        System.out.println("POINT_DEFECT C-04-2b balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(0, pointOf(user.userId()));
        assertEquals(List.of(new AdminHistory("USE", 1000, "관리자 잔액 수정")), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-3 목표 잔액이 같으면 잔액과 이력을 바꾸지 않는다")
    void adminSameBalanceWritesNothing() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 1000));
        System.out.println("POINT_DEFECT C-04-3 balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1000, pointOf(user.userId()));
        assertEquals(List.of(), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-4 음수 목표 잔액은 거절된다")
    void adminNegativeTargetIsRejected() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, -100));
        System.out.println("POINT_DEFECT C-04-4 balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertTrue(error instanceof WrongRequestException, chain(error).toString());
        assertTrue(chainContains(error, "포인트는 0 이상이어야 합니다."), chain(error).toString());
        assertEquals(1000, pointOf(user.userId()));
        assertEquals(List.of(), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-5 기준 잔액이 다르면 잔액과 이력을 바꾸지 않는다")
    void adminMismatchedExpectedPointsIsRejected() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 700, 1500));
        System.out.println("POINT_DEFECT C-04-5 balance=" + pointOf(user.userId())
                + " history=" + adminHistoryOf(user.userId())
                + " error=" + chain(error));

        assertTrue(error instanceof WrongRequestException, chain(error).toString());
        assertTrue(chainContains(error, "현재 잔액이 요청한 기준 잔액과 다릅니다."), chain(error).toString());
        assertEquals(1000, pointOf(user.userId()));
        assertEquals(List.of(), adminHistoryOf(user.userId()));
    }

    @Test
    @DisplayName("C-04-6 이력 저장 실패 시 잔액 수정도 롤백된다")
    void adminHistoryInsertFailureRollsBalanceBack() {
        UserFixture user = insertUser(1000);
        jdbc.execute("""
                ALTER TABLE point_history ADD CONSTRAINT chk_c04_admin_hist_test
                CHECK (description IS NULL OR description <> '관리자 잔액 수정')
                ENABLE NOVALIDATE
                """);
        try {
            Throwable error = call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 1500));
            Integer balance = pointOf(user.userId());
            List<AdminHistory> history = adminHistoryOf(user.userId());
            System.out.println("POINT_DEFECT C-04-6 balance=" + balance
                    + " history=" + history
                    + " error=" + chain(error));

            assertNotNull(error);
            assertTrue(chainContains(error, "ORA-02290") || chainContains(error, "CHK_C04_ADMIN_HIST_TEST"),
                    chain(error).toString());
            assertEquals(1000, balance);
            assertEquals(List.of(), history);
        } finally {
            dropAdminHistoryProbe();
        }
        Integer leftover = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_constraints WHERE constraint_name = ?",
                Integer.class, ADMIN_HISTORY_PROBE);
        assertEquals(0, leftover);
    }

    @Test
    @DisplayName("C-04-7 같은 기준 잔액의 동시 수정은 하나만 성공한다")
    void oneOfTwoAdminUpdatesWithTheSameExpectedBalanceSucceeds() throws Exception {
        UserFixture user = insertUser(1000);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Connection holder = dataSource.getConnection();
        holder.setAutoCommit(false);
        try {
            lockPoint(holder, user.userId());
            Future<Throwable> increase = executor.submit(() ->
                    call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 1500)));
            Future<Throwable> decrease = executor.submit(() ->
                    call(user, () -> adminUserService.updateMemberPoints(user.userId(), 1000, 700)));
            assertTrue(waitUntilBothWaitOnUserLock(increase, decrease, user.userId()),
                    "두 요청이 사용자 행 잠금에서 대기하지 않았습니다.");
            holder.rollback();

            Throwable increaseError = increase.get(20, TimeUnit.SECONDS);
            Throwable decreaseError = decrease.get(20, TimeUnit.SECONDS);
            Integer balance = pointOf(user.userId());
            List<AdminHistory> history = adminHistoryOf(user.userId());
            System.out.println("POINT_DEFECT C-04-7 balance=" + balance
                    + " history=" + history
                    + " increaseError=" + chain(increaseError)
                    + " decreaseError=" + chain(decreaseError));

            boolean increaseWon = increaseError == null;
            boolean decreaseWon = decreaseError == null;
            assertTrue(increaseWon ^ decreaseWon, "성공은 한 건이어야 합니다. increase="
                    + chain(increaseError) + " decrease=" + chain(decreaseError));
            Throwable failure = increaseWon ? decreaseError : increaseError;
            assertTrue(failure instanceof WrongRequestException, chain(failure).toString());
            assertTrue(chainContains(failure, "현재 잔액이 요청한 기준 잔액과 다릅니다."), chain(failure).toString());
            if (increaseWon) {
                assertEquals(1500, balance);
                assertEquals(List.of(new AdminHistory("EARN", 500, "관리자 잔액 수정")), history);
            } else {
                assertEquals(700, balance);
                assertEquals(List.of(new AdminHistory("USE", 300, "관리자 잔액 수정")), history);
            }
        } finally {
            release(holder);
            shutdown(executor);
        }
    }

    @Test
    @DisplayName("C-05-1 0 포인트 차감")
    void zeroUseIsAccepted() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> pointService.usePoint(user.userId(), 0));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-05-1 balance=" + balance + " history=" + history + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-05-2 음수 포인트 차감")
    void negativeUseIncreasesBalance() {
        UserFixture user = insertUser(1000);
        Throwable error = call(user, () -> pointService.usePoint(user.userId(), -100));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-05-2 balance=" + balance + " history=" + history + " error=" + chain(error));

        assertTrue(error instanceof WrongRequestException, chain(error).toString());
        assertTrue(chainContains(error, "사용할 포인트는 0 이상이어야 합니다."), chain(error).toString());
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    @Test
    @DisplayName("C-06 인증 없는 스케줄러 지급")
    void schedulerWithoutAuthenticationAwardsBestReview() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 21, 31);
        assertTrue(reviewIsBestCandidate(reviewId), "베스트 선정 SQL 조건을 만족하지 않습니다.");
        assertEquals("0", checked(reviewId));
        SecurityContextHolder.clearContext();
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        Throwable error;
        try {
            reviewScheduler.updateBestReviewMonthly();
            error = null;
        } catch (Throwable thrown) {
            error = thrown;
        } finally {
            SecurityContextHolder.clearContext();
        }

        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        String checked = checked(reviewId);
        System.out.println("POINT_DEFECT C-06 balance=" + balance
                + " history=" + history
                + " checked=" + checked
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1700, balance);
        assertEquals(List.of(new HistoryRow("EARN", 700)), history);
        assertEquals("1", checked);
    }

    @Test
    @DisplayName("C-06-U 다른 포인트 경로는 인증이 필요하다")
    void otherPointChangesStillRequireAuthentication() {
        UserFixture user = insertUser(1000);
        int reviewId = insertReview(user, 0, null);
        SecurityContextHolder.clearContext();

        Throwable useError = invokeWithoutAuthentication(() -> pointService.usePoint(user.userId(), 100));
        Throwable earnError = invokeWithoutAuthentication(() -> pointService.addReviewPoint(user.userId(), reviewId));
        Throwable removeError = invokeWithoutAuthentication(() -> pointService.removeReviewPoint(user.userId(), reviewId));
        Throwable selectedError = invokeWithoutAuthentication(
                () -> pointService.addSelectedReviewPoint(user.userId(), reviewId));
        Integer balance = pointOf(user.userId());
        List<HistoryRow> history = historyOf(user.userId());
        System.out.println("POINT_DEFECT C-06-U use=" + chain(useError)
                + " earn=" + chain(earnError)
                + " remove=" + chain(removeError)
                + " selected=" + chain(selectedError)
                + " balance=" + balance
                + " history=" + history);

        assertLoginRequired(useError);
        assertLoginRequired(earnError);
        assertLoginRequired(removeError);
        assertLoginRequired(selectedError);
        assertEquals(1000, balance);
        assertEquals(List.of(), history);
    }

    private Throwable invokeWithoutAuthentication(Runnable action) {
        SecurityContextHolder.clearContext();
        try {
            action.run();
            return null;
        } catch (Throwable error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertLoginRequired(Throwable error) {
        assertNotNull(error, "인증 없이 포인트 변경이 성공했습니다.");
        assertTrue(error instanceof DatabaseException, chain(error).toString());
        assertTrue(hasCause(error, WrongRequestException.class), chain(error).toString());
        assertTrue(chainContains(error, "로그인이 필요합니다."), chain(error).toString());
    }

    private boolean waitUntilBothWaitOnUserLock(Future<?> first, Future<?> second, int userId)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (first.isDone() || second.isDone()) {
                return false;
            }
            boolean waitingOnRow = activeConnections() >= 3
                    && threadsWaitingForConnection() == 0
                    && historyOf(userId).isEmpty();
            if (waitingOnRow) {
                Thread.sleep(500);
                return !first.isDone() && !second.isDone() && historyOf(userId).isEmpty();
            }
            Thread.sleep(50);
        }
        return false;
    }

    private CallResult bestAward(UserFixture user, int reviewId) {
        SecurityContextHolder.clearContext();
        try {
            authenticate(user);
            pointService.addBestReviewPoint(user.userId(), reviewId);
            return CallResult.succeeded();
        } catch (Throwable error) {
            return CallResult.failed(error);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Throwable call(UserFixture user, Runnable action) {
        SecurityContextHolder.clearContext();
        try {
            authenticate(user);
            action.run();
            return null;
        } catch (Throwable error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private int callValue(UserFixture user, IntCall action) {
        SecurityContextHolder.clearContext();
        try {
            authenticate(user);
            return action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(UserFixture user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.loginId(),
                "test-only",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private void lockPoint(Connection holder, int userId) throws SQLException {
        try (PreparedStatement statement = holder.prepareStatement(
                "SELECT point FROM user_table WHERE user_id = ? FOR UPDATE")) {
            statement.setInt(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next(), "잠글 사용자 행이 없습니다.");
            }
        }
    }

    private UserFixture insertUser(int point) {
        Integer baumannMax = jdbc.queryForObject("SELECT NVL(MAX(baumann_id), 0) FROM baumann", Integer.class);
        int baumannId = baumannMax + 1;
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
        Integer baumannMax = jdbc.queryForObject("SELECT NVL(MAX(baumann_id), 0) FROM baumann", Integer.class);
        int baumannId = baumannMax + 1;
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

    private int insertReview(UserFixture user, int likeCount, Integer reviewCount) {
        String name = PREFIX + UUID.randomUUID();
        if (reviewCount == null) {
            jdbc.update("""
                    INSERT INTO product (prd_name, prd_brand, ingredient, price, category, baumann_id)
                    VALUES (?, 'test-brand', 'test-ingredient', 1000, 'test', ?)
                    """, name, user.baumannId());
        } else {
            jdbc.update("""
                    INSERT INTO product (prd_name, prd_brand, ingredient, price, category, baumann_id, review_count)
                    VALUES (?, 'test-brand', 'test-ingredient', 1000, 'test', ?, ?)
                    """, name, user.baumannId(), reviewCount);
        }
        Integer productId = jdbc.queryForObject(
                "SELECT product_id FROM product WHERE prd_name = ?", Integer.class, name);
        assertNotNull(productId);
        jdbc.update("""
                INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, is_selected, created_at)
                VALUES (?, ?, ?, 5, ?, '0', '0', SYSDATE)
                """, productId, user.userId(), name, likeCount);
        Integer reviewId = jdbc.queryForObject(
                "SELECT review_id FROM review WHERE product_id = ? AND user_id = ?",
                Integer.class, productId, user.userId());
        assertNotNull(reviewId);
        return reviewId;
    }

    private boolean reviewIsBestCandidate(int reviewId) {
        Integer eligible = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM (
                    SELECT review_id
                    FROM (
                        SELECT r.review_id,
                               ROW_NUMBER() OVER (
                                   PARTITION BY r.product_id
                                   ORDER BY r.like_count DESC, r.dislike_count ASC
                               ) AS review_number,
                               p.review_count
                        FROM review r
                        JOIN product p ON p.product_id = r.product_id
                        WHERE r.like_count > 20
                          AND p.review_count > 30
                          AND r.product_id = (SELECT product_id FROM review WHERE review_id = ?)
                    )
                    WHERE review_number <= FLOOR(review_count * 0.1)
                      AND review_id = ?
                )
                """, Integer.class, reviewId, reviewId);
        return eligible != null && eligible == 1;
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

    private List<AdminHistory> adminHistoryOf(int userId) {
        return jdbc.query("""
                SELECT type, amount, description
                FROM point_history
                WHERE user_id = ?
                ORDER BY point_history_id
                """, (rs, row) -> new AdminHistory(
                rs.getString("type"), rs.getInt("amount"), rs.getString("description")), userId);
    }

    private void dropAdminHistoryProbe() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_constraints WHERE constraint_name = ?",
                Integer.class, ADMIN_HISTORY_PROBE);
        if (count != null && count > 0) {
            jdbc.execute("ALTER TABLE point_history DROP CONSTRAINT " + ADMIN_HISTORY_PROBE);
        }
    }

    private String selected(int reviewId) {
        String value = jdbc.queryForObject(
                "SELECT is_selected FROM review WHERE review_id = ?", String.class, reviewId);
        return value == null ? null : value.trim();
    }

    private String checked(int reviewId) {
        String value = jdbc.queryForObject(
                "SELECT is_checked FROM review WHERE review_id = ?", String.class, reviewId);
        return value == null ? null : value.trim();
    }

    private String constraintStatus(String name) {
        return jdbc.queryForObject(
                "SELECT status FROM user_constraints WHERE constraint_name = ?", String.class, name);
    }

    private int count(String sql, int userId) {
        Integer count = jdbc.queryForObject(sql, Integer.class, userId);
        return count == null ? 0 : count;
    }

    private int activeConnections() {
        HikariPoolMXBean pool = hikari().getHikariPoolMXBean();
        assertNotNull(pool);
        return pool.getActiveConnections();
    }

    private int threadsWaitingForConnection() {
        return hikari().getHikariPoolMXBean().getThreadsAwaitingConnection();
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

    private void deleteByPrefix() {
        jdbc.update("DELETE FROM point_history WHERE user_id = ?", MISSING_USER_ID);
        List<Integer> userIds = jdbc.queryForList(
                "SELECT user_id FROM user_table WHERE id LIKE ?", Integer.class, PREFIX + "%");
        List<Integer> productIds = jdbc.queryForList(
                "SELECT product_id FROM product WHERE prd_name LIKE ?", Integer.class, PREFIX + "%");
        if (!productIds.isEmpty()) {
            deleteIn("DELETE FROM point_history WHERE review_id IN (SELECT review_id FROM review WHERE product_id IN (%s))",
                    productIds);
        }
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

    private void release(Connection holder) {
        if (holder == null) {
            return;
        }
        try {
            holder.rollback();
        } catch (SQLException ignored) {
            // The connection is closed below even if rollback fails.
        }
        try {
            holder.close();
        } catch (SQLException ignored) {
            // Closing returns the connection to the pool.
        }
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

    private static boolean chainContains(Throwable error, String text) {
        return chain(error).stream().anyMatch(line -> line != null && line.contains(text));
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> chain(Throwable error) {
        List<String> lines = new ArrayList<>();
        for (Throwable current = error; current != null; current = current.getCause()) {
            lines.add(current.getClass().getName() + ": " + current.getMessage());
        }
        return lines;
    }

    @FunctionalInterface
    private interface IntCall {
        int run();
    }

    private record UserFixture(int userId, String loginId, int baumannId) {
    }

    private record HistoryRow(String type, int amount) {
    }

    private record AdminHistory(String type, int amount, String description) {
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
            return String.join(" <- ", chain(error));
        }
    }
}
