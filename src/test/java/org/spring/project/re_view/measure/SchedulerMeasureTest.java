package org.spring.project.re_view.measure;

import com.review.shop.dto.review.BestReviewDTO;
import com.review.shop.service.review.ProductReviewService;
import com.review.shop.util.ReviewScheduler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.spring.project.re_view.ReViewApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class SchedulerMeasureTest {

    private static final int PRODUCT_ID = 4;
    private static final int AUTHOR_ID = 4;

    @Autowired
    private ReviewScheduler reviewScheduler;

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scheduledSelectionDoesNotPersistWithoutLogin() throws Exception {
        Scheduled scheduled = ReviewScheduler.class
                .getMethod("updateBestReviewMonthly")
                .getAnnotation(Scheduled.class);
        assertNotNull(scheduled);
        assertNotNull(ReViewApplication.class.getAnnotation(EnableScheduling.class));

        int reviewCountColumn = jdbcTemplate.queryForObject(
                "SELECT review_count FROM product WHERE product_id = ?", Integer.class, PRODUCT_ID);
        int reviewRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review WHERE product_id = ?", Integer.class, PRODUCT_ID);
        List<Integer> candidates = productReviewService.selectBestReviewList().stream()
                .sorted(Comparator.comparingInt(BestReviewDTO::getReview_id))
                .map(BestReviewDTO::getReview_id)
                .toList();
        List<Integer> candidateUsers = productReviewService.selectBestReviewList().stream()
                .map(BestReviewDTO::getUser_id)
                .distinct()
                .toList();

        SecurityContextHolder.clearContext();
        String first = runOnce();
        String checkedAfterFirst = checked();
        int earnAfterFirst = bestEarnCount();
        int pointAfterFirst = authorPoint();

        SecurityContextHolder.clearContext();
        String second = runOnce();
        String checkedAfterSecond = checked();
        int earnAfterSecond = bestEarnCount();
        int pointAfterSecond = authorPoint();

        String report = """
                cron=%s zone=%s reviewCountColumn=%d reviewRows=%d floorTenPercent=%d
                candidates=%s users=%s
                first=%s checkedAfterFirst=%s earnAfterFirst=%d pointAfterFirst=%d
                second=%s checkedAfterSecond=%s earnAfterSecond=%d pointAfterSecond=%d
                """.formatted(
                scheduled.cron(),
                scheduled.zone(),
                reviewCountColumn,
                reviewRows,
                (int) Math.floor(reviewCountColumn * 0.1),
                candidates,
                candidateUsers,
                first,
                checkedAfterFirst,
                earnAfterFirst,
                pointAfterFirst,
                second,
                checkedAfterSecond,
                earnAfterSecond,
                pointAfterSecond);
        System.out.println(report);

        try {
            assertEquals("0 0 0 1 * *", scheduled.cron(), report);
            assertEquals("Asia/Seoul", scheduled.zone(), report);
            assertEquals(40, reviewCountColumn, report);
            assertEquals(7, reviewRows, report);
            assertEquals(List.of(4, 5, 6, 7), candidates, report);
            assertEquals(List.of(AUTHOR_ID), candidateUsers, report);
            assertTrue(first.contains("로그인이 필요합니다"), report);
            assertTrue(second.contains("로그인이 필요합니다"), report);
            assertEquals("2:1,4:0,5:0,6:0,7:0,8:0,9:0,10:0", checkedAfterFirst, report);
            assertEquals(checkedAfterFirst, checkedAfterSecond, report);
            assertEquals(0, earnAfterFirst, report);
            assertEquals(0, earnAfterSecond, report);
            assertEquals(0, pointAfterFirst, report);
            assertEquals(0, pointAfterSecond, report);
        } finally {
            jdbcTemplate.update("UPDATE review SET is_checked = 1 WHERE review_id = 2");
            jdbcTemplate.update("UPDATE review SET is_checked = 0 WHERE review_id BETWEEN 4 AND 10");
            jdbcTemplate.update("""
                    DELETE FROM point_history
                    WHERE user_id = ? AND amount = 700 AND type = 'EARN'
                    """, AUTHOR_ID);
            jdbcTemplate.update("UPDATE user_table SET point = 0 WHERE user_id = ?", AUTHOR_ID);
        }
    }

    private String runOnce() {
        try {
            reviewScheduler.runUpdateBestReviewsNow();
            return "no exception";
        } catch (RuntimeException exception) {
            return describe(exception);
        }
    }

    private String checked() {
        return jdbcTemplate.queryForObject("""
                SELECT LISTAGG(review_id || ':' || is_checked, ',')
                       WITHIN GROUP (ORDER BY review_id)
                FROM review
                WHERE review_id IN (2, 4, 5, 6, 7, 8, 9, 10)
                """, String.class);
    }

    private int bestEarnCount() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM point_history
                WHERE user_id = ? AND amount = 700 AND type = 'EARN'
                """, Integer.class, AUTHOR_ID);
        return count == null ? 0 : count;
    }

    private int authorPoint() {
        Integer point = jdbcTemplate.queryForObject(
                "SELECT point FROM user_table WHERE user_id = ?", Integer.class, AUTHOR_ID);
        return point == null ? 0 : point;
    }

    private static String describe(Throwable throwable) {
        StringBuilder chain = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (!chain.isEmpty()) {
                chain.append(" <- ");
            }
            chain.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause();
        }
        return chain.toString();
    }
}
