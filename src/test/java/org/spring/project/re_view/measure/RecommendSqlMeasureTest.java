package org.spring.project.re_view.measure;

import com.review.shop.dto.recommendations.BaumannDTO;
import com.review.shop.dto.recommendations.RecommendProductDTO;
import com.review.shop.dto.recommendations.RecommendReviewDTO;
import com.review.shop.repository.recommendations.RecommendationsMapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class RecommendSqlMeasureTest {

    private static final String MAPPER = "com.review.shop.repository.recommendations.RecommendationsMapper.";
    private static final int WARMUP = 2;
    private static final int RUNS = 10;

    private static final String LEGACY_PRODUCT_SQL = """
            SELECT * FROM (
                SELECT
                    p.product_id, p.prd_name, p.prd_brand, p.price, p.rating, p.review_count,
                    (SELECT pi.IMAGE_URL FROM PRODUCT_IMAGE pi
                     WHERE pi.PRODUCT_ID = p.PRODUCT_ID AND pi.IS_THUMBNAIL = 'Y' AND ROWNUM = 1) AS image_url,
                    (SELECT REVIEW_ID FROM (
                        SELECT r.REVIEW_ID FROM REVIEW r
                        WHERE r.PRODUCT_ID = p.PRODUCT_ID ORDER BY r.LIKE_COUNT DESC
                    ) WHERE ROWNUM = 1) AS top_review_id,
                    (SELECT CONTENT FROM (
                        SELECT r.CONTENT FROM REVIEW r
                        WHERE r.PRODUCT_ID = p.PRODUCT_ID ORDER BY r.LIKE_COUNT DESC
                    ) WHERE ROWNUM = 1) AS top_review_content,
                    (SELECT LIKE_COUNT FROM (
                        SELECT r.LIKE_COUNT FROM REVIEW r
                        WHERE r.PRODUCT_ID = p.PRODUCT_ID ORDER BY r.LIKE_COUNT DESC
                    ) WHERE ROWNUM = 1) AS top_review_likes,
                    (SELECT RATING FROM (
                        SELECT r.RATING FROM REVIEW r
                        WHERE r.PRODUCT_ID = p.PRODUCT_ID ORDER BY r.LIKE_COUNT DESC
                    ) WHERE ROWNUM = 1) AS top_review_rating,
                    (
                        SELECT ri.IMAGE_URL FROM REVIEW_IMAGES ri
                        WHERE ri.REVIEW_ID = (
                            SELECT REVIEW_ID FROM (
                                SELECT r.REVIEW_ID FROM REVIEW r
                                WHERE r.PRODUCT_ID = p.PRODUCT_ID ORDER BY r.LIKE_COUNT DESC
                            ) WHERE ROWNUM = 1
                        ) AND ROWNUM = 1
                    ) AS top_review_image_url,
                    (
                        (CASE WHEN b.FIRST = ? THEN 50 WHEN b.FIRST IS NULL THEN 10 ELSE -10 END) +
                        (CASE WHEN b.SECOND = ? THEN 50 WHEN b.SECOND IS NULL THEN 10 ELSE -10 END) +
                        (CASE WHEN b.THIRD = ? THEN 50 WHEN b.THIRD IS NULL THEN 10 ELSE -10 END) +
                        (CASE WHEN b.FOURTH = ? THEN 50 WHEN b.FOURTH IS NULL THEN 10 ELSE -10 END)
                    ) AS match_score
                FROM PRODUCT p
                JOIN BAUMANN b ON p.baumann_id = b.baumann_id
            )
            WHERE match_score > 0
            ORDER BY match_score DESC, DBMS_RANDOM.VALUE, review_count DESC
            """;

    @Autowired
    private RecommendationsMapper recommendationsMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String measureUrl;

    @Value("${spring.datasource.username}")
    private String measureUsername;

    @Value("${spring.datasource.password}")
    private String measurePassword;

    @Value("${measure.perf.url}")
    private String perfUrl;

    @Value("${measure.perf.username}")
    private String perfUsername;

    @Value("${measure.perf.password}")
    private String perfPassword;

    @Test
    void recommendQueriesOnFixtureAndTwoThousandReviews() throws Exception {
        List<String> userInfo = axesOfUser(1);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("userInfo", userInfo);

        BoundSql productBound = boundSql("findRecommendProducts", parameter);
        BoundSql reviewBound = boundSql("findRecommendReviews", parameter);
        String productSql = productBound.getSql();
        String reviewSql = reviewBound.getSql();

        List<RecommendProductDTO> products = recommendationsMapper.findRecommendProducts(userInfo);
        List<RecommendReviewDTO> reviews = recommendationsMapper.findRecommendReviews(userInfo);

        int product1Score = productScore(1, userInfo);
        int product2Score = productScore(2, userInfo);
        int product16Score = productScore(16, userInfo);
        ReviewScore review1 = reviewScore(1, userInfo);
        ReviewScore review2 = reviewScore(2, userInfo);

        int fixtureProducts = count("SELECT COUNT(*) FROM product");
        int fixtureReviews = count("SELECT COUNT(*) FROM review");

        StringBuilder report = new StringBuilder();
        report.append("userAxes=").append(userInfo)
                .append(" fixtureProducts=").append(fixtureProducts)
                .append(" fixtureReviews=").append(fixtureReviews)
                .append('\n');
        report.append("productRows=").append(products.size())
                .append(" ids=").append(products.stream().map(RecommendProductDTO::getProduct_id).toList())
                .append('\n');
        report.append("productScores=");
        for (RecommendProductDTO product : products) {
            report.append(product.getProduct_id()).append(':').append(product.getTotal_score()).append(' ');
        }
        report.append("\nprobe product1=").append(product1Score)
                .append(" product2=").append(product2Score)
                .append(" product16=").append(product16Score)
                .append(" deletedProduct3InResult=").append(containsProduct(products, 3))
                .append('\n');
        report.append("reviewRows=").append(reviews.size())
                .append(" duplicateProducts=").append(duplicateProductCount(reviews))
                .append('\n');
        for (RecommendReviewDTO review : reviews) {
            report.append("review product=").append(review.getProduct_id())
                    .append(" review=").append(review.getReview_id())
                    .append(" score=").append(review.getTotal_score())
                    .append(" likes=").append(review.getLike_count())
                    .append(" checked=").append(review.getIs_checked())
                    .append('\n');
        }
        report.append("probe review1 base=").append(review1.base)
                .append(" total=").append(review1.total)
                .append(" checked=").append(review1.checked)
                .append(" review2 base=").append(review2.base)
                .append(" total=").append(review2.total)
                .append(" checked=").append(review2.checked)
                .append('\n');
        report.append("productSqlHasRowNumber=").append(productSql.contains("ROW_NUMBER"))
                .append(" reviewSqlHasRowNumber=").append(reviewSql.contains("ROW_NUMBER"))
                .append('\n');

        try (Connection measure = DriverManager.getConnection(measureUrl, measureUsername, measurePassword)) {
            QueryTime productFixture = timeQuery(measure, productSql, productBound, parameter);
            QueryTime reviewFixture = timeQuery(measure, reviewSql, reviewBound, parameter);
            report.append("fixtureJdbc product ").append(productFixture).append('\n');
            report.append("fixtureJdbc review ").append(reviewFixture).append('\n');
            report.append(runLegacy(measure, userInfo, productIds(products)));
        }

        try (Connection perf = DriverManager.getConnection(perfUrl, perfUsername, perfPassword)) {
            int perfReviews = scalar(perf, "SELECT COUNT(*) FROM review");
            int perfProducts = scalar(perf, "SELECT COUNT(*) FROM product");
            int perfUsers = scalar(perf, "SELECT COUNT(*) FROM user_table");
            report.append("perf users=").append(perfUsers)
                    .append(" products=").append(perfProducts)
                    .append(" reviews=").append(perfReviews)
                    .append('\n');
            QueryTime productPerf = timeQuery(perf, productSql, productBound, parameter);
            QueryTime reviewPerf = timeQuery(perf, reviewSql, reviewBound, parameter);
            report.append("perfJdbc product ").append(productPerf).append('\n');
            report.append("perfJdbc review ").append(reviewPerf).append('\n');
            report.append("explain product\n").append(explain(perf, productSql, productBound, parameter));
            report.append("explain review\n").append(explain(perf, reviewSql, reviewBound, parameter));
            assertEquals(2000, perfReviews, report.toString());
            assertEquals(200, perfProducts, report.toString());
            assertEquals(20, perfUsers, report.toString());
        }

        System.out.println(report);

        assertEquals(List.of("D", "S", "P", "W"), userInfo, report.toString());
        assertEquals(20, fixtureProducts, report.toString());
        assertEquals(40, fixtureReviews, report.toString());
        assertFalse(productSql.contains("ROW_NUMBER"), report.toString());
        assertTrue(reviewSql.contains("ROW_NUMBER"), report.toString());

        assertEquals(16, products.size(), report.toString());
        assertEquals(4, products.get(0).getProduct_id(), report.toString());
        assertEquals(140, products.get(0).getTotal_score(), report.toString());
        assertEquals(120, scoreOf(products, 1), report.toString());
        assertEquals(120, product1Score, report.toString());
        assertEquals(60, product2Score, report.toString());
        assertEquals(0, product16Score, report.toString());
        assertFalse(containsProduct(products, 2), report.toString());
        assertFalse(containsProduct(products, 16), report.toString());

        assertEquals(16, reviews.size(), report.toString());
        assertEquals(0, duplicateProductCount(reviews), report.toString());
        RecommendReviewDTO product1Review = reviews.stream()
                .filter(review -> review.getProduct_id() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals(2, product1Review.getReview_id(), report.toString());
        assertFalse(reviews.stream().anyMatch(review -> review.getReview_id() == 1), report.toString());
        assertEquals(72, product1Review.getTotal_score(), report.toString());
        assertTrue(review1.base > review2.base, report.toString());
        assertTrue(review2.total > review1.total, report.toString());
        assertEquals(0, review1.checked, report.toString());
        assertEquals(1, review2.checked, report.toString());
    }

    private String runLegacy(Connection connection, List<String> userInfo, List<Integer> currentIds) {
        StringBuilder report = new StringBuilder();
        try {
            List<Integer> counts = new ArrayList<>();
            long[] nanos = new long[RUNS];
            for (int i = 0; i < WARMUP; i++) {
                readLegacyIds(connection, userInfo);
            }
            LinkedHashSet<Integer> ids = new LinkedHashSet<>();
            for (int i = 0; i < RUNS; i++) {
                long start = System.nanoTime();
                List<Integer> rows = readLegacyIds(connection, userInfo);
                nanos[i] = System.nanoTime() - start;
                counts.add(rows.size());
                ids.addAll(rows);
            }
            Set<Integer> current = new LinkedHashSet<>(currentIds);
            Set<Integer> onlyCurrent = new LinkedHashSet<>(current);
            onlyCurrent.removeAll(ids);
            Set<Integer> onlyLegacy = new LinkedHashSet<>(ids);
            onlyLegacy.removeAll(current);
            report.append("legacy e7c2fa82 findRecommencementsWithAll executed rowsPerRun=").append(counts)
                    .append(' ').append(formatTime(nanos))
                    .append(" distinctIds=").append(ids.size())
                    .append(" onlyCurrent=").append(onlyCurrent)
                    .append(" onlyLegacy=").append(onlyLegacy)
                    .append('\n');
            report.append("legacy review SQL is absent at e7c2fa82\n");
        } catch (SQLException exception) {
            report.append("legacy e7c2fa82 unverified ")
                    .append(exception.getClass().getSimpleName())
                    .append(": ")
                    .append(exception.getMessage())
                    .append('\n');
        }
        return report.toString();
    }

    private List<Integer> readLegacyIds(Connection connection, List<String> userInfo) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(LEGACY_PRODUCT_SQL)) {
            for (int i = 0; i < userInfo.size(); i++) {
                statement.setString(i + 1, userInfo.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("product_id"));
                }
            }
        }
        return ids;
    }

    private List<String> axesOfUser(int userId) {
        Integer baumannId = recommendationsMapper.getBaumannTypeByUserId(userId);
        BaumannDTO baumann = recommendationsMapper.getBaumannDTOWithId(baumannId);
        return List.of(baumann.getFirst(), baumann.getSecond(), baumann.getThird(), baumann.getFourth());
    }

    private BoundSql boundSql(String statementId, Map<String, Object> parameter) {
        MappedStatement statement = sqlSessionFactory.getConfiguration().getMappedStatement(MAPPER + statementId);
        return statement.getBoundSql(parameter);
    }

    private QueryTime timeQuery(Connection connection, String sql, BoundSql boundSql, Map<String, Object> parameter)
            throws SQLException {
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < WARMUP; i++) {
            readCount(connection, sql, boundSql, parameter);
        }
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            counts.add(readCount(connection, sql, boundSql, parameter));
            nanos[i] = System.nanoTime() - start;
        }
        return new QueryTime(counts, nanos);
    }

    private int readCount(Connection connection, String sql, BoundSql boundSql, Map<String, Object> parameter)
            throws SQLException {
        int rows = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, boundSql, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows++;
                }
            }
        }
        return rows;
    }

    private void bind(PreparedStatement statement, BoundSql boundSql, Map<String, Object> parameter) throws SQLException {
        MetaObject metaObject = sqlSessionFactory.getConfiguration().newMetaObject(parameter);
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        for (int i = 0; i < mappings.size(); i++) {
            ParameterMapping mapping = mappings.get(i);
            Object value = boundSql.hasAdditionalParameter(mapping.getProperty())
                    ? boundSql.getAdditionalParameter(mapping.getProperty())
                    : metaObject.getValue(mapping.getProperty());
            @SuppressWarnings("unchecked")
            TypeHandler<Object> typeHandler = (TypeHandler<Object>) mapping.getTypeHandler();
            typeHandler.setParameter(statement, i + 1, value, mapping.getJdbcType());
        }
    }

    private String explain(Connection connection, String sql, BoundSql boundSql, Map<String, Object> parameter)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("EXPLAIN PLAN FOR " + sql)) {
            bind(statement, boundSql, parameter);
            statement.execute();
        }
        StringBuilder plan = new StringBuilder();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT PLAN_TABLE_OUTPUT FROM TABLE(DBMS_XPLAN.DISPLAY())")) {
            while (resultSet.next()) {
                plan.append(resultSet.getString(1)).append('\n');
            }
        }
        return plan.toString();
    }

    private int productScore(int productId, List<String> axes) {
        Integer score = jdbcTemplate.queryForObject("""
                SELECT (
                    (CASE WHEN pb.FIRST = ? THEN 30 WHEN pb.FIRST IS NULL THEN 10 ELSE 0 END) +
                    (CASE WHEN pb.SECOND = ? THEN 30 WHEN pb.SECOND IS NULL THEN 10 ELSE 0 END) +
                    (CASE WHEN pb.THIRD = ? THEN 30 WHEN pb.THIRD IS NULL THEN 10 ELSE 0 END) +
                    (CASE WHEN pb.FOURTH = ? THEN 30 WHEN pb.FOURTH IS NULL THEN 10 ELSE 0 END)
                ) + CASE
                    WHEN p.review_count >= 50 THEN 30
                    WHEN p.review_count >= 30 THEN 20
                    WHEN p.review_count >= 10 THEN 10
                    ELSE 0 END
                FROM product p
                JOIN baumann pb ON p.baumann_id = pb.baumann_id
                WHERE p.product_id = ?
                """, Integer.class, axes.get(0), axes.get(1), axes.get(2), axes.get(3), productId);
        return score == null ? -1 : score;
    }

    private ReviewScore reviewScore(int reviewId, List<String> axes) {
        return jdbcTemplate.queryForObject("""
                SELECT (
                    (CASE WHEN rb.FIRST = ? THEN 10 ELSE 0 END) +
                    (CASE WHEN rb.SECOND = ? THEN 10 ELSE 0 END) +
                    (CASE WHEN rb.THIRD = ? THEN 10 ELSE 0 END) +
                    (CASE WHEN rb.FOURTH = ? THEN 10 ELSE 0 END)
                ) + (
                    (CASE WHEN pb.FIRST = ? THEN 5 WHEN pb.FIRST IS NULL THEN 3 ELSE 0 END) +
                    (CASE WHEN pb.SECOND = ? THEN 5 WHEN pb.SECOND IS NULL THEN 3 ELSE 0 END) +
                    (CASE WHEN pb.THIRD = ? THEN 5 WHEN pb.THIRD IS NULL THEN 3 ELSE 0 END) +
                    (CASE WHEN pb.FOURTH = ? THEN 5 WHEN pb.FOURTH IS NULL THEN 3 ELSE 0 END)
                ) + CASE
                    WHEN ABS(r.rating - p.rating) <= 0.5 THEN 10
                    WHEN ABS(r.rating - p.rating) <= 1.0 THEN 5
                    ELSE 0 END AS base_score,
                    r.is_checked
                FROM review r
                JOIN product p ON r.product_id = p.product_id
                JOIN user_table u ON r.user_id = u.user_id
                JOIN baumann rb ON u.baumann_id = rb.baumann_id
                JOIN baumann pb ON p.baumann_id = pb.baumann_id
                WHERE r.review_id = ?
                """, (rs, rowNum) -> {
            double base = rs.getDouble("base_score");
            int checked = rs.getInt("is_checked");
            double total = checked == 1 ? base * 1.2 : base;
            return new ReviewScore(reviewId, base, total, checked);
        }, axes.get(0), axes.get(1), axes.get(2), axes.get(3),
                axes.get(0), axes.get(1), axes.get(2), axes.get(3), reviewId);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? -1 : value;
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static int duplicateProductCount(List<RecommendReviewDTO> reviews) {
        Set<Integer> seen = new LinkedHashSet<>();
        int duplicates = 0;
        for (RecommendReviewDTO review : reviews) {
            if (!seen.add(review.getProduct_id())) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private static boolean containsProduct(List<RecommendProductDTO> products, int productId) {
        return products.stream().anyMatch(product -> product.getProduct_id() == productId);
    }

    private static int scoreOf(List<RecommendProductDTO> products, int productId) {
        return products.stream()
                .filter(product -> product.getProduct_id() == productId)
                .map(RecommendProductDTO::getTotal_score)
                .findFirst()
                .orElse(-1);
    }

    private static List<Integer> productIds(List<RecommendProductDTO> products) {
        return products.stream().map(RecommendProductDTO::getProduct_id).toList();
    }

    private static String formatTime(long[] nanos) {
        long max = Long.MIN_VALUE;
        long sum = 0;
        StringBuilder samples = new StringBuilder();
        for (int i = 0; i < nanos.length; i++) {
            sum += nanos[i];
            max = Math.max(max, nanos[i]);
            if (i > 0) {
                samples.append(',');
            }
            samples.append(String.format(Locale.US, "%.3f", nanos[i] / 1_000_000d));
        }
        return String.format(Locale.US, "avg=%.3fms max=%.3fms samples=%s",
                (sum / (double) nanos.length) / 1_000_000d, max / 1_000_000d, samples);
    }

    private record ReviewScore(int reviewId, double base, double total, int checked) {
    }

    private record QueryTime(List<Integer> rowCounts, long[] nanos) {
        @Override
        public String toString() {
            return "rows=" + rowCounts + " " + formatTime(nanos);
        }
    }
}
