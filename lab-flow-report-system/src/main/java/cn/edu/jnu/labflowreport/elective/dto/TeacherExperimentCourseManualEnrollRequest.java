package cn.edu.jnu.labflowreport.elective.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherExperimentCourseManualEnrollRequest(
        @NotNull(message = "studentId 不能为空")
        Long studentId,
        @NotNull(message = "slotId 不能为空")
        Long slotId
) {
}
