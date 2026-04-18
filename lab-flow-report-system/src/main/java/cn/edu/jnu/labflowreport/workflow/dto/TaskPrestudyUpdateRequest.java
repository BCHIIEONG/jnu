package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskPrestudyUpdateRequest(
        @NotBlank(message = "预习标题不能为空")
        String title,
        String description
) {
}
