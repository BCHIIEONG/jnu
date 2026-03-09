package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;

public record TeacherTaskProgressStudentVO(
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        String classDisplayName,
        Integer progressCount,
        String completionStatus,
        LocalDateTime latestUpdatedAt,
        LocalDateTime requestedAt,
        LocalDateTime confirmedAt
) {
}
