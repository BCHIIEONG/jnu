package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record TaskCreateRequest(
        @NotBlank(message = "title 不能为空")
        String title,
        String description,
        LocalDateTime deadlineAt
) {
}

