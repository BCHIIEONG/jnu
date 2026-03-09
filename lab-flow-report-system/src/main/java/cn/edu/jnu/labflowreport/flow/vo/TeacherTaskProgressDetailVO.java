package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherTaskProgressDetailVO(
        Long taskId,
        Long studentId,
        String studentUsername,
        String studentDisplayName,
        String classDisplayName,
        String completionStatus,
        LocalDateTime requestedAt,
        LocalDateTime confirmedAt,
        String confirmedByDisplayName,
        List<TaskProgressVO> logs
) {
}
