package com.review.shop.testsupport;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@Profile("test")
public class TestExternalServiceConfig {

    @Bean
    JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }
}
