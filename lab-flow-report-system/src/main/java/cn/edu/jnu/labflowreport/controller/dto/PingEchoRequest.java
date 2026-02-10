package cn.edu.jnu.labflowreport.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record PingEchoRequest(
        @NotBlank(message = "message 不能为空")
        String message
) {
}

