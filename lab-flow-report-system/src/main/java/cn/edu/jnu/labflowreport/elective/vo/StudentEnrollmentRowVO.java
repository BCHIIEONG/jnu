package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record StudentEnrollmentRowVO(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        String courseDescription,
        Long teacherId,
        String teacherDisplayName,
        Long semesterId,
        String semesterName,
        String status,
        LocalDateTime enrollDeadlineAt,
        Long slotId,
        LocalDate lessonDate,
        String slotCode,
        String slotName,
        LocalTime slotStartTime,
        LocalTime slotEndTime,
        Long labRoomId,
        String labRoomName,
        Integer capacity,
        LocalDateTime selectedAt
) {
}
