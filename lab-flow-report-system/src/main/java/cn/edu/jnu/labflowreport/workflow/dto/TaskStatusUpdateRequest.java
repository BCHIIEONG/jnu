package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusUpdateRequest(
        @NotBlank(message = "status 不能为空")
        String status
) {
}

