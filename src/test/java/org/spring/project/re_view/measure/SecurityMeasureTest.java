package org.spring.project.re_view.measure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("measure")
@Tag("measure")
class SecurityMeasureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fiveRepresentativeRequests() throws Exception {
        MvcResult products = mockMvc.perform(get("/api/products")).andReturn();
        MvcResult qna = mockMvc.perform(post("/api/qna")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn();
        MvcResult admin = mockMvc.perform(get("/api/admin/reviews")
                .with(user("measure_user").roles("USER"))).andReturn();
        MvcResult outside = mockMvc.perform(get("/api/not-in-matchers")).andReturn();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andReturn();

        String qnaBody = body(qna);
        String adminBody = body(admin);
        String loginBody = body(login);
        JsonNode qnaJson = objectMapper.readTree(qnaBody);
        JsonNode adminJson = objectMapper.readTree(adminBody);
        JsonNode loginJson = objectMapper.readTree(loginBody);

        String report = """
                products=%d array=%s
                qna=%d %s
                admin=%d %s
                outside=%d %s
                login=%d %s
                """.formatted(
                products.getResponse().getStatus(), body(products).startsWith("["),
                qna.getResponse().getStatus(), qnaBody,
                admin.getResponse().getStatus(), adminBody,
                outside.getResponse().getStatus(), body(outside),
                login.getResponse().getStatus(), loginBody);
        System.out.println(report);

        assertEquals(200, products.getResponse().getStatus(), report);
        assertTrue(body(products).startsWith("["), report);

        assertEquals(403, qna.getResponse().getStatus(), report);
        assertError(qnaJson, 403, "Access Denied", "접근 권한이 부족합니다.", "/api/qna", report);

        assertEquals(403, admin.getResponse().getStatus(), report);
        assertError(adminJson, 403, "Access Denied", "접근 권한이 부족합니다.", "/api/admin/reviews", report);

        assertEquals(500, outside.getResponse().getStatus(), report);
        assertError(objectMapper.readTree(body(outside)), 500, "Other Exception!",
                "No static resource api/not-in-matchers.", "/api/not-in-matchers", report);

        assertEquals(400, login.getResponse().getStatus(), report);
        assertError(loginJson, 400, "Validation Failed", loginJson.path("message").asText(), "/api/auth/login", report);
        assertTrue(loginJson.path("message").asText().contains("id: 아이디를 입력해주세요."), report);
        assertTrue(loginJson.path("message").asText().contains("password: 비밀번호를 입력해주세요."), report);
    }

    private static void assertError(JsonNode json, int status, String code, String message, String path, String report) {
        assertEquals(status, json.path("status").asInt(), report);
        assertEquals(code, json.path("code").asText(), report);
        assertEquals(message, json.path("message").asText(), report);
        assertEquals(path, json.path("path").asText(), report);
        assertTrue(!json.path("timestamp").isMissingNode(), report);
        assertTrue(!json.path("timestamp").asText().isBlank(), report);
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
