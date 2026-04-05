package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminLabRoomVO(
        Long id,
        String name,
        String location,
        String openHours,
        List<AdminLabRoomOpenSlotVO> openSlots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
