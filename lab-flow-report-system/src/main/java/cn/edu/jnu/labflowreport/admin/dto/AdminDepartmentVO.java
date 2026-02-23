package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDateTime;

public record AdminDepartmentVO(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

