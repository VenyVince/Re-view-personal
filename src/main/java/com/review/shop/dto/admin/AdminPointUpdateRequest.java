package com.review.shop.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(example = "{\"points\": 1500, \"expected_points\": 1000}")
public class AdminPointUpdateRequest {
    private Integer points;
    private Integer expected_points;
}
