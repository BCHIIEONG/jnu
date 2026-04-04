package cn.edu.jnu.labflowreport.elective.dto;

import jakarta.validation.constraints.NotNull;

public record ExperimentCourseEnrollRequest(
        @NotNull(message = "slotId 不能为空")
        Long slotId
) {
}
