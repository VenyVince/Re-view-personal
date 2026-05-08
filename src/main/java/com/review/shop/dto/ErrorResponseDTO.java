package com.review.shop.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
