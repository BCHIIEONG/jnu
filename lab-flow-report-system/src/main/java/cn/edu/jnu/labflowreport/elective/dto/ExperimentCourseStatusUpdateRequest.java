package cn.edu.jnu.labflowreport.elective.dto;

import jakarta.validation.constraints.NotNull;

public record ExperimentCourseStatusUpdateRequest(
        @NotNull(message = "status 不能为空")
        String status
) {
}
