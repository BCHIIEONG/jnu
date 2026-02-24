package cn.edu.jnu.labflowreport.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCourseScheduleRequest(
        @NotNull(message = "semesterId 不能为空") Long semesterId,
        @NotNull(message = "classId 不能为空") Long classId,
        @NotNull(message = "teacherId 不能为空") Long teacherId,
        Long labRoomId,
        @NotBlank(message = "lessonDate 不能为空") String lessonDate,
        @NotNull(message = "slotId 不能为空") Long slotId,
        String courseName
) {
}

