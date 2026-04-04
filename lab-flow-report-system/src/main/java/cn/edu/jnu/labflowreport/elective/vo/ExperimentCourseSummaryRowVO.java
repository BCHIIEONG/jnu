package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDateTime;

public record ExperimentCourseSummaryRowVO(
        Long id,
        String title,
        String description,
        Long teacherId,
        String teacherDisplayName,
        Long semesterId,
        String semesterName,
        String status,
        LocalDateTime enrollDeadlineAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
