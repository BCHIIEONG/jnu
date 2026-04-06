package cn.edu.jnu.labflowreport.discussion.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskDiscussionMessageCreateRequest(
        @NotBlank String content
) {
}
