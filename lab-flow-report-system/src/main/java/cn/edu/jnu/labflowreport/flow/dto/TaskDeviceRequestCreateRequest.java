package cn.edu.jnu.labflowreport.flow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TaskDeviceRequestCreateRequest(
        @NotNull(message = "deviceId 不能为空")
        Long deviceId,
        @NotNull(message = "quantity 不能为空")
        @Min(value = 1, message = "quantity 最小为 1")
        Integer quantity,
        String note
) {
}
