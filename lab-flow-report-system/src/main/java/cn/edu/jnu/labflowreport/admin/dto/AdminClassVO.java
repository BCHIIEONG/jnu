package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminClassVO(
        Long id,
        Long departmentId,
        String departmentName,
        Integer grade,
        String name,
        String displayName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
