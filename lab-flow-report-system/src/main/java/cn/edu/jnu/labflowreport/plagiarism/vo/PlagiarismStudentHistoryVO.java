package cn.edu.jnu.labflowreport.plagiarism.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlagiarismStudentHistoryVO(
        Long runId,
        Long taskId,
        StudentInfo student,
        Integer currentVersionNo,
        BigDecimal maxAcrossVersions,
        BigDecimal maxEarlierVersions,
        List<VersionRisk> versions
) {

    public record StudentInfo(Long id, String username, String displayName) {
    }

    public record TopMatchStudent(Long id, String username, String displayName) {
    }

    public record VersionRisk(
            Long submissionId,
            Integer versionNo,
            LocalDateTime submittedAt,
            BigDecimal maxScore,
            TopMatchStudent topMatchStudent,
            boolean hasResult
    ) {
    }
}

