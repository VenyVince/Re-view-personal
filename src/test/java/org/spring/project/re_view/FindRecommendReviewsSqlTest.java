package org.spring.project.re_view;

import com.review.shop.dto.recommendations.RecommendReviewDTO;
import com.review.shop.repository.recommendations.RecommendationsMapper;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FindRecommendReviewsSqlTest {

    private static final List<String> VIEWER = List.of("D", "S", "P", "W");

    @Autowired
    private RecommendationsMapper recommendationsMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SqlSession sqlSession;

    @Autowired
    private Environment environment;

    @BeforeEach
    void requireEmptyTestDatabase() {
        String url = environment.getProperty("spring.datasource.url", "");
        String username = environment.getProperty("spring.datasource.username", "");
        String normalizedUrl = url.toLowerCase();
        assertTrue(normalizedUrl.contains(":1522") && normalizedUrl.contains("/reviewtest"), url);
        assertFalse(normalizedUrl.contains(":1521") || normalizedUrl.contains("xepdb1"), url);
        assertEquals("REVIEW_TEST", username);

        assertEquals(0, count("review"));
        assertEquals(0, count("product"));
        assertEquals(0, count("user_table"));
        assertEquals(0, count("baumann"));
    }

    @Test
    void executesTheRecommendationQueryOnTheTestDatabase() {
        long author = user(baumann("O", "R", "N", "T"), "viewer-mismatch");
        long productId = product("connection-probe", baumann("O", "R", "N", "T"));
        long reviewId = review(productId, author, "5", 3, "0", false, "connection-review");

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(1, rows.size());
        RecommendReviewDTO row = rows.get(0);
        assertEquals(reviewId, row.getReview_id());
        assertEquals(productId, row.getProduct_id());
        assertEquals(5f, row.getReview_rating(), 0.001f);
        assertEquals(0, row.getTotal_score());
        assertEquals(5f, row.getProduct_rating(), 0.001f);
        assertEquals(new BigDecimal("5"), storedRating(productId));
    }

    @Test
    void excludesProductsWithFewerThanTwoValidReviews() {
        long author = user(baumann("O", "R", "N", "T"), "single");
        long alone = product("one-valid-review", baumann("O", "R", "N", "T"));
        review(alone, author, "5", 1, "0", false, "only-valid");
        long untouched = product("no-reviews", baumann("O", "R", "N", "T"));

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(0, find(rows, "one-valid-review").getTotal_score());
        assertTrue(rows.stream().noneMatch(row -> row.getProduct_id() == untouched));
        assertEquals(0, eligibleProductCount());
    }

    @Test
    void scoresIdenticalRatingsAsTen() {
        long author = user(baumann("O", "R", "N", "T"), "same");
        long productId = product("all-fives", baumann("O", "R", "N", "T"));
        review(productId, author, "5", 1, "0", false, "five-a");
        long winner = review(productId, author, "5", 2, "0", false, "five-b");

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "all-fives");

        assertEquals(0, new BigDecimal("0").compareTo(populationStddev(productId)));
        assertEquals(winner, row.getReview_id());
        assertEquals(10, row.getTotal_score());
    }

    @Test
    void ceilKeepsTheSingleHighestStddevWhenTwoProductsQualify() {
        long author = user(baumann("O", "R", "N", "T"), "ceil-two");
        pair(author, "wide", "3", "5", 4);
        pair(author, "narrow", "4", "4.8", 2);

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(1, topCount(2));
        assertEquals(new BigDecimal("1.0"), expectedThreshold("1.0"));
        assertEquals(5, find(rows, "wide").getTotal_score());
        assertEquals(10, find(rows, "narrow").getTotal_score());
    }

    @Test
    void ceilKeepsTheSingleHighestStddevWhenThreeProductsQualify() {
        long author = user(baumann("O", "R", "N", "T"), "ceil-three");
        pair(author, "top", "3", "5", 5);
        pair(author, "middle", "3.2", "5", 3);
        pair(author, "low", "4.6", "5", 1);

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(1, topCount(3));
        assertEquals(new BigDecimal("1.0"), expectedThreshold("1.0"));
        assertEquals(5, find(rows, "top").getTotal_score());
        assertEquals(5, find(rows, "middle").getTotal_score());
        assertEquals(10, find(rows, "low").getTotal_score());
    }

    @Test
    void ceilKeepsTheTwoHighestStddevsWhenFourProductsQualify() {
        long author = user(baumann("O", "R", "N", "T"), "ceil-four");
        pair(author, "std-1.0", "3", "5", 8);
        pair(author, "std-0.8", "3.2", "4.8", 6);
        pair(author, "std-0.4", "4", "4.8", 4);
        pair(author, "std-0.2", "4", "4.4", 2);

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(2, topCount(4));
        assertEquals(new BigDecimal("0.9"), expectedThreshold("1.0", "0.8"));
        assertEquals(0, find(rows, "std-1.0").getTotal_score());
        assertEquals(5, find(rows, "std-0.8").getTotal_score());
        assertEquals(10, find(rows, "std-0.4").getTotal_score());
        assertEquals(10, find(rows, "std-0.2").getTotal_score());
    }

    @Test
    void keepsTheBucketScoreWhenDeviationEqualsTheThreshold() {
        long author = user(baumann("O", "R", "N", "T"), "equal");
        pair(author, "equal-half", "4", "5", 7);

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "equal-half");

        assertEquals(new BigDecimal("0.5"), expectedThreshold("0.5"));
        assertEquals(10, row.getTotal_score());
    }

    @Test
    void scoresDeviationAboveOneAsZeroWhenItDoesNotExceedTheThreshold() {
        long author = user(baumann("O", "R", "N", "T"), "above-one");
        pair(author, "spread", "2", "5", 4);

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "spread");

        assertEquals(new BigDecimal("1.5"), expectedThreshold("1.5"));
        assertEquals(0, row.getTotal_score());
    }

    @Test
    void returnsOnlyTheHighestScoringReviewForEachProduct() {
        long author = user(baumann("O", "R", "N", "T"), "representative");
        long productId = product("one-product", baumann("O", "R", "N", "T"));
        review(productId, author, "5", 1, "0", false, "lower-like");
        long winner = review(productId, author, "5", 9, "0", false, "higher-like");

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(1, rows.size());
        assertEquals(winner, rows.get(0).getReview_id());
        assertEquals(9, rows.get(0).getLike_count());
    }

    @Test
    void returnsAtMostSixteenReviewsOrderedByScoreThenLikes() {
        long checkedAuthor = user(baumann("D", "S", "P", "W"), "checked-author");
        long plainAuthor = user(baumann("O", "R", "N", "T"), "plain-author");
        long productBaumann = baumann("O", "R", "N", "T");
        long checkedProduct = product("checked-review", productBaumann);
        long plainProduct = product("plain-review", productBaumann);
        review(checkedProduct, checkedAuthor, "5", 1, "1", false, "checked");
        review(plainProduct, checkedAuthor, "5", 50, "0", false, "plain");
        for (int likes = 1; likes <= 15; likes++) {
            long extra = product("extra-" + likes, productBaumann);
            review(extra, plainAuthor, "5", likes, "0", false, "extra-" + likes);
        }

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(16, rows.size());
        assertEquals(find(rows, "checked-review").getProduct_id(), rows.get(0).getProduct_id());
        assertEquals(48, rows.get(0).getTotal_score());
        assertEquals(find(rows, "plain-review").getProduct_id(), rows.get(1).getProduct_id());
        assertEquals(40, rows.get(1).getTotal_score());
        assertTrue(rows.stream().noneMatch(row -> names.get("extra-1").equals(row.getPrd_name())));
        assertEquals(16, rows.stream().map(RecommendReviewDTO::getProduct_id).distinct().count());
    }

    @Test
    void addsAuthorAndProductBaumannPointsWithoutChangingTheRatingRule() {
        long match = user(baumann("D", "S", "P", "W"), "full-match");
        long firstOnly = user(baumann("D", "O", "N", "T"), "first-only");
        long other = user(baumann("O", "R", "N", "T"), "other");
        review(product("full-match", baumann("O", "R", "N", "T")), match, "5", 1, "0", false, "full");
        review(product("first-only", baumann("O", "R", "N", "T")), firstOnly, "5", 1, "0", false, "first");
        review(product("null-axis", baumann(null, "O", "N", "T")), other, "5", 1, "0", false, "null-axis");

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertEquals(40, find(rows, "full-match").getTotal_score());
        assertEquals(10, find(rows, "first-only").getTotal_score());
        assertEquals(3, find(rows, "null-axis").getTotal_score());
    }

    @Test
    void doesNotRankADeletedReviewAheadOfValidReviews() {
        long matched = user(baumann("D", "S", "P", "W"), "deleted-author");
        long other = user(baumann("O", "R", "N", "T"), "live-author");
        long productId = product("deleted-loses", baumann("O", "R", "N", "T"));
        long deletedId = review(productId, matched, "1", 1, "0", true, "deleted-review");
        long winner = review(productId, other, "5", 9, "0", false, "live-a");
        review(productId, other, "5", 8, "0", false, "live-b");

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "deleted-loses");

        assertEquals(0, new BigDecimal("0").compareTo(populationStddev(productId)));
        assertEquals(winner, row.getReview_id());
        assertEquals(9, row.getLike_count());
        assertEquals(10, row.getTotal_score());
        assertTrue(recommendationsMapper.findRecommendReviews(VIEWER).stream()
                .noneMatch(candidate -> candidate.getReview_id() == deletedId));
    }

    @Test
    void excludesAProductThatHasOnlyDeletedReviews() {
        long author = user(baumann("D", "S", "P", "W"), "only-deleted");
        long deletedProduct = product("deleted-only", baumann("O", "R", "N", "T"));
        review(deletedProduct, author, "5", 99, "1", true, "gone");
        long visibleAuthor = user(baumann("O", "R", "N", "T"), "still-visible");
        review(product("still-visible", baumann("O", "R", "N", "T")), visibleAuthor, "5", 1, "0", false, "visible");

        List<RecommendReviewDTO> rows = recommendationsMapper.findRecommendReviews(VIEWER);

        assertTrue(rows.stream().noneMatch(row -> names.get("deleted-only").equals(row.getPrd_name())));
        assertEquals(0, find(rows, "still-visible").getTotal_score());
    }

    @Test
    void mapsAFractionalProductRatingAndKeepsReviewScoreFields() {
        long author = user(baumann("O", "R", "N", "T"), "fraction");
        long productId = product("three-point-five", baumann("O", "R", "N", "T"));
        review(productId, author, "3", 1, "0", false, "three");
        long winner = review(productId, author, "4", 6, "0", false, "four");

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "three-point-five");

        assertEquals(new BigDecimal("3.5"), storedRating(productId));
        assertEquals(3.5f, row.getProduct_rating(), 0.001f);
        assertEquals(4f, row.getReview_rating(), 0.001f);
        assertEquals(winner, row.getReview_id());
        assertEquals(10, row.getTotal_score());
    }

    @Test
    void mapsANullProductRatingToZeroBecauseTheDtoFieldIsPrimitive() {
        long author = user(baumann("O", "R", "N", "T"), "null-rating");
        long productId = product("null-product-rating", baumann("O", "R", "N", "T"));
        review(productId, author, "3", 1, "0", false, "rated-three");
        long reviewId = review(productId, author, "4", 2, "0", false, "rated-four");
        assertEquals(10, find(recommendationsMapper.findRecommendReviews(VIEWER), "null-product-rating").getTotal_score());
        jdbc.update("UPDATE product SET rating = NULL WHERE product_id = ?", productId);
        assertNull(storedRating(productId));
        sqlSession.clearCache();

        RecommendReviewDTO row = find(recommendationsMapper.findRecommendReviews(VIEWER), "null-product-rating");

        assertEquals(reviewId, row.getReview_id());
        assertEquals(4f, row.getReview_rating(), 0.001f);
        assertEquals(0f, row.getProduct_rating(), 0.001f);
        assertEquals(0, row.getTotal_score());
    }

    @Test
    void prefersTheNewerReviewWhenScoreAndLikesMatch() {
        long author = user(baumann("O", "R", "N", "T"), "newer");
        long productId = product("newer-wins", baumann("O", "R", "N", "T"));
        reviewAt(productId, author, "5", 4, "0", false, "older", Timestamp.valueOf("2026-01-01 00:00:00"));
        long newer = reviewAt(productId, author, "5", 4, "0", false, "newer", Timestamp.valueOf("2026-06-01 00:00:00"));

        List<Long> ids = repeatedIds("newer-wins");

        assertEquals(List.of(newer, newer, newer), ids);
        assertEquals(10, find(recommendationsMapper.findRecommendReviews(VIEWER), "newer-wins").getTotal_score());
    }

    @Test
    void prefersTheLowerReviewIdWhenScoreLikesAndCreatedAtMatch() {
        long author = user(baumann("O", "R", "N", "T"), "same-time");
        long productId = product("lower-id-wins", baumann("O", "R", "N", "T"));
        Timestamp sameTime = Timestamp.valueOf("2026-03-01 00:00:00");
        long first = reviewAt(productId, author, "5", 4, "0", false, "first", sameTime);
        long second = reviewAt(productId, author, "5", 4, "0", false, "second", sameTime);

        assertTrue(first < second);
        assertEquals(List.of(first, first, first), repeatedIds("lower-id-wins"));
    }

    @Test
    void selectsTheSixteenBoundaryDeterministicallyByReviewId() {
        long author = user(baumann("O", "R", "N", "T"), "boundary");
        long productBaumann = baumann("O", "R", "N", "T");
        Timestamp sameTime = Timestamp.valueOf("2026-04-01 00:00:00");
        long[] ids = new long[17];
        for (int index = 0; index < ids.length; index++) {
            long productId = product("boundary-" + index, productBaumann);
            ids[index] = reviewAt(productId, author, "5", 1, "0", false, "boundary-" + index, sameTime);
        }

        List<List<Long>> attempts = List.of(reviewIds(), reviewIds(), reviewIds());

        assertEquals(attempts.get(0), attempts.get(1));
        assertEquals(attempts.get(0), attempts.get(2));
        assertEquals(16, attempts.get(0).size());
        for (int index = 0; index < 16; index++) {
            assertEquals(ids[index], attempts.get(0).get(index));
        }
        assertFalse(attempts.get(0).contains(ids[16]));
    }

    private long pair(long author, String name, String low, String high, int likes) {
        long productId = product(name, baumann("O", "R", "N", "T"));
        review(productId, author, low, 1, "0", false, name + "-low");
        review(productId, author, high, likes, "0", false, name + "-high");
        assertEquals(0, new BigDecimal(low).add(new BigDecimal(high)).divide(new BigDecimal("2"))
                .compareTo(storedRating(productId)));
        return productId;
    }

    private long baumann(String first, String second, String third, String fourth) {
        Long max = jdbc.queryForObject("SELECT NVL(MAX(baumann_id), 0) FROM baumann", Long.class);
        long id = max + 1;
        jdbc.update("""
                INSERT INTO baumann (baumann_id, type, first, second, third, fourth)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, "type-" + id, first, second, third, fourth);
        return id;
    }

    private long user(long baumannId, String nickname) {
        String token = nickname + "-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO user_table (baumann_id, password, id, name, email, nickname)
                VALUES (?, 'test-only', ?, 'tester', ?, ?)
                """, baumannId, token, token + "@test.local", nickname);
        return jdbc.queryForObject("SELECT user_id FROM user_table WHERE id = ?", Long.class, token);
    }

    private long product(String name, long baumannId) {
        String storedName = name + "-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product (prd_name, prd_brand, ingredient, price, category, baumann_id)
                VALUES (?, 'test-brand', 'test-ingredient', 1000, 'test', ?)
                """, storedName, baumannId);
        names.put(name, storedName);
        return jdbc.queryForObject("SELECT product_id FROM product WHERE prd_name = ?", Long.class, storedName);
    }

    private final java.util.Map<String, String> names = new java.util.HashMap<>();

    private long review(long productId, long userId, String rating, int likes, String checked, boolean deleted, String content) {
        return reviewAt(productId, userId, rating, likes, checked, deleted, content, null);
    }

    private long reviewAt(long productId, long userId, String rating, int likes, String checked, boolean deleted,
                          String content, Timestamp createdAt) {
        if (createdAt == null && deleted) {
            jdbc.update("""
                    INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)
                    """, productId, userId, content, new BigDecimal(rating), likes, checked);
        } else if (createdAt == null) {
            jdbc.update("""
                    INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, SYSDATE)
                    """, productId, userId, content, new BigDecimal(rating), likes, checked);
        } else if (deleted) {
            jdbc.update("""
                    INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE)
                    """, productId, userId, content, new BigDecimal(rating), likes, checked, createdAt);
        } else {
            jdbc.update("""
                    INSERT INTO review (product_id, user_id, content, rating, like_count, is_checked, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, productId, userId, content, new BigDecimal(rating), likes, checked, createdAt);
        }
        return jdbc.queryForObject(
                "SELECT review_id FROM review WHERE product_id = ? AND TO_CHAR(content) = ?",
                Long.class, productId, content);
    }

    private List<Long> repeatedIds(String logicalName) {
        long first = find(recommendationsMapper.findRecommendReviews(VIEWER), logicalName).getReview_id();
        long second = find(recommendationsMapper.findRecommendReviews(VIEWER), logicalName).getReview_id();
        long third = find(recommendationsMapper.findRecommendReviews(VIEWER), logicalName).getReview_id();
        return List.of(first, second, third);
    }

    private List<Long> reviewIds() {
        return recommendationsMapper.findRecommendReviews(VIEWER).stream()
                .map(row -> (long) row.getReview_id())
                .toList();
    }

    private RecommendReviewDTO find(List<RecommendReviewDTO> rows, String logicalName) {
        String storedName = names.get(logicalName);
        return rows.stream()
                .filter(row -> storedName.equals(row.getPrd_name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing " + logicalName + " in " + rows));
    }

    private BigDecimal storedRating(long productId) {
        return jdbc.queryForObject("SELECT rating FROM product WHERE product_id = ?", BigDecimal.class, productId);
    }

    private BigDecimal populationStddev(long productId) {
        return jdbc.queryForObject("""
                SELECT STDDEV_POP(rating)
                  FROM review
                 WHERE product_id = ?
                   AND deleted_at IS NULL
                   AND rating BETWEEN 1 AND 5
                """, BigDecimal.class, productId);
    }

    private int eligibleProductCount() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM (
                        SELECT product_id
                          FROM review
                         WHERE deleted_at IS NULL
                           AND rating BETWEEN 1 AND 5
                         GROUP BY product_id
                        HAVING COUNT(*) >= 2
                       )
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private static int topCount(int eligibleProducts) {
        return (int) Math.ceil(eligibleProducts * 0.3d);
    }

    private static BigDecimal expectedThreshold(String... topStddevs) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String stddev : topStddevs) {
            sum = sum.add(new BigDecimal(stddev));
        }
        return sum.divide(new BigDecimal(topStddevs.length));
    }

    private int count(String table) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
