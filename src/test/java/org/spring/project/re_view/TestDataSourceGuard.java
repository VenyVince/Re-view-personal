package org.spring.project.re_view;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.Locale;

/**
 * Stops the test profile from opening the development database.
 */
public class TestDataSourceGuard implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isTestProfile(environment)) {
            return;
        }

        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        if (url == null || url.isBlank() || username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "테스트 프로파일에는 TEST_DATASOURCE_URL, TEST_DATASOURCE_USERNAME, TEST_DATASOURCE_PASSWORD가 필요하다. 개발 DB 설정으로 대체하지 않는다.");
        }

        String normalizedUrl = url.toLowerCase(Locale.ROOT);
        String normalizedUser = username.trim().toUpperCase(Locale.ROOT);
        boolean testTarget = normalizedUrl.contains(":1522") && normalizedUrl.contains("/reviewtest");
        boolean devTarget = normalizedUrl.contains(":1521")
                || normalizedUrl.contains("xepdb1")
                || normalizedUrl.contains("oracle-data");
        if (!testTarget || devTarget || !"REVIEW_TEST".equals(normalizedUser)) {
            throw new IllegalStateException(
                    "테스트 프로파일은 포트 1522, 서비스 REVIEWTEST, 계정 REVIEW_TEST에만 연결할 수 있다. url="
                            + url + ", username=" + username);
        }
    }

    private boolean isTestProfile(ConfigurableEnvironment environment) {
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            return true;
        }
        String configured = environment.getProperty("spring.profiles.active", "");
        return Arrays.asList(configured.toLowerCase(Locale.ROOT).split(",")).contains("test");
    }
}
