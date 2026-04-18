package cn.edu.jnu.labflowreport.workflow.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TaskPrestudyAttachmentVO {
    private Long id;
    private Long prestudyId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
}
