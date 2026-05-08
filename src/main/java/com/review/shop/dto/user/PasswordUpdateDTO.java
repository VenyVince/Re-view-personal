package com.review.shop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PasswordUpdateDTO {
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 8, max = 100, message = "새 비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    String newPassword;
}
