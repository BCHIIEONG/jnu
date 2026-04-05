package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;

public record TaskCompletionVO(
        Long taskId,
        Long studentId,
        String status,
        String completionSource,
        LocalDateTime requestedAt,
        LocalDateTime confirmedAt,
        Long confirmedBy,
        String confirmedByDisplayName
) {
}
