package org.spring.project.re_view.measure;

import com.review.shop.dto.orders.OrderCreateDTO;
import com.review.shop.dto.orders.OrderDTO;
import com.review.shop.dto.review.review_create.CreateReviewRequestDTO;
import com.review.shop.exception.WrongRequestException;
import com.review.shop.service.order.OrderService;
import com.review.shop.service.review.ProductReviewService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class OrderPointMeasureTest {

    private static final int USER_ID = 1;
    private static final String LOGIN_ID = "measure_user";

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PointService pointService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void buyerLimitSnapshotPointAndStockRollback() {
        StringBuilder report = new StringBuilder();
        int pointBefore = point(USER_ID);
        int ordersBefore = count("SELECT COUNT(*) FROM orders");
        int useRowsBefore = useRows(USER_ID, 100);
        try {
            runAsUser(() -> {
                boolean noOrder = productReviewService.canCreate(1, 3);
                boolean otherBuyer = productReviewService.canCreate(3, USER_ID);
                boolean alreadyReviewed = productReviewService.canCreate(2, USER_ID);
                boolean ownUnreviewed = productReviewService.canCreate(1, USER_ID);
                String noOrderMessage = rejectMessage(1, 3);
                String otherBuyerMessage = rejectMessage(3, USER_ID);
                String alreadyReviewedMessage = rejectMessage(2, USER_ID);

                report.append("canCreate noOrder=").append(noOrder)
                        .append(" otherBuyer=").append(otherBuyer)
                        .append(" alreadyReviewed=").append(alreadyReviewed)
                        .append(" ownUnreviewed=").append(ownUnreviewed)
                        .append('\n');
                report.append("reject noOrder=").append(noOrderMessage)
                        .append(" otherBuyer=").append(otherBuyerMessage)
                        .append(" alreadyReviewed=").append(alreadyReviewedMessage)
                        .append('\n');

                ItemSnapshot before = item(1);
                jdbcTemplate.update("UPDATE product SET prd_name = ?, price = ? WHERE product_id = ?",
                        "변경토너", 999, 1);
                ItemSnapshot afterRename = item(1);

                pointService.usePoint(USER_ID, 100);
                int pointAfterUse = point(USER_ID);
                int useRowsAfter = useRows(USER_ID, 100);

                orderService.processOrder(order(USER_ID, 1, 1, 0));
                ItemSnapshot created = jdbcTemplate.queryForObject("""
                        SELECT product_name, product_price
                        FROM order_item
                        WHERE product_id = 1 AND product_name = ?
                        """, (rs, row) -> new ItemSnapshot(rs.getString(1), rs.getInt(2)), "변경토너");
                ItemSnapshot originalStill = item(1);
                jdbcTemplate.update("UPDATE product SET prd_name = ? WHERE product_id = ?", "두번째변경", 1);
                ItemSnapshot createdAfterSecondRename = jdbcTemplate.queryForObject("""
                        SELECT product_name, product_price
                        FROM order_item
                        WHERE product_id = 1 AND product_price = 999
                        """, (rs, row) -> new ItemSnapshot(rs.getString(1), rs.getInt(2)));

                int ordersAfterSave = count("SELECT COUNT(*) FROM orders");
                int pointBeforeStock = point(USER_ID);
                int use50Before = useRows(USER_ID, 50);
                String stockMessage = null;
                try {
                    orderService.processOrder(order(USER_ID, 18, 1, 50));
                    stockMessage = "no exception";
                } catch (RuntimeException exception) {
                    stockMessage = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                }
                int ordersAfterStock = count("SELECT COUNT(*) FROM orders");
                int pointAfterStock = point(USER_ID);
                int use50After = useRows(USER_ID, 50);

                report.append("item1 before=").append(before)
                        .append(" afterProductRename=").append(afterRename)
                        .append(" created=").append(created)
                        .append(" originalStill=").append(originalStill)
                        .append(" createdAfterSecondRename=").append(createdAfterSecondRename)
                        .append('\n');
                report.append("pointBefore=").append(pointBefore)
                        .append(" pointAfterUse=").append(pointAfterUse)
                        .append(" use100 before=").append(useRowsBefore)
                        .append(" after=").append(useRowsAfter)
                        .append('\n');
                report.append("orders before=").append(ordersBefore)
                        .append(" afterSave=").append(ordersAfterSave)
                        .append(" stock=").append(stockMessage)
                        .append(" ordersAfterStock=").append(ordersAfterStock)
                        .append(" pointBeforeStock=").append(pointBeforeStock)
                        .append(" pointAfterStock=").append(pointAfterStock)
                        .append(" use50 before=").append(use50Before)
                        .append(" after=").append(use50After)
                        .append('\n');
                System.out.println(report);

                assertFalse(noOrder, report.toString());
                assertFalse(otherBuyer, report.toString());
                assertFalse(alreadyReviewed, report.toString());
                assertTrue(ownUnreviewed, report.toString());
                assertEquals("잘못된 접근입니다.", noOrderMessage, report.toString());
                assertEquals(noOrderMessage, otherBuyerMessage, report.toString());
                assertEquals(noOrderMessage, alreadyReviewedMessage, report.toString());
                assertEquals("측정토너", before.name, report.toString());
                assertEquals(12000, before.price, report.toString());
                assertEquals(before, afterRename, report.toString());
                assertEquals(new ItemSnapshot("변경토너", 999), created, report.toString());
                assertEquals(before, originalStill, report.toString());
                assertEquals(created, createdAfterSecondRename, report.toString());
                assertEquals(pointBefore - 100, pointAfterUse, report.toString());
                assertEquals(useRowsBefore + 1, useRowsAfter, report.toString());
                assertEquals(ordersBefore + 1, ordersAfterSave, report.toString());
                assertEquals("WrongRequestException: 재고가 부족합니다. 상품ID: 18", stockMessage, report.toString());
                assertEquals(ordersAfterSave, ordersAfterStock, report.toString());
                assertEquals(pointBeforeStock, pointAfterStock, report.toString());
                assertEquals(use50Before, use50After, report.toString());
            });
        } finally {
            jdbcTemplate.update("""
                    UPDATE product
                    SET prd_name = ?, price = ?, stock = ?
                    WHERE product_id = ?
                    """, "측정토너", 12000, 20, 1);
            jdbcTemplate.update("UPDATE product SET stock = 0 WHERE product_id = 18");
            jdbcTemplate.update("DELETE FROM order_item WHERE order_id NOT IN (1, 2, 3, 4)");
            jdbcTemplate.update("DELETE FROM orders WHERE order_id NOT IN (1, 2, 3, 4)");
            jdbcTemplate.update("DELETE FROM point_history WHERE user_id = ?", USER_ID);
            jdbcTemplate.update("UPDATE user_table SET point = ? WHERE user_id = ?", 10000, USER_ID);
            jdbcTemplate.update("""
                    INSERT INTO point_history (user_id, amount, type, description)
                    VALUES (?, ?, 'EARN', '시드 잔액')
                    """, USER_ID, 10000);
        }
    }

    private String rejectMessage(int orderItemId, int userId) {
        CreateReviewRequestDTO request = new CreateReviewRequestDTO();
        request.setOrder_item_id(orderItemId);
        try {
            productReviewService.createReviewWithReward(1, userId, request);
            return "no exception";
        } catch (WrongRequestException exception) {
            return exception.getMessage();
        }
    }

    private OrderCreateDTO order(int userId, int productId, int quantity, int usingPoint) {
        OrderCreateDTO order = new OrderCreateDTO();
        order.setUser_id(userId);
        order.setUsing_point(usingPoint);
        order.setOrder_list(List.of(new OrderDTO(productId, quantity)));
        return order;
    }

    private ItemSnapshot item(int orderItemId) {
        return jdbcTemplate.queryForObject("""
                SELECT product_name, product_price
                FROM order_item
                WHERE order_item_id = ?
                """, (rs, row) -> new ItemSnapshot(rs.getString(1), rs.getInt(2)), orderItemId);
    }

    private int point(int userId) {
        Integer value = jdbcTemplate.queryForObject(
                "SELECT point FROM user_table WHERE user_id = ?", Integer.class, userId);
        return value == null ? -1 : value;
    }

    private int useRows(int userId, int amount) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM point_history
                WHERE user_id = ? AND type = 'USE' AND amount = ?
                """, Integer.class, userId, amount);
        return value == null ? -1 : value;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? -1 : value;
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

    private record ItemSnapshot(String name, int price) {
    }
}
