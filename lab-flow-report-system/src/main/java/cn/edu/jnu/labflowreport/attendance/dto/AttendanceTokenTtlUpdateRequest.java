package cn.edu.jnu.labflowreport.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AttendanceTokenTtlUpdateRequest(
        @NotNull(message = "tokenTtlSeconds 不能为空")
        @Min(value = 3, message = "tokenTtlSeconds 最小为 3")
        @Max(value = 60, message = "tokenTtlSeconds 最大为 60")
        Integer tokenTtlSeconds
) {
}

