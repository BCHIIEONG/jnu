package cn.edu.jnu.labflowreport.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SemesterManageResultVO(
        Long id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int affectedCourseCount,
        int affectedInstanceCount,
        boolean hasOutOfRangeCourses,
        String warningMessage
) {
}
