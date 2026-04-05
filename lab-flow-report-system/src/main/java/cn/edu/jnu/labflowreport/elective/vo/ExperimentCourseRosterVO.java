package cn.edu.jnu.labflowreport.elective.vo;

import java.util.List;

public record ExperimentCourseRosterVO(
        List<ExperimentCourseEnrollmentRowVO> enrollments,
        List<ExperimentCourseBlockedStudentVO> blockedStudents
) {
}
