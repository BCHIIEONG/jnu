package cn.edu.jnu.labflowreport.workflow.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TaskPrestudyNotificationVO {
    private Long taskId;
    private String taskTitle;
    private Long prestudyId;
    private String prestudyTitle;
    private String prestudyDescription;
    private Integer prestudyVersion;
    private LocalDateTime prestudyPublishedAt;
    private List<TaskPrestudyAttachmentVO> prestudyAttachments;
}
