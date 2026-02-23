package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminLabRoomVO(
        Long id,
        String name,
        String location,
        String openHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

