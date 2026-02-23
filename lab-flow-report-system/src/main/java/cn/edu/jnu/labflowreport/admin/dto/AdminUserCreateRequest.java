package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminUserCreateRequest(
        @NotBlank(message = "username 不能为空")
        String username,
        @NotBlank(message = "displayName 不能为空")
        String displayName,
        String password,
        Boolean enabled,
        Long departmentId,
        Long classId,
        @NotEmpty(message = "roleCodes 不能为空")
        List<String> roleCodes
) {
}

