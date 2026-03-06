package cn.edu.jnu.labflowreport.plagiarism.vo;

import java.time.LocalDateTime;

public record PlagiarismRunVO(
        Long runId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

