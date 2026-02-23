package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminDeviceRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        @NotBlank(message = "name 不能为空")
        String name,
        String status,
        String location,
        String description
) {
}

