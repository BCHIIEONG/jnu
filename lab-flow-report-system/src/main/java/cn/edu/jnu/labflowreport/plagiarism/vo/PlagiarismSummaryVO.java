package cn.edu.jnu.labflowreport.plagiarism.vo;

import java.math.BigDecimal;
import java.util.List;

public record PlagiarismSummaryVO(
        Long runId,
        Long taskId,
        BigDecimal maxScore,
        TopMatchStudent topMatchStudent,
        int imagesProcessed,
        int imagesSkipped,
        int textAttachmentsProcessed,
        int textAttachmentsSkipped,
        List<EvidenceItem> evidence,
        List<SkippedAttachment> skippedAttachments
) {

    public record TopMatchStudent(Long id, String username, String displayName) {
    }

    public record EvidenceItem(String type, BigDecimal score, Object detail) {
    }

    public record SkippedAttachment(String fileName, String contentType, String reason) {
    }
}
