package cn.edu.jnu.labflowreport.elective.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherExperimentCourseRemoveStudentRequest(
        @NotNull(message = "studentId 不能为空")
        Long studentId
) {
}
