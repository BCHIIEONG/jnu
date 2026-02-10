package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmissionCreateRequest(
        @NotBlank(message = "contentMd 不能为空")
        String contentMd
) {
}

