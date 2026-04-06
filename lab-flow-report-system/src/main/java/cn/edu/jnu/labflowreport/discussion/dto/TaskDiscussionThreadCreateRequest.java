package cn.edu.jnu.labflowreport.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskDiscussionThreadCreateRequest(
        @NotNull String type,
        @NotBlank String content
) {
}
