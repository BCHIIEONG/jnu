package cn.edu.jnu.labflowreport.workflow.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TaskUpdateRequest(
        String title,
        String description,
        LocalDateTime deadlineAt,
        List<Long> classIds,
        Long experimentCourseId
) {
}
