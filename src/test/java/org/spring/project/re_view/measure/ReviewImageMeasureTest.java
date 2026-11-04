package org.spring.project.re_view.measure;

import com.review.shop.exception.WrongRequestException;
import com.review.shop.image.ImageService;
import com.review.shop.image.minio.MinioProperties;
import com.review.shop.service.review.ProductReviewService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("measure")
@Tag("measure")
class ReviewImageMeasureTest {

    private static final int REVIEW_ID = 1;
    private static final int USER_ID = 1;
    private static final String LOGIN_ID = "measure_user";
    private static final String ORIGINAL_CONTENT = "풀매치 기본";
    private static final double ORIGINAL_RATING = 4.5;
    private static final String ORIGINAL_KEY = "reviews/fixture/review-1.jpg";
    private static final byte[] FILE_BYTES = "measure-image".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void keepReplaceAndDirectUpload() throws Exception {
        ensureBucket();
        StringBuilder report = new StringBuilder();
        try {
            runAsUser(() -> {
                List<String> before = imageKeys();
                String emptyContent = updateOrMessage("", 4.0, List.of());
                List<String> afterEmptyContent = imageKeys();
                String contentAfterEmpty = content();

                productReviewService.updateReview(REVIEW_ID, USER_ID, "측정수정", 4.0, List.of());
                List<String> afterEmptyImages = imageKeys();

                productReviewService.updateReview(
                        REVIEW_ID, USER_ID, "측정수정", 4.0, List.of("http://example.com/old.jpg"));
                List<String> afterHttp = imageKeys();

                Map<String, Object> issued = imageService.uploadReviewImages("measure.jpg");
                String objectKey = String.valueOf(issued.get("objectKey"));
                String putUrl = String.valueOf(issued.get("presignedUrl"));
                boolean issuedContainsFile = issued.values().stream().anyMatch(value ->
                        value instanceof byte[] || String.valueOf(value).contains("measure-image"));
                int putStatus = putBytes(putUrl, FILE_BYTES);

                productReviewService.updateReview(REVIEW_ID, USER_ID, "측정수정", 4.0, List.of(objectKey));
                List<String> afterReplace = imageKeys();
                String getUrl = imageService.presignedUrlGet(objectKey);
                StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .build());

                report.append("before=").append(before)
                        .append(" emptyContent=").append(emptyContent)
                        .append(" keysAfterEmptyContent=").append(afterEmptyContent)
                        .append(" contentAfterEmpty=").append(contentAfterEmpty)
                        .append('\n');
                report.append("afterEmptyImages=").append(afterEmptyImages)
                        .append(" afterHttp=").append(afterHttp)
                        .append('\n');
                report.append("issuedKeys=").append(issued.keySet())
                        .append(" objectKey=").append(objectKey)
                        .append(" putExpires=").append(queryParam(putUrl, "X-Amz-Expires"))
                        .append(" getExpires=").append(queryParam(getUrl, "X-Amz-Expires"))
                        .append(" issuedContainsFile=").append(issuedContainsFile)
                        .append(" putStatus=").append(putStatus)
                        .append(" afterReplace=").append(afterReplace)
                        .append(" objectSize=").append(stat.size())
                        .append('\n');
                System.out.println(report);

                assertEquals(List.of(ORIGINAL_KEY), before, report.toString());
                assertEquals("WrongRequestException: 리뷰 내용이 필수입니다", emptyContent, report.toString());
                assertEquals(before, afterEmptyContent, report.toString());
                assertEquals(ORIGINAL_CONTENT, contentAfterEmpty, report.toString());
                assertEquals(before, afterEmptyImages, report.toString());
                assertEquals(before, afterHttp, report.toString());
                assertFalse(afterHttp.stream().anyMatch(key -> key.startsWith("http")), report.toString());
                assertTrue(objectKey.startsWith("reviews/"), report.toString());
                assertTrue(putUrl.startsWith("http"), report.toString());
                assertFalse(issuedContainsFile, report.toString());
                assertEquals("600", queryParam(putUrl, "X-Amz-Expires"), report.toString());
                assertEquals(200, putStatus, report.toString());
                assertEquals(List.of(objectKey), afterReplace, report.toString());
                assertFalse(afterReplace.get(0).startsWith("http"), report.toString());
                assertNotEquals(putUrl, getUrl, report.toString());
                assertEquals("300", queryParam(getUrl, "X-Amz-Expires"), report.toString());
                assertEquals(FILE_BYTES.length, stat.size(), report.toString());
            });
        } finally {
            jdbcTemplate.update("DELETE FROM review_images WHERE review_id = ?", REVIEW_ID);
            jdbcTemplate.update("""
                    INSERT INTO review_images (review_id, image_url)
                    VALUES (?, ?)
                    """, REVIEW_ID, ORIGINAL_KEY);
            jdbcTemplate.update("""
                    UPDATE review
                    SET content = ?, rating = ?, updated_at = NULL
                    WHERE review_id = ?
                    """, ORIGINAL_CONTENT, ORIGINAL_RATING, REVIEW_ID);
        }
    }

    private void ensureBucket() throws Exception {
        String bucket = minioProperties.getBucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String updateOrMessage(String content, double rating, List<String> images) {
        try {
            productReviewService.updateReview(REVIEW_ID, USER_ID, content, rating, images);
            return "no exception";
        } catch (WrongRequestException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private List<String> imageKeys() {
        return jdbcTemplate.queryForList(
                "SELECT image_url FROM review_images WHERE review_id = ? ORDER BY review_image_id",
                String.class,
                REVIEW_ID);
    }

    private String content() {
        return jdbcTemplate.queryForObject(
                "SELECT content FROM review WHERE review_id = ?", String.class, REVIEW_ID);
    }

    private int putBytes(String url, byte[] body) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private static String queryParam(String url, String name) {
        String query = URI.create(url).getRawQuery();
        if (query == null) {
            return "";
        }
        for (String pair : query.split("&")) {
            int mark = pair.indexOf('=');
            if (mark < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, mark), StandardCharsets.UTF_8);
            if (name.equals(key)) {
                return URLDecoder.decode(pair.substring(mark + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private void runAsUser(CheckedAction action) throws Exception {
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

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
