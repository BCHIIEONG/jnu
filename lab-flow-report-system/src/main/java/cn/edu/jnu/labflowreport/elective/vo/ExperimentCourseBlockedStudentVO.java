package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDateTime;

public record ExperimentCourseBlockedStudentVO(
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        String classDisplayName,
        LocalDateTime blockedAt
) {
}
