package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminClassVO(
        Long id,
        Long departmentId,
        String departmentName,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

