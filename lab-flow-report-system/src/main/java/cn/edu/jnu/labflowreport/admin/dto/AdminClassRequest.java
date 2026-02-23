package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminClassRequest(
        @NotNull(message = "departmentId 不能为空")
        Long departmentId,
        @NotBlank(message = "name 不能为空")
        String name
) {
}

