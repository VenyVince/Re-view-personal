package com.review.shop.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityEndpointSecurityTest.ProbeController.class)
@ContextConfiguration(classes = {
        SecurityEndpointSecurityTest.TestApplication.class,
        SecurityEndpointSecurityTest.ProbeController.class,
        SecurityConfig.class,
        CorsConfig.class
})
class SecurityEndpointSecurityTest {

    private final MockMvc mockMvc;

    @Autowired
    SecurityEndpointSecurityTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("public GET endpoints are accessible without login")
    void publicGetEndpointIsAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().string("products"));
    }

    @Test
    @DisplayName("protected GET endpoints return 401 without login")
    void protectedGetEndpointReturnsUnauthorizedWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("Unauthorized")))
                .andExpect(jsonPath("$.path", is("/api/auth/me")));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("protected GET endpoints are accessible with login")
    void protectedGetEndpointIsAccessibleWithLogin() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("me"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("admin endpoints return 403 for non-admin user")
    void adminEndpointReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("Access Denied")))
                .andExpect(jsonPath("$.path", is("/api/admin/users")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin endpoints are accessible with admin role")
    void adminEndpointIsAccessibleWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-users"));
    }

    @Test
    @DisplayName("public login endpoint is accessible without login")
    void publicLoginEndpointIsAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("login"));
    }

    @Test
    @DisplayName("protected POST endpoints return 401 without login when CSRF is valid")
    void protectedPostEndpointReturnsUnauthorizedWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/reviews/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/reviews/1")));
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/products")
        String products() {
            return "products";
        }

        @GetMapping("/api/auth/me")
        String me() {
            return "me";
        }

        @GetMapping("/api/admin/users")
        String adminUsers() {
            return "admin-users";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "login";
        }

        @PostMapping("/api/reviews/{productId}")
        String createReview() {
            return "create-review";
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
