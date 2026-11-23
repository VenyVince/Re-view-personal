package org.spring.project.re_view;

import com.review.shop.dto.orders.OrderCreateDTO;
import com.review.shop.dto.orders.OrderDTO;
import com.review.shop.exception.DatabaseException;
import com.review.shop.exception.WrongRequestException;
import com.review.shop.service.admin.AdminReviewService;
import com.review.shop.service.order.OrderService;
import com.review.shop.service.review.ProductReviewService;
import com.review.shop.service.userinfo.other.PointService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each test commits through the real service transaction.
 * The class itself must stay non-transactional so a rollback inside the service is visible afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class PointTransactionTest {

    private static final String PREFIX = "point-tx-";

    @Autowired
    private PointService pointService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private AdminReviewService adminReviewService;

    @Autowired
    private JdbcTemplate jdbc;

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
    }

    @AfterEach
    void removeRowsCreatedByThisClass() {
        deleteByPrefix();
    }

    @Test
    @DisplayName("B-01 잔액 부족이면 주문 트랜잭션 전체가 유지된다")
    void insufficientPointLeavesOrderUntouched() {
        UserFixture user = insertUser(1000);
        int productId = insertProduct(user, 10);
        Throwable error = call(user, () -> orderService.processOrder(order(user, productId, 1500, 1)));

        Outcome outcome = outcome(user, productId, null);
        System.out.println("POINT_TX B-01 exception=" + chain(error));
        System.out.println("POINT_TX B-01 after point=" + outcome.point
                + " history=" + outcome.history + " stock=" + outcome.stock + " orders=" + outcome.orders);

        assertInsufficientPoints(error);
        assertEquals(1000, outcome.point);
        assertEquals(0, outcome.history);
        assertEquals(10, outcome.stock);
        assertEquals(0, outcome.orders);
    }

    @Test
    @DisplayName("B-02 이력 INSERT 실패 시 잔액 UPDATE가 롤백된다")
    void historyInsertFailureRollsBalanceBack() {
        assertEquals("ENABLED", constraintStatus("FK_POINT_HISTORY_REVIEW"));
        UserFixture user = insertUser(1000);
        int missingReviewId = -1;
        Throwable error = call(user, () -> pointService.addReviewPoint(user.userId(), missingReviewId));

        int history = historyCount(user.userId());
        Integer point = pointOf(user.userId());
        System.out.println("POINT_TX B-02 exception=" + chain(error));
        System.out.println("POINT_TX B-02 after point=" + point + " history=" + history);

        assertNotNull(error);
        assertTrue(error instanceof DatabaseException, chain(error).toString());
        assertTrue(chainContains(error, "ORA-02291") || chainContains(error, "FK_POINT_HISTORY_REVIEW"),
                chain(error).toString());
        assertEquals(1000, point);
        assertEquals(0, history);
    }

    @Test
    @DisplayName("B-03 주문 INSERT 실패 시 포인트와 재고가 롤백된다")
    void orderInsertFailureRollsPointAndStockBack() {
        assertEquals("ENABLED", constraintStatus("FK_ADDRESS_TO_ORDER"));
        assertEquals("ENABLED", constraintStatus("FK_PAYMENT_TO_ORDER"));
        UserFixture user = insertUser(1000);
        int productId = insertProduct(user, 10);
        Throwable error = call(user, () -> orderService.processOrder(order(user, productId, 100, -1)));

        Outcome outcome = outcome(user, productId, null);
        System.out.println("POINT_TX B-03 exception=" + chain(error));
        System.out.println("POINT_TX B-03 after point=" + outcome.point
                + " history=" + outcome.history + " stock=" + outcome.stock + " orders=" + outcome.orders);

        assertNotNull(error);
        assertFalse(chainContains(error, "포인트가 부족합니다."), chain(error).toString());
        assertTrue(chainContains(error, "ORA-02291")
                        || chainContains(error, "FK_ADDRESS_TO_ORDER")
                        || chainContains(error, "FK_PAYMENT_TO_ORDER"),
                chain(error).toString());
        assertEquals(1000, outcome.point);
        assertEquals(0, outcome.history);
        assertEquals(10, outcome.stock);
        assertEquals(0, outcome.orders);
    }

    @Test
    @DisplayName("B-04 본인 리뷰 삭제의 포인트 회수 실패는 소프트 삭제까지 롤백된다")
    void ownReviewDeleteRollsBackWhenPointRecoveryFails() {
        UserFixture user = insertUser(0);
        int productId = insertProduct(user, 10);
        int reviewId = insertReview(user, productId);
        Throwable error = call(user, () ->
                productReviewService.deleteReviewWithPenalty(productId, user.userId(), reviewId));

        Outcome outcome = outcome(user, productId, reviewId);
        System.out.println("POINT_TX B-04 exception=" + chain(error));
        System.out.println("POINT_TX B-04 after point=" + outcome.point
                + " history=" + outcome.history + " deletedAt=" + outcome.deletedAt);

        assertInsufficientPoints(error);
        assertNull(outcome.deletedAt);
        assertEquals(0, outcome.point);
        assertEquals(0, outcome.history);
    }

    @Test
    @DisplayName("B-05 운영자 리뷰 삭제는 포인트 회수 실패와 트랜잭션이 분리된다")
    void adminReviewDeleteKeepsSoftDeleteWhenPointRecoveryFails() {
        UserFixture user = insertUser(0);
        int productId = insertProduct(user, 10);
        int reviewId = insertReview(user, productId);
        Throwable error = call(user, () -> adminReviewService.deleteReview(reviewId));

        Outcome outcome = outcome(user, productId, reviewId);
        System.out.println("POINT_TX B-05 desiredDeletedAt=null actualDeletedAt=" + outcome.deletedAt
                + " desiredPoint=0 actualPoint=" + outcome.point
                + " desiredHistory=0 actualHistory=" + outcome.history);
        System.out.println("POINT_TX B-05 exception=" + chain(error));

        assertInsufficientPoints(error);
        assertNotNull(outcome.deletedAt);
        assertEquals(0, outcome.point);
        assertEquals(0, outcome.history);
    }

    @Test
    @DisplayName("C-05-3 0포인트 주문")
    void zeroPointOrderCreatesOrderWithoutHistory() {
        UserFixture user = insertUser(1000);
        int productId = insertProduct(user, 10);
        int addressId = insertAddress(user);
        int paymentId = insertPayment(user);
        Throwable error = call(user, () ->
                orderService.processOrder(orderWithKeys(user, productId, 0, addressId, paymentId)));

        Outcome outcome = outcome(user, productId, null);
        Integer usedPoint = usedPointOf(user.userId());
        System.out.println("POINT_TX C-05-3 point=" + outcome.point
                + " history=" + outcome.history
                + " stock=" + outcome.stock
                + " orders=" + outcome.orders
                + " usedPoint=" + usedPoint
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(1000, outcome.point);
        assertEquals(0, outcome.history);
        assertEquals(9, outcome.stock);
        assertEquals(1, outcome.orders);
        assertEquals(0, usedPoint);
    }

    @Test
    @DisplayName("C-05-4 음수 포인트 주문")
    void negativePointOrderIsRejected() {
        UserFixture user = insertUser(1000);
        int productId = insertProduct(user, 10);
        int addressId = insertAddress(user);
        int paymentId = insertPayment(user);
        Throwable error = call(user, () ->
                orderService.processOrder(orderWithKeys(user, productId, -100, addressId, paymentId)));

        Outcome outcome = outcome(user, productId, null);
        System.out.println("POINT_TX C-05-4 point=" + outcome.point
                + " history=" + outcome.history
                + " stock=" + outcome.stock
                + " orders=" + outcome.orders
                + " error=" + chain(error));

        assertTrue(error instanceof WrongRequestException, chain(error).toString());
        assertTrue(chainContains(error, "사용할 포인트는 0 이상이어야 합니다."), chain(error).toString());
        assertEquals(1000, outcome.point);
        assertEquals(0, outcome.history);
        assertEquals(10, outcome.stock);
        assertEquals(0, outcome.orders);
    }

    @Test
    @DisplayName("C-05-5 양수 포인트 주문")
    void positivePointOrderDeductsAndRecordsHistory() {
        UserFixture user = insertUser(1000);
        int productId = insertProduct(user, 10);
        int addressId = insertAddress(user);
        int paymentId = insertPayment(user);
        Throwable error = call(user, () ->
                orderService.processOrder(orderWithKeys(user, productId, 100, addressId, paymentId)));

        Outcome outcome = outcome(user, productId, null);
        List<HistoryDetail> history = historyDetails(user.userId());
        Integer usedPoint = usedPointOf(user.userId());
        System.out.println("POINT_TX C-05-5 point=" + outcome.point
                + " history=" + history
                + " stock=" + outcome.stock
                + " orders=" + outcome.orders
                + " usedPoint=" + usedPoint
                + " error=" + chain(error));

        assertNull(error, chain(error).toString());
        assertEquals(900, outcome.point);
        assertEquals(List.of(new HistoryDetail("USE", 100, "물품 구매로 인한 차감")), history);
        assertEquals(9, outcome.stock);
        assertEquals(1, outcome.orders);
        assertEquals(100, usedPoint);
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

    private void assertInsufficientPoints(Throwable error) {
        assertNotNull(error, "잔액 부족 예외가 발생하지 않았습니다.");
        assertTrue(error instanceof DatabaseException, chain(error).toString());
        assertTrue(chainContains(error, "포인트가 부족합니다."), chain(error).toString());
        assertTrue(hasCause(error, WrongRequestException.class), chain(error).toString());
        assertFalse(chainContains(error, "ORA-00054") || chainContains(error, "ORA-30006"), chain(error).toString());
    }

    private OrderCreateDTO order(UserFixture user, int productId, int usingPoint, int missingKey) {
        return orderWithKeys(user, productId, usingPoint, missingKey, missingKey);
    }

    private OrderCreateDTO orderWithKeys(
            UserFixture user, int productId, int usingPoint, int addressId, int paymentId) {
        OrderCreateDTO order = new OrderCreateDTO();
        order.setUser_id(user.userId());
        order.setUsing_point(usingPoint);
        order.setAddress_id(addressId);
        order.setPayment_id(paymentId);
        order.setOrder_list(List.of(new OrderDTO(productId, 1)));
        return order;
    }

    private void authenticate(UserFixture user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user.loginId(),
                "test-only",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
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

    private int insertProduct(UserFixture user, int stock) {
        String name = PREFIX + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (prd_name, prd_brand, ingredient, price, category, baumann_id, stock)
                VALUES (?, 'test-brand', 'test-ingredient', 1000, 'test', ?, ?)
                """, name, user.baumannId(), stock);
        Integer productId = jdbc.queryForObject(
                "SELECT product_id FROM product WHERE prd_name = ?", Integer.class, name);
        assertNotNull(productId);
        return productId;
    }

    private int insertAddress(UserFixture user) {
        jdbc.update("""
                INSERT INTO address (user_id, recipient, is_default)
                VALUES (?, 'tester', '1')
                """, user.userId());
        Integer addressId = jdbc.queryForObject(
                "SELECT MAX(address_id) FROM address WHERE user_id = ?", Integer.class, user.userId());
        assertNotNull(addressId);
        return addressId;
    }

    private int insertPayment(UserFixture user) {
        jdbc.update("""
                INSERT INTO payment_methods (user_id, card_company, is_default, is_active)
                VALUES (?, 'test', '1', '1')
                """, user.userId());
        Integer paymentId = jdbc.queryForObject(
                "SELECT MAX(payment_id) FROM payment_methods WHERE user_id = ?", Integer.class, user.userId());
        assertNotNull(paymentId);
        return paymentId;
    }

    private int insertReview(UserFixture user, int productId) {
        String content = PREFIX + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at)
                VALUES (?, ?, ?, 5, 0, '0', SYSDATE)
                """, productId, user.userId(), content);
        Integer reviewId = jdbc.queryForObject(
                "SELECT review_id FROM review WHERE product_id = ? AND user_id = ?",
                Integer.class, productId, user.userId());
        assertNotNull(reviewId);
        return reviewId;
    }

    private Outcome outcome(UserFixture user, int productId, Integer reviewId) {
        Timestamp deletedAt = null;
        if (reviewId != null) {
            deletedAt = jdbc.queryForObject(
                    "SELECT deleted_at FROM review WHERE review_id = ?", Timestamp.class, reviewId);
        }
        return new Outcome(
                pointOf(user.userId()),
                historyCount(user.userId()),
                stockOf(productId),
                orderCount(user.userId()),
                deletedAt);
    }

    private Integer pointOf(int userId) {
        return jdbc.queryForObject("SELECT point FROM user_table WHERE user_id = ?", Integer.class, userId);
    }

    private int historyCount(int userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM point_history WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private Integer stockOf(int productId) {
        return jdbc.queryForObject("SELECT stock FROM product WHERE product_id = ?", Integer.class, productId);
    }

    private int orderCount(int userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private Integer usedPointOf(int userId) {
        return jdbc.queryForObject(
                "SELECT used_point FROM orders WHERE user_id = ?", Integer.class, userId);
    }

    private List<HistoryDetail> historyDetails(int userId) {
        return jdbc.query("""
                SELECT type, amount, description
                FROM point_history
                WHERE user_id = ?
                ORDER BY point_history_id
                """, (rs, rowNum) -> new HistoryDetail(
                rs.getString("type"), rs.getInt("amount"), rs.getString("description")), userId);
    }

    private String constraintStatus(String name) {
        return jdbc.queryForObject(
                "SELECT status FROM user_constraints WHERE constraint_name = ?", String.class, name);
    }

    private void deleteByPrefix() {
        List<Integer> userIds = jdbc.queryForList(
                "SELECT user_id FROM user_table WHERE id LIKE ?", Integer.class, PREFIX + "%");
        List<Integer> productIds = jdbc.queryForList(
                "SELECT product_id FROM product WHERE prd_name LIKE ?", Integer.class, PREFIX + "%");
        if (!userIds.isEmpty()) {
            deleteIn("DELETE FROM point_history WHERE user_id IN (%s)", userIds);
            deleteIn("""
                    DELETE FROM order_item WHERE order_id IN (
                        SELECT order_id FROM orders WHERE user_id IN (%s)
                    )
                    """, userIds);
            deleteIn("DELETE FROM orders WHERE user_id IN (%s)", userIds);
            deleteIn("DELETE FROM address WHERE user_id IN (%s)", userIds);
            deleteIn("DELETE FROM payment_methods WHERE user_id IN (%s)", userIds);
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

    private static boolean chainContains(Throwable error, String text) {
        return chain(error).stream().anyMatch(line -> line.contains(text));
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

    private record UserFixture(int userId, String loginId, int baumannId) {
    }

    private record Outcome(Integer point, int history, Integer stock, int orders, Timestamp deletedAt) {
    }

    private record HistoryDetail(String type, int amount, String description) {
    }
}
