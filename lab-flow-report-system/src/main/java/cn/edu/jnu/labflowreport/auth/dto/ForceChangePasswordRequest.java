package cn.edu.jnu.labflowreport.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForceChangePasswordRequest(
        @NotBlank(message = "username 不能为空")
        String username,
        @NotBlank(message = "displayName 不能为空")
        String displayName,
        @NotBlank(message = "newPassword 不能为空")
        String newPassword,
        @NotBlank(message = "confirmPassword 不能为空")
        String confirmPassword
) {
}
