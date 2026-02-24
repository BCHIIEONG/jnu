package cn.edu.jnu.labflowreport.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public record AttendanceCheckinRequest(
        @NotBlank(message = "token 不能为空") String token
) {
}

