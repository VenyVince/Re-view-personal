package com.review.shop.dto.user;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// id와 email
@Data
public class TemPasswordDTO {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String id;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

}
