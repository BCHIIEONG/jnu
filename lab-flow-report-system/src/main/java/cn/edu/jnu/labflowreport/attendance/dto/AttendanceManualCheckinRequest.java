package cn.edu.jnu.labflowreport.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record AttendanceManualCheckinRequest(
        @NotNull(message = "studentId 不能为空") Long studentId
) {
}

