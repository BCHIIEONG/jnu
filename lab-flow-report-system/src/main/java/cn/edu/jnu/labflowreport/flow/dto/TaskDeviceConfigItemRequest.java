package cn.edu.jnu.labflowreport.flow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TaskDeviceConfigItemRequest(
        @NotNull(message = "deviceId 不能为空")
        Long deviceId,
        @NotNull(message = "maxQuantity 不能为空")
        @Min(value = 0, message = "maxQuantity 不能小于 0")
        Integer maxQuantity
) {
}
