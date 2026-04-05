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
        String slotGroupName,
        String slotMode,
        LocalDate firstLessonDate,
        String repeatPattern,
        String rangeMode,
        LocalDate rangeStartDate,
        LocalDate rangeEndDate,
        LocalDateTime selectedAt
) {
    public record ScheduleRow(
            Long enrollmentId,
            Long courseId,
            String courseTitle,
            Long teacherId,
            String teacherDisplayName,
            Long semesterId,
            Long slotId,
            Long instanceId,
            String instanceDisplayName,
            Integer teachingWeek,
            LocalDate lessonDate,
            String slotCode,
            String slotName,
            LocalTime slotStartTime,
            LocalTime slotEndTime,
            Long labRoomId,
            String labRoomName
    ) {
    }
}
