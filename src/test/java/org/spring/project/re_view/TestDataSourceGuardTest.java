package org.spring.project.re_view;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestDataSourceGuardTest {

    private static final String TEST_URL = "jdbc:oracle:thin:@//127.0.0.1:1522/REVIEWTEST";
    private static final String TEST_USER = "REVIEW_TEST";
    private static final String TEST_PASSWORD = "unit-test-only";

    private final TestDataSourceGuard guard = new TestDataSourceGuard();

    @Test
    void allowsTheTestDatabase() {
        assertDoesNotThrow(() -> apply(TEST_URL, TEST_USER, TEST_PASSWORD));
    }

    @Test
    void blocksMissingConnectionSettings() {
        assertThrows(IllegalStateException.class, () -> apply(null, TEST_USER, TEST_PASSWORD));
        assertThrows(IllegalStateException.class, () -> apply(TEST_URL, null, TEST_PASSWORD));
        assertThrows(IllegalStateException.class, () -> apply(TEST_URL, TEST_USER, null));
        assertThrows(IllegalStateException.class, () -> apply("  ", TEST_USER, TEST_PASSWORD));
    }

    @Test
    void blocksDevelopmentPort() {
        assertThrows(IllegalStateException.class, () -> apply(
                "jdbc:oracle:thin:@//127.0.0.1:1521/REVIEWTEST", TEST_USER, TEST_PASSWORD));
    }

    @Test
    void blocksDevelopmentServiceName() {
        assertThrows(IllegalStateException.class, () -> apply(
                "jdbc:oracle:thin:@//127.0.0.1:1522/XEPDB1", TEST_USER, TEST_PASSWORD));
    }

    @Test
    void blocksDevelopmentAccount() {
        assertThrows(IllegalStateException.class, () -> apply(TEST_URL, "REVIEW", TEST_PASSWORD));
    }

    @Test
    void blocksWrongServiceOnTheTestPort() {
        assertThrows(IllegalStateException.class, () -> apply(
                "jdbc:oracle:thin:@//127.0.0.1:1522/OTHERDB", TEST_USER, TEST_PASSWORD));
    }

    @Test
    void blocksWrongAccountOnTheTestService() {
        assertThrows(IllegalStateException.class, () -> apply(TEST_URL, "OTHER_USER", TEST_PASSWORD));
    }

    private void apply(String url, String username, String password) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("test");
        Map<String, Object> values = new HashMap<>();
        if (url != null) {
            values.put("spring.datasource.url", url);
        }
        if (username != null) {
            values.put("spring.datasource.username", username);
        }
        if (password != null) {
            values.put("spring.datasource.password", password);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("guard-test", values));
        guard.postProcessEnvironment(environment, new SpringApplication());
    }
}
