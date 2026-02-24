package cn.edu.jnu.labflowreport.workflow.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AttachmentVO {
    private Long id;
    private Long submissionId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}

