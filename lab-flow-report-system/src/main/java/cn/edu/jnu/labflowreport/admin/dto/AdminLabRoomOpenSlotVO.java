package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record AdminLabRoomOpenSlotVO(
        Long id,
        Integer weekday,
        LocalTime startTime,
        LocalTime endTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
