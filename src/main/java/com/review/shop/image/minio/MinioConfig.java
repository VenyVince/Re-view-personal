package com.review.shop.image.minio;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    // MinIO Private URL
    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(minioProperties.getRootUser(), minioProperties.getRootPassword())
                .build();
    }

    // MINIO Public URL
    @Bean
    public MinioClient publicMinioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getPublicUrl())
                .credentials(minioProperties.getRootUser(), minioProperties.getRootPassword())
                .build();
    }
}
