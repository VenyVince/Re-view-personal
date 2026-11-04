package org.spring.project.re_view.measure;

import com.review.shop.dto.search.header.HeaderSearchProductDTO;
import com.review.shop.dto.search.header.HeaderSearchReviewDTO;
import com.review.shop.repository.search.header.HeaderSearchProductMapper;
import com.review.shop.repository.search.header.HeaderSearchReviewMapper;
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
class HeaderSearchMeasureTest {

    private static final String PRODUCT_MAPPER =
            "com.review.shop.repository.search.header.HeaderSearchProductMapper.searchProducts";
    private static final String REVIEW_MAPPER =
            "com.review.shop.repository.search.header.HeaderSearchReviewMapper.searchReviews";
    private static final int WARMUP = 2;
    private static final int RUNS = 10;

    @Autowired
    private HeaderSearchProductMapper productMapper;

    @Autowired
    private HeaderSearchReviewMapper reviewMapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

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
    void headerSearchCasesAndCurrentSqlTime() throws Exception {
        List<HeaderSearchProductDTO> byName = products("측정토너", null, null, null);
        List<HeaderSearchProductDTO> byPartial = products("측정", null, null, null);
        List<HeaderSearchProductDTO> byLatest = products("측정", "latest", null, null);
        List<HeaderSearchProductDTO> byLowPrice = products("측정", "low_price", null, null);
        List<HeaderSearchProductDTO> byBrand = products("측정", null, null, "MeasureBrand");
        List<HeaderSearchProductDTO> brandInCategorySlot = products("측정", null, "MeasureBrand", null);
        List<HeaderSearchProductDTO> oneCharacter = products("측", null, null, null);
        List<HeaderSearchReviewDTO> reviews = reviewMapper.searchReviews("리뷰검색어", null, null, null);

        String productSql = boundSql(PRODUCT_MAPPER, "측정", null, null, null).getSql();
        String reviewSql = boundSql(REVIEW_MAPPER, "리뷰검색어", null, null, null).getSql();

        StringBuilder report = new StringBuilder();
        report.append("nameIds=").append(productIds(byName))
                .append(" partialIds=").append(productIds(byPartial))
                .append(" latestIds=").append(productIds(byLatest))
                .append(" lowPriceIds=").append(productIds(byLowPrice))
                .append('\n');
        report.append("brandIds=").append(productIds(byBrand))
                .append(" brandPassedAsCategory=").append(productIds(brandInCategorySlot))
                .append(" oneCharacterIds=").append(productIds(oneCharacter))
                .append('\n');
        report.append("reviewIds=");
        for (HeaderSearchReviewDTO review : reviews) {
            report.append(review.getReview_id()).append(':').append(review.getContent()).append(' ');
        }
        report.append("\nproductSqlHasLike=").append(productSql.contains("LIKE"))
                .append(" productSqlDeletes=").append(productSql.contains("deleted_at IS NULL"))
                .append(" productSqlHasLength=").append(productSql.toLowerCase(Locale.ROOT).contains("length"))
                .append(" reviewSqlHasLike=").append(reviewSql.contains("LIKE"))
                .append('\n');

        try (Connection measure = DriverManager.getConnection(measureUrl, measureUsername, measurePassword)) {
            report.append("fixture product ").append(time(measure, PRODUCT_MAPPER, "측정", null, null, null)).append('\n');
            report.append("fixture review ").append(time(measure, REVIEW_MAPPER, "리뷰검색어", null, null, null)).append('\n');
        }

        try (Connection perf = DriverManager.getConnection(perfUrl, perfUsername, perfPassword)) {
            report.append("perf users=").append(scalar(perf, "SELECT COUNT(*) FROM user_table"))
                    .append(" products=").append(scalar(perf, "SELECT COUNT(*) FROM product"))
                    .append(" reviews=").append(scalar(perf, "SELECT COUNT(*) FROM review"))
                    .append('\n');
            report.append("perf product ").append(time(perf, PRODUCT_MAPPER, "성능", null, null, null)).append('\n');
            report.append("perf review ").append(time(perf, REVIEW_MAPPER, "성능", null, null, null)).append('\n');
            report.append("explain product\n").append(explain(perf, PRODUCT_MAPPER, "성능", null, null, null));
            report.append("explain review\n").append(explain(perf, REVIEW_MAPPER, "성능", null, null, null));
        }

        System.out.println(report);

        assertEquals(List.of("1"), productIds(byName), report.toString());
        assertFalse(productIds(byName).contains("3"), report.toString());
        assertEquals(List.of("1", "2"), productIds(byPartial), report.toString());
        assertEquals(productIds(byPartial), productIds(byLatest), report.toString());
        assertEquals(List.of("2", "1"), productIds(byLowPrice), report.toString());
        assertEquals(List.of("1"), productIds(byBrand), report.toString());
        assertEquals(List.of(), productIds(brandInCategorySlot), report.toString());
        assertEquals(List.of("1", "2"), productIds(oneCharacter), report.toString());
        assertEquals(0, duplicateCount(productIds(byPartial)), report.toString());
        assertEquals(1, reviews.size(), report.toString());
        assertEquals(3, reviews.get(0).getReview_id(), report.toString());
        assertTrue(reviews.get(0).getContent().contains("리뷰검색어"), report.toString());
        assertTrue(productSql.contains("deleted_at IS NULL"), report.toString());
        assertFalse(productSql.toLowerCase(Locale.ROOT).contains("length"), report.toString());
        assertTrue(reviewSql.contains("deleted_at IS NULL"), report.toString());
    }

