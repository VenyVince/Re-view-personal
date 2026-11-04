package org.spring.project.re_view.measure;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class MeasureConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${measure.perf.url}")
    private String perfUrl;

    @Value("${measure.perf.username}")
    private String perfUsername;

    @Value("${measure.perf.password}")
    private String perfPassword;

    @Test
    void fixtureIsLoadedThroughMeasureProfile() {
        assertEquals(5, count("SELECT COUNT(*) FROM user_table"));
        assertEquals(20, count("SELECT COUNT(*) FROM product"));
        assertEquals(40, count("SELECT COUNT(*) FROM review"));
        assertEquals(4, count("SELECT COUNT(*) FROM order_item"));
        assertEquals(10000, count("SELECT point FROM user_table WHERE user_id = 1"));
    }

    @Test
    void performanceSchemaHasTwoThousandReviews() throws Exception {
        try (Connection connection = DriverManager.getConnection(perfUrl, perfUsername, perfPassword);
             Statement statement = connection.createStatement()) {
            assertEquals(20, queryCount(statement, "SELECT COUNT(*) FROM user_table"));
            assertEquals(200, queryCount(statement, "SELECT COUNT(*) FROM product"));
            assertEquals(2000, queryCount(statement, "SELECT COUNT(*) FROM review"));
        }
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? -1 : value;
    }

    private int queryCount(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
