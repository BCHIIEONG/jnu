package cn.edu.jnu.labflowreport.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public record AttendanceStaticCheckinRequest(
        @NotBlank(message = "code 不能为空") String code
) {
}

