package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminDeviceVO(
        Long id,
        String code,
        String name,
        String status,
        String location,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

