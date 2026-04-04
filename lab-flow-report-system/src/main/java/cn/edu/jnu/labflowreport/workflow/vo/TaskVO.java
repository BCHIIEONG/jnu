package cn.edu.jnu.labflowreport.workflow.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class TaskVO {
    private Long id;
    private String title;
    private String description;
    private Long publisherId;
    private String publisherName;
    private Long experimentCourseId;
    private String experimentCourseTitle;
    private LocalDateTime deadlineAt;
    private String status;
    private LocalDateTime createdAt;
    private List<TaskAttachmentVO> attachments;
}