    private List<HeaderSearchProductDTO> products(String keyword, String sort, String filterCategory, String filterBrand) {
        return productMapper.searchProducts(keyword, sort, filterCategory, filterBrand);
    }

    private BoundSql boundSql(String statementId, String keyword, String sort, String filterCategory, String filterBrand) {
        MappedStatement statement = sqlSessionFactory.getConfiguration().getMappedStatement(statementId);
        return statement.getBoundSql(params(keyword, sort, filterCategory, filterBrand));
    }

    private Map<String, Object> params(String keyword, String sort, String filterCategory, String filterBrand) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("sort", sort);
        params.put("filter_category", filterCategory);
        params.put("filter_brand", filterBrand);
        return params;
    }

    private QueryTime time(Connection connection, String statementId, String keyword, String sort,
                           String filterCategory, String filterBrand) throws SQLException {
        Map<String, Object> params = params(keyword, sort, filterCategory, filterBrand);
        BoundSql sql = boundSql(statementId, keyword, sort, filterCategory, filterBrand);
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < WARMUP; i++) {
            readCount(connection, sql, params);
        }
        long[] nanos = new long[RUNS];
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            counts.add(readCount(connection, sql, params));
            nanos[i] = System.nanoTime() - start;
        }
        return new QueryTime(counts, nanos);
    }

    private int readCount(Connection connection, BoundSql boundSql, Map<String, Object> params) throws SQLException {
        int rows = 0;
        try (PreparedStatement statement = connection.prepareStatement(boundSql.getSql())) {
            bind(statement, boundSql, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows++;
                }
            }
        }
        return rows;
    }

    private void bind(PreparedStatement statement, BoundSql boundSql, Map<String, Object> params) throws SQLException {
        MetaObject metaObject = sqlSessionFactory.getConfiguration().newMetaObject(params);
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

    private String explain(Connection connection, String statementId, String keyword, String sort,
                           String filterCategory, String filterBrand) throws SQLException {
        Map<String, Object> params = params(keyword, sort, filterCategory, filterBrand);
        BoundSql boundSql = boundSql(statementId, keyword, sort, filterCategory, filterBrand);
        try (PreparedStatement statement = connection.prepareStatement("EXPLAIN PLAN FOR " + boundSql.getSql())) {
            bind(statement, boundSql, params);
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

    private int scalar(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static List<String> productIds(List<HeaderSearchProductDTO> products) {
        return products.stream().map(HeaderSearchProductDTO::getProduct_id).toList();
    }

    private static int duplicateCount(List<String> ids) {
        Set<String> seen = new LinkedHashSet<>();
        int duplicates = 0;
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates++;
            }
        }
        return duplicates;
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

    private record QueryTime(List<Integer> rowCounts, long[] nanos) {
        @Override
        public String toString() {
            return "rows=" + rowCounts + " " + formatTime(nanos);
        }
    }
}
