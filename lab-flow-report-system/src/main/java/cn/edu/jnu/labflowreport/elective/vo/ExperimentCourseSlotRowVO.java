package cn.edu.jnu.labflowreport.elective.vo;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExperimentCourseSlotRowVO(
        Long id,
        Long courseId,
        LocalDate lessonDate,
        Long slotId,
        String slotCode,
        String slotName,
        LocalTime slotStartTime,
        LocalTime slotEndTime,
        Long labRoomId,
        String labRoomName,
        Integer capacity,
        Integer enrolledCount
) {
}
