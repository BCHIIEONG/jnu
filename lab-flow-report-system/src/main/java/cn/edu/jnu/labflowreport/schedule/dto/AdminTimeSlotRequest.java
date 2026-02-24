package cn.edu.jnu.labflowreport.schedule.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminTimeSlotRequest(
        @NotBlank(message = "code 不能为空") String code,
        @NotBlank(message = "name 不能为空") String name,
        @NotBlank(message = "startTime 不能为空") String startTime,
        @NotBlank(message = "endTime 不能为空") String endTime
) {
}

