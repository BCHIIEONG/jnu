package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;

public record ProgressAttachmentVO(
        Long id,
        Long progressLogId,
        String fileName,
        Long fileSize,
        String contentType,
        LocalDateTime uploadedAt
) {
}
