package cn.edu.jnu.labflowreport.flow.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TaskProgressVO(
        Long id,
        Long taskId,
        Long studentId,
        Integer stepNo,
        String content,
        LocalDateTime createdAt,
        List<ProgressAttachmentVO> attachments
) {
}
