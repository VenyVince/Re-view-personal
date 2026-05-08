package com.review.shop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.shop.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler; // ★ 필수 import
import java.time.LocalDateTime;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@AllArgsConstructor
public class SecurityConfig {
    private final ObjectMapper objectMapper;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/auth/login")
                )

                .cors(withDefaults())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/check-id",
                                "/api/auth/find-id",
                                "/api/auth/send-temp-password"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/products",
                                "/api/products/*",
                                "/api/reviews",
                                "/api/reviews/*",
                                "/api/reviews/*/reviews",
                                "/api/products/*/reviews/search",
                                "/api/search",
                                "/api/images/banners",
                                "/api/recommendations/admin-pick",
                                "/api/qna/list/*"
                                ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/my-baumann-type",
                                "/api/auth/reset-password",
                                "/api/users/me/**",
                                "/api/addresses/**",
                                "/api/cart/**",
                                "/api/wishlist/**",
                                "/api/orders/**",
                                "/api/users/me/payments/**",
                                "/api/users/me/points/**",
                                "/api/reviews/exists/**",
                                "/api/reviews/*/comments",
                                "/api/reviews/comments/**",
                                "/api/reviews/*/reaction",
                                "/api/reviews/*/report",
                                "/api/users/reviews/search",
                                "/api/qna/my",
                                "/api/images/products/convert-data",
                                "/api/images/products/convert-datas",
                                "/api/recommendations/all"
                                ).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/*/*").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/qna").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/qna").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/qna/*").authenticated()

                        .anyRequest().permitAll()
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                    HttpStatus.FORBIDDEN.value(),
                                    "Access Denied",
                                    "접근 권한이 부족합니다.",
                                    request.getRequestURI(),
                                    LocalDateTime.now()
                            );
                            response.getWriter().write(
                                    objectMapper.writeValueAsString(errorResponseDTO)
                            );
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            ErrorResponseDTO errorResponseDTO = new ErrorResponseDTO(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "Unauthorized",
                                    "로그인이 필요합니다.",
                                    request.getRequestURI(),
                                    LocalDateTime.now()
                            );
                            response.getWriter().write(
                                    objectMapper.writeValueAsString(errorResponseDTO)
                            );
                        })
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("로그아웃 되었습니다.");
                        })
                );

        return http.build();
    }
}
