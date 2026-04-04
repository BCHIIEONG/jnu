package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExperimentCourseEnrollmentRowVO(
        Long courseId,
        String courseTitle,
        Long slotId,
        LocalDate lessonDate,
        String slotName,
        String labRoomName,
        Integer capacity,
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        String classDisplayName,
        LocalDateTime selectedAt
) {
}
