package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AdminLabRoomOpenSlotRequest(
        @NotNull(message = "weekday 不能为空")
        @Min(value = 1, message = "weekday 必须在 1-7 之间")
        @Max(value = 7, message = "weekday 必须在 1-7 之间")
        Integer weekday,
        @NotNull(message = "startTime 不能为空")
        LocalTime startTime,
        @NotNull(message = "endTime 不能为空")
        LocalTime endTime
) {
}
