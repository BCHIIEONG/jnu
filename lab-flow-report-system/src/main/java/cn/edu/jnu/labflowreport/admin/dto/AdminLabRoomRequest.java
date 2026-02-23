package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLabRoomRequest(
        @NotBlank(message = "name 不能为空")
        String name,
        String location,
        String openHours
) {
}

