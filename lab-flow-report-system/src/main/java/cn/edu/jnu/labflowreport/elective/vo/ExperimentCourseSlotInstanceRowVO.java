package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExperimentCourseSlotInstanceRowVO(
        Long id,
        Long courseId,
        Long slotGroupId,
        LocalDate lessonDate,
        Integer teachingWeek,
        String displayName,
        Long slotId,
        String slotCode,
        String slotName,
        LocalTime slotStartTime,
        LocalTime slotEndTime,
        Long labRoomId,
        String labRoomName,
        Integer capacity
) {
}
