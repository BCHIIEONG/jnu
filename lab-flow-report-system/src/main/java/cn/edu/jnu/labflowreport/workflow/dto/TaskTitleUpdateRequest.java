package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskTitleUpdateRequest(
        @NotBlank(message = "title 不能为空")
        String title
) {
}
