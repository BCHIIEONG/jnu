package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminSemesterVO(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

