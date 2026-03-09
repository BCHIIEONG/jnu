package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminDeviceRequest(
        @NotBlank(message = "code 不能为空")
        String code,
        @NotBlank(message = "name 不能为空")
        String name,
        @NotNull(message = "totalQuantity 不能为空")
        @Min(value = 1, message = "totalQuantity 最小为 1")
        Integer totalQuantity,
        String status,
        String location,
        String description
) {
}
